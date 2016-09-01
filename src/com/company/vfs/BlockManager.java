package com.company.vfs;


import com.company.vfs.exception.BlockLimitExceededException;

import java.io.IOException;
import java.util.BitSet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class BlockManager {

    private static final int NO_BLOCK = -1;

    private final int blockSize;
    private final int maxBlocks;
    private final ByteStorage byteStorage;
    private final int blockMapOffset;
    private final int blockTableOffset;
    private final BitSet blockMap;
    private final ReadWriteLock lock;

    BlockManager(int blockSize, int maxBlocks, ByteStorage byteStorage) throws IOException {
        this.blockSize = blockSize;
        this.maxBlocks = maxBlocks;
        this.byteStorage = new SynchronizedByteStorage(byteStorage);

        this.blockMapOffset = 0;
        int blockMapLength = (maxBlocks + 7) / 8;
        byte[] blockMapBytes = byteStorage.getBytes(blockMapOffset, blockMapLength);
        blockMap = BitSet.valueOf(blockMapBytes);

        this.blockTableOffset = blockMapLength;

        this.lock = new ReentrantReadWriteLock();
    }

    int getBlockOffset(Metadata metadata, int position) throws IOException {
        lock.readLock().lock();
        try {
            int blockNumber = position / blockSize;
            int offsetInBlock = position % blockSize;

            int blockIndex = getNthBlock(metadata, blockNumber);
            if (blockIndex < 0) {
                throw new IndexOutOfBoundsException("No block found for given position.");
            }

            return blockIndex * blockSize + offsetInBlock;
        }
        finally {
            lock.readLock().unlock();
        }
    }

    int ensureBlockOffset(Metadata metadata, int position) throws IOException {
        lock.writeLock().lock();
        try {
            int blockNumber = position / blockSize;
            int offsetInBlock = position % blockSize;

            int blockIndex = ensureNthBlock(metadata, blockNumber);

            return blockIndex * blockSize + offsetInBlock;
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    void truncateBlocksToSize(Metadata metadata) throws IOException {
        lock.writeLock().lock();
        try {

            int currentBlock = metadata.getFirstBlock();
            if(currentBlock < 0) {
                return;
            }

            int size = metadata.getDataLength();
            int maxBlocks = (size + blockSize - 1) / blockSize;

            if(maxBlocks == 0) {
                metadata.setFirstBlock(Metadata.NO_BLOCK);
            }

            int blockCount = 1;
            while (currentBlock >= 0) {
                int nextBlock = getNextBlock(currentBlock);

                if(blockCount > maxBlocks) {
                    setDeallocated(currentBlock);
                }

                currentBlock = nextBlock;
                ++blockCount;
            }
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    void deallocateBlocks(Metadata metadata) throws IOException {
        int currentBlock = metadata.getFirstBlock();
        lock.writeLock().lock();
        try {
            while (currentBlock >= 0) {
                int nextBlock = getNextBlock(currentBlock);
                setDeallocated(currentBlock);
                currentBlock = nextBlock;
            }
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    private int getNthBlock(Metadata metadata, int blockNumber) throws IOException {
        int currentBlock = metadata.getFirstBlock();

        int i = 1;
        while(currentBlock >= 0 && i <= blockNumber) {
            currentBlock = getNextBlock(currentBlock);
            ++i;
        }

        return currentBlock;
    }

    private int ensureNthBlock(Metadata metadata, int blockNumber) throws IOException {
        int currentBlock = metadata.getFirstBlock();
        if(currentBlock < 0) {
            currentBlock = allocateBlock();
            metadata.setFirstBlock(currentBlock);
        }

        int i = 1;
        while(i <= blockNumber) {
            int nextBlock = getNextBlock(currentBlock);
            if(nextBlock < 0) {
                nextBlock = allocateNextBlock(currentBlock);
            }
            currentBlock = nextBlock;
            ++i;
        }

        return currentBlock;
    }

    private int getNextBlock(int block) throws IOException {
        int offset = blockTableOffset + block * 4;
        return byteStorage.getInt(offset);
    }

    private int allocateBlock() throws IOException {
        int block = blockMap.nextClearBit(0);
        if(block >= maxBlocks) {
            throw new BlockLimitExceededException();
        }

        setAllocated(block);
        int offset = blockTableOffset + block * 4;
        byteStorage.putInt(offset, NO_BLOCK);

        return block;
    }

    private int allocateNextBlock(int block) throws IOException {
        int offset = blockTableOffset + block * 4;
        int allocatedBlock = allocateBlock();
        byteStorage.putInt(offset, allocatedBlock);
        return allocatedBlock;
    }

    static int size(int maxBlocks) {
        int blockMapLength = (maxBlocks + 7) / 8;
        int blockTableLength = maxBlocks * 4;
        return blockMapLength + blockTableLength;
    }

    int getMaxBlocks() {
        return maxBlocks;
    }

    int getBlockCount() {
        lock.readLock().lock();
        try {
            return blockMap.cardinality();
        }
        finally {
            lock.readLock().unlock();
        }
    }

    private void setAllocated(int block) throws IOException {
        blockMap.set(block);
        int byteOffset = blockMapOffset + block / 8;
        byte mapByte = byteStorage.getByte(byteOffset);
        mapByte |= 1 << (block % 8);
        byteStorage.putByte(byteOffset, mapByte);
    }

    private void setDeallocated(int block) throws IOException {
        blockMap.clear(block);
        int byteOffset = blockMapOffset + block / 8;
        byte mapByte = byteStorage.getByte(byteOffset);
        mapByte &= ~(1 << (block % 8));
        byteStorage.putByte(byteOffset, mapByte);
    }
}
