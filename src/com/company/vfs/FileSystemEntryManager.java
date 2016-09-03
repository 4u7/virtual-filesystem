package com.company.vfs;

import com.company.vfs.Metadata.Type;
import com.company.vfs.exception.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

class FileSystemEntryManager {

    private final MetadataManager metadataManager;
    private final BlockManager blockManager;
    private final ByteStorage dataBlockStorage;
    private final ConcurrentHashMap<Metadata, Integer> openedFiles;
    private final ReadWriteLock fileSystemStructureLock;

    FileSystemEntryManager(MetadataManager metadataManager, BlockManager blockManager, ByteStorage dataBlockStorage) {

        this.metadataManager = metadataManager;
        this.blockManager = blockManager;
        this.dataBlockStorage = new SynchronizedByteStorage(dataBlockStorage);
        this.openedFiles = new ConcurrentHashMap<>();
        this.fileSystemStructureLock = new ReentrantReadWriteLock();
    }

    void createDirectory(String path) throws IOException {

        if(PathUtils.isRoot(path)) {
            throw new FileAlreadyExistsException(path);
        }

        fileSystemStructureLock.writeLock().lock();
        try {
            String name = PathUtils.getName(path);
            String pathTo = PathUtils.getPathTo(path);

            Metadata metadata = getMetadata(pathTo);

            if (metadata == null) {
                throw new NoSuchFileException(pathTo);
            }

            if (metadata.getType() != Type.Directory) {
                throw new NotDirectoryException(pathTo);
            }

            if (readDirectoryContents(metadata)
                    .stream()
                    .filter(e -> name.equals(e.getName()))
                    .findFirst()
                    .isPresent()) {
                throw new FileAlreadyExistsException(path);
            }

            createFileSystemEntry(metadata, name, Type.Directory);
        }
        finally {
            fileSystemStructureLock.writeLock().unlock();
        }
    }

    OutputStream createFile(String path) throws IOException {
        if(PathUtils.isRoot(path)) {
            throw new FileAlreadyExistsException(path);
        }

        fileSystemStructureLock.writeLock().lock();
        try {
            String name = PathUtils.getName(path);
            String pathTo = PathUtils.getPathTo(path);

            Metadata metadata = getMetadata(pathTo);

            if (metadata == null) {
                throw new NoSuchFileException(pathTo);
            }

            if (metadata.getType() != Type.Directory) {
                throw new NotDirectoryException(pathTo);
            }

            if (readDirectoryContents(metadata)
                    .stream()
                    .filter(e -> name.equals(e.getName()))
                    .findFirst()
                    .isPresent()) {
                throw new FileAlreadyExistsException(path);
            }

            Metadata fileMetadata = createFileSystemEntry(metadata, name, Type.File);
            return new EntryOutputStream(fileMetadata, false);
        }
        finally {
            fileSystemStructureLock.writeLock().unlock();
        }
    }

    void delete(String path) throws IOException {
        if(PathUtils.isRoot(path)) {
            throw new AccessDeniedException("Root directory can not be deleted.");
        }

        fileSystemStructureLock.writeLock().lock();
        try {
            String name = PathUtils.getName(path);
            String pathTo = PathUtils.getPathTo(path);

            Metadata parentMetadata = getMetadata(pathTo);
            if (parentMetadata == null || parentMetadata.getType() != Type.Directory) {
                throw new NoSuchFileException(path);
            }

            List<FileSystemEntry> entries = readDirectoryContents(parentMetadata);
            Optional<FileSystemEntry> entryToDelete = entries.stream()
                    .filter(e -> name.equals(e.getName()))
                    .findFirst();

            if (!entryToDelete.isPresent()) {
                throw new NoSuchFileException(path);
            }

            Metadata metadataToDelete = getEntryMetadata(entryToDelete.get());
            if (metadataToDelete.getType() == Type.Directory && metadataToDelete.getDataLength() > 0) {
                throw new DirectoryNotEmptyException(path);
            }

            if (isOpened(metadataToDelete)) {
                throw new AccessDeniedException("Opened file can not be deleted.");
            }

            blockManager.deallocateBlocks(metadataToDelete);
            metadataManager.deallocateMetadata(metadataToDelete);

            entries.remove(entryToDelete.get());
            writeDirectoryContents(parentMetadata, entries);
        }
        finally {
            fileSystemStructureLock.writeLock().unlock();
        }
    }

