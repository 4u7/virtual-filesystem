package com.company.vfs;

import com.company.vfs.exception.*;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.util.List;

/**
 * Emulates filesystem-like structure inside a single file.
 * Supports directory structure, creating, deleting, reading, and writing files.
 * When created allocates file on disk with maximum size for provided filesystem parameters.
 * VirtualFileSystem object is thread-safe. You may perform any operations from any threads.
 * Multiple VirtualFileSystem objects associated with single file are not supported and will
 * result in undefined behavior.
 */
public class VirtualFileSystem implements FileSystem {

    private static final int MAGIC_CONSTANT = 0x70AD70E5;
    private static final int HEADER_SIZE = 12;

    private static final int DEFAULT_BLOCK_SIZE = 4096;

    private final BlockManager blockManager;
    private final MetadataManager metadataManager;
    private final FileSystemEntryManager fileSystemEntryManager;

    private VirtualFileSystem(BlockManager blockManager, MetadataManager metadataManager,
                              FileSystemEntryManager fileSystemEntryManager) throws IOException {

        this.blockManager = blockManager;
        this.metadataManager = metadataManager;
        this.fileSystemEntryManager = fileSystemEntryManager;
    }

    /**
     * Opens existing vfs file and returns VirtualFileSystem object associated with it.
     * @param path path to file which contains filesystem
     * @return VirtualFileSystem object associated with file
     * @throws IllegalArgumentException if path is null
     * @throws NoSuchFileException if file doesn't exist
     * @throws FileFormatException if file format is invalid
     * @throws IOException if some error occurs while reading or writing underlying file
     */
    public static VirtualFileSystem open(String path) throws IOException {
        if(path == null) {
            throw new IllegalArgumentException("path parameter can't be null.");
        }

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

            ByteStorage dataBlocksStorage = new SynchronizedByteStorage(
                    new MappedFileByteStorage(path, HEADER_SIZE + BlockManager.size(maxBlocks), maxBlocks * blockSize));

            ByteStorage blockManagerStorage = new SynchronizedByteStorage(
                    new MappedFileByteStorage(path, HEADER_SIZE, BlockManager.size(maxBlocks)));
            BlockManager blockManager = new BlockManager(blockSize, maxBlocks, blockManagerStorage, dataBlocksStorage);

            MetadataManager metadataManager = new MetadataManager(blockManager, dataBlocksStorage);

            FileSystemEntryManager fileSystemEntryManager = new FileSystemEntryManager(metadataManager,
                    blockManager, dataBlocksStorage);

            return new VirtualFileSystem(blockManager, metadataManager, fileSystemEntryManager);
        }
    }

    /**
     * Creates new virtual file system in provided path
     * @param path path to file to be created
     * @return Builder object which can be used to configure vfs
     */
    public static VirtualFileSystem create(String path, int maxBlocks) throws IOException {
        if(path == null) {
            throw new IllegalArgumentException("path parameter can't be null.");
        }

        File file = new File(path);
        if(file.exists()) {
            throw new FileAlreadyExistsException(path);
        }

        int blockSize = DEFAULT_BLOCK_SIZE;

        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
        randomAccessFile.writeInt(MAGIC_CONSTANT);
        randomAccessFile.writeInt(blockSize);
        randomAccessFile.writeInt(maxBlocks);

        ByteStorage dataBlocksStorage = new SynchronizedByteStorage(
                new MappedFileByteStorage(path, HEADER_SIZE + BlockManager.size(maxBlocks), maxBlocks * blockSize));

        ByteStorage blockManagerStorage = new SynchronizedByteStorage(
                new MappedFileByteStorage(path, HEADER_SIZE, BlockManager.size(maxBlocks)));
        BlockManager blockManager = new BlockManager(blockSize, maxBlocks, blockManagerStorage, dataBlocksStorage);

        // 0 and 1 block chains reserved for metadata
        blockManager.allocateBlockChain();
        blockManager.allocateBlockChain();

        MetadataManager metadataManager = new MetadataManager(blockManager, dataBlocksStorage);

        FileSystemEntryManager fileSystemEntryManager = new FileSystemEntryManager(metadataManager,
                blockManager, dataBlocksStorage);

        return new VirtualFileSystem(blockManager, metadataManager, fileSystemEntryManager);
    }

    /**
     * Returns list of directories in target directory.
     * @param path path to directory
     * @return list of directories in target directory.
     * @throws IllegalArgumentException if path is null
     * @throws NoSuchFileException if target directory doesn't exist
     * @throws NotDirectoryException if target is not directory
     * @throws IOException if some error occurs while reading or writing underlying file
     */
    @Override
    public List<String> getDirectories(String path) throws IOException {
        if(path == null) {
            throw new IllegalArgumentException("path parameter can't be null.");
        }
        return fileSystemEntryManager.getDirectories(path);
    }

    /**
     * Returns list of files in target directory.
     * @param path path to directory
     * @return list of files in target directory.
     * @throws IllegalArgumentException if path is null
     * @throws NoSuchFileException if target directory doesn't exist
     * @throws NotDirectoryException if target is not directory
     * @throws IOException if some error occurs while reading or writing underlying file
     */
    @Override
    public List<String> getFiles(String path) throws IOException {
        if(path == null) {
            throw new IllegalArgumentException("path parameter can't be null.");
        }
        return fileSystemEntryManager.getFiles(path);
    }

    /**
     * Checks if entry exists in filesystem
     * @param path path to entry
     * @return true if entry exits, false otherwise
     * @throws IllegalArgumentException if path is null
     * @throws IOException if some error occurs while reading or writing underlying file
     */
    @Override
    public boolean exists(String path) throws IOException {
        if(path == null) {
            throw new IllegalArgumentException("path parameter can't be null.");
        }
        return fileSystemEntryManager.exists(path);
    }

    /**
     * Deletes filesystem entry in given path
     * @param path path to entry
     * @throws IllegalArgumentException if path is null
     * @throws AccessDeniedException if target is root directory or opened for reading or writing
     * @throws NoSuchFileException if target directory doesn't exist
     * @throws DirectoryNotEmptyException if target is non empty directory
     * @throws IOException if some error occurs while reading or writing underlying file
     */
    @Override
    public void delete(String path) throws IOException {
        if(path == null) {
            throw new IllegalArgumentException("path parameter can't be null.");
        }
        fileSystemEntryManager.delete(path);
    }

    /**
     * Checks if filesystem entry is directory
     * @param path path to entry
     * @return true if entry is directory, otherwise false
     * @throws IllegalArgumentException if path is null
     * @throws IOException if some error occurs while reading or writing underlying file
     */
    @Override
    public boolean isDirectory(String path) throws IOException {
        if(path == null) {
            throw new IllegalArgumentException("path parameter can't be null.");
        }
        return fileSystemEntryManager.isDirectory(path);
    }

    /**
     * Checks if filesystem entry is file
     * @param path path to entry
     * @return true if entry is file, otherwise false
     * @throws IllegalArgumentException if path is null
     * @throws IOException if some error occurs while reading or writing underlying file
     */
    @Override
    public boolean isFile(String path) throws IOException {
        if(path == null) {
            throw new IllegalArgumentException("path parameter can't be null.");
        }
        return fileSystemEntryManager.isFile(path);
    }

    /**
     * Creates directory with given path, parent directory must exists or exception will be thrown
     * @param path of directory to create
     * @throws IllegalArgumentException if path is null
     * @throws FileAlreadyExistsException if entry in target path already exists
     * @throws NoSuchFileException if parent directory doesn't exist
     * @throws NotDirectoryException if parent is not a directory
     * @throws BlockLimitExceededException if no more free blocks available.
     * @throws FileSystemEntriesLimitExceededException if no more entries available.
     * @throws IOException if some error occurs while reading or writing underlying file
     */
    @Override
    public void createDirectory(String path) throws IOException {
        if(path == null) {
            throw new IllegalArgumentException("path parameter can't be null.");
        }
        fileSystemEntryManager.createDirectory(path);
    }

    /**
     * Creates new file with given path and returns OutputStream
     * which can be used to write to this file. OutputStream may throw BlockLimitExceededException
     * while writing if no more free blocks available.
     * @param path of file to create
     * @return OutputStream which can be used to write to this file
     * @throws IllegalArgumentException if path is null
     * @throws FileAlreadyExistsException if entry in target path already exists
     * @throws NoSuchFileException if parent directory doesn't exist
     * @throws NotDirectoryException if parent is not a directory
     * @throws BlockLimitExceededException if no more free blocks available.
     * @throws FileSystemEntriesLimitExceededException if no more entries available.
     * @throws IOException if some error occurs while reading or writing underlying file
     */
    @Override
    public OutputStream createFile(String path) throws IOException {
        if(path == null) {
            throw new IllegalArgumentException("path parameter can't be null.");
        }
        return fileSystemEntryManager.createFile(path);
    }

    /**
     * Opens existing file with given path for writing and returns OutputStream
     * which can be used to write to this file. OutputStream may throw BlockLimitExceededException
     * while writing if no more free blocks available.
     * @param path of file to open
     * @return OutputStream which can be used to write to this file
     * @throws IllegalArgumentException if path is null
     * @throws NoSuchFileException if file doesn't exist
     * @throws NotFileException if target is not file
     * @throws IOException if some error occurs while reading or writing underlying file
     */
    @Override
    public OutputStream writeFile(String path, boolean append) throws IOException {
        if(path == null) {
            throw new IllegalArgumentException("path parameter can't be null.");
        }
        return fileSystemEntryManager.writeFile(path, append);
    }

    /**
     * Opens existing file with given path for reading and returns InputStream
     * which can be used to read this file.
     * @param path of file to open
     * @return InputStream which can be used to read file
     * @throws IllegalArgumentException if path is null
     * @throws NoSuchFileException if file doesn't exist
     * @throws NotFileException if target is not file
     * @throws IOException if some error occurs while reading or writing underlying file
     */
    @Override
    public InputStream readFile(String path) throws IOException {
        if(path == null) {
            throw new IllegalArgumentException("path parameter can't be null.");
        }
        return fileSystemEntryManager.readFile(path);
    }

    /**
     * @return maximum number of blocks
     */
    public int getMaxBlocks() {
        return blockManager.getMaxBlocks();
    }

    /**
     * @return number of allocated blocks
     */
    public int getBlocksCount() {
        return blockManager.getBlockCount();
    }

    /**
     * @return number of allocated entries
     */
    public int getEntriesCount() {
        return metadataManager.getMetadataCount();
    }
}
