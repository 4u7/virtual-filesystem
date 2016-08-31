package com.company.vfs;

import com.company.vfs.Metadata.Type;
import com.company.vfs.exception.FileAlreadyExistsException;
import com.company.vfs.exception.NoSuchFileException;
import com.company.vfs.exception.NotDirectoryException;
import com.company.vfs.exception.NotFileException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

class FileSystemEntryManager {

    private final MetadataManager metadataManager;
    private final BlockManager blockManager;
    private final ByteStorage dataBlockStorage;

    FileSystemEntryManager(MetadataManager metadataManager, BlockManager blockManager, ByteStorage dataBlockStorage) {

        this.metadataManager = metadataManager;
        this.blockManager = blockManager;
        this.dataBlockStorage = new SynchronizedByteStorage(dataBlockStorage);
    }

    void createDirectory(String path) throws IOException {

        if(PathUtils.isRoot(path)) {
            throw new FileAlreadyExistsException(path);
        }

        String name = PathUtils.getName(path);
        String pathTo = PathUtils.getPathTo(path);

        Metadata metadata = getMetadata(pathTo);

        if(metadata == null) {
            throw new NoSuchFileException(pathTo);
        }

        if(metadata.getType() != Type.Directory) {
            throw new NotDirectoryException(pathTo);
        }

        if(readDirectoryContents(metadata)
                .stream()
                .filter(e -> name.equals(e.getName()))
                .findFirst()
                .isPresent()) {
            throw new FileAlreadyExistsException(path);
        }

        createFileSystemEntry(metadata, name, Type.Directory);
    }

    List<String> getDirectories(String path) throws IOException {
        Metadata metadata = getMetadata(path);

        if(metadata == null) {
            throw new NoSuchFileException(path);
        }

        if(metadata.getType() != Type.Directory) {
            throw new NotDirectoryException(path);
        }

        return readDirectoryContents(metadata)
                .stream()
                .filter(this::isDirectory)
                .map(FileSystemEntry::getName)
                .collect(Collectors.toList());
    }

    List<String> getFiles(String path) throws IOException {
        Metadata metadata = getMetadata(path);

        if(metadata == null) {
            throw new NoSuchFileException(path);
        }

        if(metadata.getType() != Type.Directory) {
            throw new NotDirectoryException(path);
        }

        return readDirectoryContents(metadata)
                .stream()
                .filter(this::isFile)
                .map(FileSystemEntry::getName)
                .collect(Collectors.toList());
    }

    boolean exists(String path) throws IOException {
        return PathUtils.isRoot(path) || getMetadata(path) != null;
    }

    boolean isDirectory(String path) throws IOException {
        if(PathUtils.isRoot(path)) {
            return true;
        }

        Metadata metadata = getMetadata(path);
        return metadata != null && metadata.getType() == Type.Directory;
    }

    OutputStream createFile(String path) throws IOException {
        if(PathUtils.isRoot(path)) {
            throw new FileAlreadyExistsException(path);
        }

        String name = PathUtils.getName(path);
        String pathTo = PathUtils.getPathTo(path);

        Metadata metadata = getMetadata(pathTo);

        if(metadata == null) {
            throw new NoSuchFileException(pathTo);
        }

        if(metadata.getType() != Type.Directory) {
            throw new NotDirectoryException(pathTo);
        }

        if(readDirectoryContents(metadata)
                .stream()
                .filter(e -> name.equals(e.getName()))
                .findFirst()
                .isPresent()) {
            throw new FileAlreadyExistsException(path);
        }

        Metadata fileMetadata = createFileSystemEntry(metadata, name, Type.Directory);
        return new EntryOutputStream(fileMetadata, dataBlockStorage, blockManager, false);
    }

    OutputStream writeFile(String path, boolean append) throws IOException {
        Metadata metadata = getMetadata(path);

        if(metadata == null) {
            throw new NoSuchFileException(path);
        }

        if(metadata.getType() != Type.File) {
            throw new NotFileException(path);
        }

        return new EntryOutputStream(metadata, dataBlockStorage, blockManager, append);
    }

    InputStream readFile(String path) throws IOException {
        Metadata metadata = getMetadata(path);

        if(metadata == null) {
            throw new NoSuchFileException(path);
        }

        if(metadata.getType() != Type.File) {
            throw new NotFileException(path);
        }

        return new EntryInputStream(metadata, dataBlockStorage, blockManager);
    }

    void delete(String path) {
        // TODO: implemetation
    }

    private Metadata createFileSystemEntry(Metadata metadata, String name, Type type) throws IOException {

        // TODO: ensure thread safety

        Metadata entryMetadata = metadataManager.allocateMetadata(type);

        try(OutputStream outputStream = new EntryOutputStream(metadata, dataBlockStorage, blockManager, true)) {
            FileSystemEntry entry = new FileSystemEntry(entryMetadata.getId(), name);
            entry.write(outputStream);
        }

        return entryMetadata;
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

    private List<FileSystemEntry> readDirectoryContents(Metadata metadata) throws IOException {
        List<FileSystemEntry> contents = new ArrayList<>();

        try(InputStream inputStream = new EntryInputStream(metadata, dataBlockStorage, blockManager)) {
            while (inputStream.available() > 0) {
                contents.add(FileSystemEntry.read(inputStream));
            }
        }
        return contents;
    }

    private boolean isDirectory(FileSystemEntry entry) {
        try {
            Metadata entryMetadata =  metadataManager.getMetadata(entry.getMetadataId());
            return entryMetadata.getType() == Type.Directory;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isFile(FileSystemEntry entry) {
        try {
            Metadata entryMetadata =  metadataManager.getMetadata(entry.getMetadataId());
            return entryMetadata.getType() == Type.File;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
