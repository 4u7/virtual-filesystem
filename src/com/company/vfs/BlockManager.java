package com.company.vfs;


import com.company.vfs.exception.BlockLimitExceededException;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class BlockManager {

    private static final int NO_BLOCK = -1;

    private final int blockSize;
    private final int maxBlocks;
    private final ByteStorage byteStorage;
    private final ByteStorage dataBlocksStorage;
    private final int blockMapOffset;
    private final int blockTableOffset;
    private final BitSet blockMap;
    private final ReadWriteLock lock;
    private final Map<Integer, List<Integer>> blockChainCache = new ConcurrentHashMap<>();

    BlockManager(int blockSize, int maxBlocks, ByteStorage byteStorage, ByteStorage dataBlocksStorage) throws IOException {
        this.blockSize = blockSize;
        this.maxBlocks = maxBlocks;
        this.byteStorage = byteStorage;
        this.dataBlocksStorage = dataBlocksStorage;

        this.blockMapOffset = 0;
        int blockMapLength = (maxBlocks + 7) / 8;
        byte[] blockMapBytes = new byte[blockMapLength];
        byteStorage.getBytes(blockMapOffset, blockMapBytes);
        blockMap = BitSet.valueOf(blockMapBytes);

        this.blockTableOffset = blockMapLength;

        this.lock = new ReentrantReadWriteLock();
    }

    int allocateBlockChain() throws IOException {
        lock.writeLock().lock();
        try {
            return allocateBlock();
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    int getGlobalOffset(int firstBlock, int position) throws IOException {

        int blockNumber = position / blockSize;
        int offsetInBlock = position % blockSize;

        if(blockNumber == 0 && firstBlock > 0) {
            return firstBlock * blockSize + offsetInBlock;
        }

        lock.readLock().lock();
        try {
            int blockIndex = getNthBlock(firstBlock, blockNumber);
            if (blockIndex < 0) {
                throw new IndexOutOfBoundsException("No block found for given position.");
            }
            return blockIndex * blockSize + offsetInBlock;
        }
        finally {
            lock.readLock().unlock();
        }
    }

    int ensureGlobalOffset(int firstBlock, int position) throws IOException {

        int blockNumber = position / blockSize;
        int offsetInBlock = position % blockSize;

        if(blockNumber == 0 && firstBlock > 0) {
            return firstBlock * blockSize + offsetInBlock;
        }

        lock.writeLock().lock();
        try {
            int blockIndex = ensureNthBlock(firstBlock, blockNumber);
            return blockIndex * blockSize + offsetInBlock;
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    void truncateBlockChain(int firstBlock, int size) throws IOException {
        lock.writeLock().lock();
        try {

            if(size <= 0) {
                throw new IllegalArgumentException("size should be > 0");
            }

            if(firstBlock < 0) {
                throw new IllegalArgumentException("invalid firstBlock");
            }

            int currentBlock = firstBlock;
            int previousBlock = NO_BLOCK;
            int maxBlocks = (size + blockSize - 1) / blockSize;
            int blockCount = 1;

            while (currentBlock >= 0) {
                int nextBlock = getNextBlock(currentBlock);

                if(blockCount > maxBlocks) {
                    setDeallocated(currentBlock);
                    if(previousBlock >= 0) {
                        setNextBlock(previousBlock, NO_BLOCK);
                    }
                }

                previousBlock = currentBlock;
                currentBlock = nextBlock;
                ++blockCount;
            }

            List<Integer> cachedBlockChain = blockChainCache.get(firstBlock);
            if(cachedBlockChain != null) {
                cachedBlockChain = cachedBlockChain.subList(0, maxBlocks - 1);
                blockChainCache.put(firstBlock, cachedBlockChain);
            }
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    void deallocateBlockChain(int firstBlock) throws IOException {
        int currentBlock = firstBlock;
        lock.writeLock().lock();
        try {
            while (currentBlock >= 0) {
                int nextBlock = getNextBlock(currentBlock);
                setDeallocated(currentBlock);
                currentBlock = nextBlock;
            }

            blockChainCache.remove(firstBlock);
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    private int getNthBlock(int firstBlock, int blockNumber) throws IOException {

        if(blockNumber == 0) {
            return firstBlock;
        }

        List<Integer> cachedBlockChain = blockChainCache.get(firstBlock);
        if(cachedBlockChain == null) {

            int currentBlock = firstBlock;
            cachedBlockChain = new ArrayList<>();

            while(currentBlock >= 0) {
                currentBlock = getNextBlock(currentBlock);
                if(currentBlock < 0) {
                    break;
                }
                cachedBlockChain.add(currentBlock);
            }

            blockChainCache.put(firstBlock, cachedBlockChain);
        }

        --blockNumber;
        if(blockNumber > cachedBlockChain.size()) {
            return NO_BLOCK;
        }

        return cachedBlockChain.get(blockNumber);
    }

    private int ensureNthBlock(int firstBlock, int blockNumber) throws IOException {

        if(blockNumber == 0) {
            return firstBlock;
        }

        int currentBlock = firstBlock;
        --blockNumber;

        List<Integer> cachedBlockChain = blockChainCache.get(firstBlock);
        if(cachedBlockChain == null) {
            cachedBlockChain = new ArrayList<>();
        }

        if(blockNumber < cachedBlockChain.size()) {
            return cachedBlockChain.get(blockNumber);
        }
        else
        {
            int cachedChainSize = cachedBlockChain.size();
            if(cachedChainSize > 0) {
                currentBlock = cachedBlockChain.get(cachedChainSize - 1);
                blockNumber -= cachedChainSize;
                cachedBlockChain = new ArrayList<>(cachedBlockChain); // copy list for modifications
            }
        }

        while(blockNumber >= 0) {
            int nextBlock = getNextBlock(currentBlock);
            if(nextBlock < 0) {
                nextBlock = allocateNextBlock(currentBlock);
            }
            currentBlock = nextBlock;
            cachedBlockChain.add(currentBlock);
            --blockNumber;
        }

        blockChainCache.put(firstBlock, cachedBlockChain);
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
        setNextBlock(block, NO_BLOCK);

        // fill allocated block with zeros
        byte zeros[] = new byte[blockSize];
        dataBlocksStorage.putBytes(blockSize * block, zeros);

        return block;
    }

    private int allocateNextBlock(int block) throws IOException {
        int allocatedBlock = allocateBlock();
        setNextBlock(block, allocatedBlock);
        return allocatedBlock;
    }

    private void setNextBlock(int block, int nextBlock) throws IOException {
        int offset = blockTableOffset + block * Integer.BYTES;
        byteStorage.putInt(offset, nextBlock);
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

    int getBlockSize() {
        return blockSize;
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