    List<String> getDirectories(String path) throws IOException {
        fileSystemStructureLock.readLock().lock();
        try {
            Metadata metadata = getMetadata(path);

            if (metadata == null) {
                throw new NoSuchFileException(path);
            }

            if (metadata.getType() != Type.Directory) {
                throw new NotDirectoryException(path);
            }

            return readDirectoryContents(metadata)
                    .stream()
                    .filter(this::isDirectory)
                    .map(FileSystemEntry::getName)
                    .collect(Collectors.toList());
        }
        catch (UncheckedIOException e) {
            throw e.getCause();
        }
        finally {
            fileSystemStructureLock.readLock().unlock();
        }
    }

    List<String> getFiles(String path) throws IOException {
        fileSystemStructureLock.readLock().lock();
        try {
            Metadata metadata = getMetadata(path);

            if (metadata == null) {
                throw new NoSuchFileException(path);
            }

            if (metadata.getType() != Type.Directory) {
                throw new NotDirectoryException(path);
            }

            return readDirectoryContents(metadata)
                    .stream()
                    .filter(this::isFile)
                    .map(FileSystemEntry::getName)
                    .collect(Collectors.toList());
        }
        catch (UncheckedIOException e) {
            throw e.getCause();
        }
        finally {
            fileSystemStructureLock.readLock().unlock();
        }
    }

    boolean exists(String path) throws IOException {
        fileSystemStructureLock.readLock().lock();
        try {
            return PathUtils.isRoot(path) || getMetadata(path) != null;
        }
        finally {
            fileSystemStructureLock.readLock().unlock();
        }
    }

    boolean isDirectory(String path) throws IOException {
        if(PathUtils.isRoot(path)) {
            return true;
        }

        fileSystemStructureLock.readLock().lock();
        try {
            Metadata metadata = getMetadata(path);
            return metadata != null && metadata.getType() == Type.Directory;
        }
        finally {
            fileSystemStructureLock.readLock().unlock();
        }
    }

    boolean isFile(String path) throws IOException {
        if(PathUtils.isRoot(path)) {
            return false;
        }

        fileSystemStructureLock.readLock().lock();
        try {
            Metadata metadata = getMetadata(path);
            return metadata != null && metadata.getType() == Type.File;
        }
        finally {
            fileSystemStructureLock.readLock().unlock();
        }
    }

    OutputStream writeFile(String path, boolean append) throws IOException {
        fileSystemStructureLock.readLock().lock();
        try {
            Metadata metadata = getMetadata(path);

            if (metadata == null) {
                throw new NoSuchFileException(path);
            }

            if (metadata.getType() != Type.File) {
                throw new NotFileException(path);
            }

            return new EntryOutputStream(metadata, append);
        }
        finally {
            fileSystemStructureLock.readLock().unlock();
        }
    }

    InputStream readFile(String path) throws IOException {
        fileSystemStructureLock.readLock().lock();
        try {
            Metadata metadata = getMetadata(path);

            if (metadata == null) {
                throw new NoSuchFileException(path);
            }

            if (metadata.getType() != Type.File) {
                throw new NotFileException(path);
            }

            return new EntryInputStream(metadata);
        }
        finally {
            fileSystemStructureLock.readLock().unlock();
        }
    }

    private void openFile(Metadata metadata) {
        boolean result = false;
        while (!result) {
            Integer timesOpened = openedFiles.get(metadata);
            if(timesOpened == null) {
                result = openedFiles.putIfAbsent(metadata, 1) == null;
            }
            else {
                result = openedFiles.replace(metadata, timesOpened, timesOpened + 1);
            }
        }
    }

    private void closeFile(Metadata metadata) throws VirtualFileSystemException {

        boolean result = false;
        while (!result) {
            Integer timesOpened = openedFiles.getOrDefault(metadata, 0);

            if(timesOpened < 1) {
                throw new VirtualFileSystemException("Attempt to close file which is not opened.");
            }

            if(timesOpened == 1) {
                result = openedFiles.remove(metadata, timesOpened);
            }
            else
            {
                result = openedFiles.replace(metadata, timesOpened, timesOpened - 1);
            }
        }
    }

