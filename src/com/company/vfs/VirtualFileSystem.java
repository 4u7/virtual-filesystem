package com.company.vfs;

import com.company.vfs.exception.FileFormatException;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.util.List;

public class VirtualFileSystem implements FileSystem {

    private static final int MAGIC_CONSTANT = 0x70AD70E5;
    private static final int HEADER_SIZE = 16;

    private final FileSystemEntryManager fileSystemEntryManager;

    private VirtualFileSystem(ByteStorage byteStorage, int blockSize, int maxBlocks, int maxEntries) {
        int metadataSize = MetadataManager.size(maxEntries);
        ByteStorage metadataByteStorage = byteStorage.slice(HEADER_SIZE, metadataSize);
        MetadataManager metadataManager = new MetadataManager(maxEntries, metadataByteStorage);

        int blockTableOffset = HEADER_SIZE + metadataSize;
        int blockTableSize = BlockManager.size(maxBlocks);
        ByteStorage blockTableByteStorage = byteStorage.slice(blockTableOffset, blockTableSize);
        BlockManager blockManager = new BlockManager(blockSize, maxBlocks, blockTableByteStorage);

        int dataBlocksOffset = blockTableOffset + blockTableSize;
        ByteStorage dataBlocksStorage = byteStorage.slice(dataBlocksOffset, maxBlocks * blockSize);

        fileSystemEntryManager = new FileSystemEntryManager(metadataManager, blockManager, dataBlocksStorage);
    }

    public static VirtualFileSystem open(String path) throws IOException {
        File file = new File(path);
        if(!file.exists()) {
            throw new NoSuchFileException(path);
        }

        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

        if(randomAccessFile.readInt() != MAGIC_CONSTANT) {
            throw new FileFormatException("Invalid file format.");
        }

        int blockSize = randomAccessFile.readInt();
        int maxBlocks = randomAccessFile.readInt();
        int maxEntries = randomAccessFile.readInt();

        ByteStorage byteStorage = new MappedFileByteStorage(path);
        return new VirtualFileSystem(byteStorage, blockSize, maxBlocks, maxEntries);
    }

    public static Builder create(String path) {
        return new Builder(path);
    }

    @Override
    public List<String> getDirectories(String path) throws IOException {
        return null;
    }

    @Override
    public List<String> getFiles(String path) throws IOException {
        return null;
    }

    @Override
    public boolean exists(String path) throws IOException {
        return false;
    }

    @Override
    public void delete(String path) throws IOException {

    }

    @Override
    public boolean isDirectory(String path) throws IOException {
        return false;
    }

    @Override
    public void createDirectory(String path) throws IOException {
        fileSystemEntryManager.createDirectory(path);
    }

    @Override
    public OutputStream createFile(String path) throws IOException {
        return null;
    }

    @Override
    public OutputStream writeFile(String path, boolean append) throws IOException {
        return null;
    }

    @Override
    public InputStream readFile(String path) throws IOException {
        return null;
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

        public void maxEntries(int maxEntries) {
            this.maxEntries = maxEntries;
        }

        public void maxBlocks(int maxBlocks) {
            this.maxBlocks = maxBlocks;
        }
    }
}
