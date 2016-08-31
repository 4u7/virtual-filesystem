package com.company.vfs;

import com.company.vfs.exception.FileFormatException;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.util.List;

public class VirtualFileSystem implements FileSystem {

    private static final int MAGIC_CONSTANT = 0x70AD70E5;
    private static final int HEADER_SIZE = 16;

    private final MetadataManager metadataManager;
    private final BlockManager blockManager;

    private final FileSystemEntryManager fileSystemEntryManager;

    private VirtualFileSystem(ByteStorage byteStorage, int blockSize, int maxBlocks, int maxEntries) throws IOException {
        int metadataSize = MetadataManager.size(maxEntries);
        ByteStorage metadataByteStorage = byteStorage.slice(HEADER_SIZE, metadataSize);
        this.metadataManager = new MetadataManager(maxEntries, metadataByteStorage);

        int blockTableOffset = HEADER_SIZE + metadataSize;
        int blockTableSize = BlockManager.size(maxBlocks);
        ByteStorage blockTableByteStorage = byteStorage.slice(blockTableOffset, blockTableSize);
        this.blockManager = new BlockManager(blockSize, maxBlocks, blockTableByteStorage);

        int dataBlocksOffset = blockTableOffset + blockTableSize;
        ByteStorage dataBlocksStorage = byteStorage.slice(dataBlocksOffset, maxBlocks * blockSize);

        fileSystemEntryManager = new FileSystemEntryManager(metadataManager, blockManager, dataBlocksStorage);
    }

    public static VirtualFileSystem open(String path) throws IOException {
        File file = new File(path);
        if(!file.exists()) {
            throw new NoSuchFileException(path);
        }

        try(RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw")) {

            if (randomAccessFile.readInt() != MAGIC_CONSTANT) {
                throw new FileFormatException("Invalid file format.");
            }

            int blockSize = randomAccessFile.readInt();
            int maxBlocks = randomAccessFile.readInt();
            int maxEntries = randomAccessFile.readInt();

            ByteStorage byteStorage = new MappedFileByteStorage(path);
            return new VirtualFileSystem(byteStorage, blockSize, maxBlocks, maxEntries);
        }
    }

    public static Builder create(String path) {
        return new Builder(path);
    }

    @Override
    public List<String> getDirectories(String path) throws IOException {
        return fileSystemEntryManager.getDirectories(path);
    }

    @Override
    public List<String> getFiles(String path) throws IOException {
        return fileSystemEntryManager.getFiles(path);
    }

    @Override
    public boolean exists(String path) throws IOException {
        return fileSystemEntryManager.exists(path);
    }

    @Override
    public void delete(String path) throws IOException {
        fileSystemEntryManager.delete(path);
    }

    @Override
    public boolean isDirectory(String path) throws IOException {
        return fileSystemEntryManager.isDirectory(path);
    }

    @Override
    public void createDirectory(String path) throws IOException {
        fileSystemEntryManager.createDirectory(path);
    }

    @Override
    public OutputStream createFile(String path) throws IOException {
        return fileSystemEntryManager.createFile(path);
    }

    @Override
    public OutputStream writeFile(String path, boolean append) throws IOException {
        return fileSystemEntryManager.writeFile(path, append);
    }

    @Override
    public InputStream readFile(String path) throws IOException {
        return fileSystemEntryManager.readFile(path);
    }

    public int getMaxBlocks() {
        return blockManager.getMaxBlocks();
    }

    public int getMaxEntries() {
        return metadataManager.getMaxEntries();
    }

    public int getBlockCount() {
        return blockManager.getBlockCount();
    }

    public int getEntriesCount() {
        return metadataManager.getEntriesCount();
    }

    public static class Builder {

        private static final int DEFAULT_BLOCK_SIZE = 4096;
        private static final int DEFAULT_MAX_BLOCKS = 32768;
        private static final int DEFAULT_MAX_ENTRIES = 32768;

        private int blockSize = DEFAULT_BLOCK_SIZE;
        private int maxEntries = DEFAULT_MAX_ENTRIES;
        private int maxBlocks = DEFAULT_MAX_BLOCKS;
        private final String path;

        public Builder(String path) {
            this.path = path;
        }

        public VirtualFileSystem build() throws IOException {

            File file = new File(path);
            if(file.exists()) {
                throw new FileAlreadyExistsException(path);
            }

            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
            randomAccessFile.writeInt(MAGIC_CONSTANT);
            randomAccessFile.writeInt(blockSize);
            randomAccessFile.writeInt(maxBlocks);
            randomAccessFile.writeInt(maxEntries);

            ByteStorage byteStorage = new MappedFileByteStorage(path);
            return new VirtualFileSystem(byteStorage, blockSize, maxBlocks, maxEntries);
        }

        public Builder maxEntries(int maxEntries) {
            this.maxEntries = maxEntries;
            return this;
        }

        public Builder maxBlocks(int maxBlocks) {
            this.maxBlocks = maxBlocks;
            return this;
        }
    }
}