    private boolean isOpened(Metadata metadata) {
        return openedFiles.containsKey(metadata);
    }

    private Metadata getMetadata(String pathTo) throws IOException {
        List<String> pathComponents = PathUtils.getPathComponents(pathTo);
        Metadata current = metadataManager.getRoot();
        for (String component: pathComponents) {
            Optional<FileSystemEntry> entry = readDirectoryContents(current)
                    .stream()
                    .filter(e -> e.getName().equals(component))
                    .findFirst();

            if(!entry.isPresent()) {
                return null;
            }

            current = getEntryMetadata(entry.get());
        }

        return current;
    }

    private Metadata getEntryMetadata(FileSystemEntry fileSystemEntry) {
        return metadataManager.getMetadata(fileSystemEntry.getMetadataId());
    }

    private Metadata createFileSystemEntry(Metadata metadata, String name, Type type) throws IOException {
        Metadata entryMetadata = metadataManager.allocateMetadata(type);
        try (DataOutputStream outputStream = new DataOutputStream(new EntryOutputStream(metadata, true))) {
            FileSystemEntry entry = new FileSystemEntry(entryMetadata.getId(), name);
            entry.write(outputStream);
        }
        return entryMetadata;
    }

    private List<FileSystemEntry> readDirectoryContents(Metadata metadata) throws IOException {
        List<FileSystemEntry> contents = new ArrayList<>();
        try (DataInputStream inputStream = new DataInputStream(new EntryInputStream(metadata))) {
            while (inputStream.available() > 0) {
                contents.add(FileSystemEntry.read(inputStream));
            }
        }
        return contents;
    }

    private void writeDirectoryContents(Metadata metadata, List<FileSystemEntry> entries) throws IOException {
        metadata.setDataLength(0);
        try (DataOutputStream outputStream = new DataOutputStream(new EntryOutputStream(metadata, false))) {
            for (FileSystemEntry entry : entries) {
                entry.write(outputStream);
            }
        }
        blockManager.truncateBlocksToSize(metadata);
    }

    private boolean isDirectory(FileSystemEntry entry) {
        try {
            Metadata entryMetadata =  metadataManager.getMetadata(entry.getMetadataId());
            return entryMetadata.getType() == Type.Directory;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private boolean isFile(FileSystemEntry entry) {
        try {
            Metadata entryMetadata =  metadataManager.getMetadata(entry.getMetadataId());
            return entryMetadata.getType() == Type.File;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private class EntryOutputStream extends OutputStream {

        private final Metadata metadata;
        private int position = 0;
        private boolean closed = false;

        EntryOutputStream(Metadata metadata, boolean append)
                throws IOException {
            this.metadata = metadata;

            if(append) {
                position = metadata.getDataLength();
            }

            openFile(metadata);
        }

        @Override
        public void write(int b) throws IOException {

            if(closed) {
                throw new ClosedStreamException();
            }

            int offset = blockManager.ensureBlockOffset(metadata, position);
            dataBlockStorage.putByte(offset, (byte)b);
            ++position;

            metadata.updateDataLength(position);
        }

        @Override
        public void close() throws IOException {
            super.close();
            if(!closed) {
                closeFile(metadata);
                closed = true;
            }
        }
    }

    private class EntryInputStream extends InputStream {

        private final Metadata metadata;
        private int position = 0;
        private boolean closed = false;

        EntryInputStream(Metadata metadata) {
            this.metadata = metadata;
            openFile(metadata);
        }

        @Override
        public int read() throws IOException {

            if(closed) {
                throw new ClosedStreamException();
            }

            if(position >= metadata.getDataLength()) {
                return -1;
            }

            int offset = blockManager.getBlockOffset(metadata, position);
            int result = dataBlockStorage.getByte(offset) & 0xFF;
            ++position;

            return result;
        }

        @Override
        public int available() throws IOException {
            return metadata.getDataLength() - position;
        }

        @Override
        public void close() throws IOException {
            super.close();
            if(!closed) {
                closeFile(metadata);
                closed = true;
            }
        }
    }
}
