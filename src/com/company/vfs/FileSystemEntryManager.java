package com.company.vfs;

import com.company.vfs.Metadata.Type;
import com.company.vfs.exception.FileAlreadyExistsException;
import com.company.vfs.exception.NoSuchFileException;
import com.company.vfs.exception.NotDirectoryException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FileSystemEntryManager {

    private final MetadataManager metadataManager;
    private final BlockManager blockManager;
    private final ByteStorage dataBlockStorage;

    FileSystemEntryManager(MetadataManager metadataManager, BlockManager blockManager, ByteStorage dataBlockStorage) {

        this.metadataManager = metadataManager;
        this.blockManager = blockManager;
        this.dataBlockStorage = new SynchronizedByteStorage(dataBlockStorage);
    }

    public void createDirectory(String path) throws IOException {

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

        createFileSystemEntry(metadata, name, Type.Directory);
    }

    private Metadata createFileSystemEntry(Metadata metadata, String name, Type type) throws IOException {
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
            while (inputStream.available() >= 0) {
                contents.add(FileSystemEntry.read(inputStream));
            }
        }
        return contents;
    }
}
