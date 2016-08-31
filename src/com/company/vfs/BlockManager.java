package com.company.vfs;


class BlockManager {

    private final int blockSize;
    private final int maxBlocks;
    private final ByteStorage byteStorage;
    private final int blockMapOffset;
    private final int blockTableOffset;

    public BlockManager(int blockSize, int maxBlocks, ByteStorage byteStorage) {
        this.blockSize = blockSize;
        this.maxBlocks = maxBlocks;
        this.byteStorage = byteStorage;

        this.blockMapOffset = 0;
        this.blockTableOffset = 0; // TODO: set to value
    }

    int getGlobalOffset(Metadata metadata, int position) {
        int blockNumber = position / blockSize;
        int offsetInBlock = position % blockSize;

        int blockIndex = getNthBlock(metadata, blockNumber);
        if(blockIndex < 0) {
            // TODO: throw if offset > max
        }

        return blockIndex * blockSize + offsetInBlock;
    }

    int ensureGlobalOffset(Metadata metadata, int position) {
        int blockNumber = position / blockSize;
        int offsetInBlock = position % blockSize;

        int blockIndex = ensureNthBlock(metadata, blockNumber);

        return blockIndex * blockSize + offsetInBlock;
    }

    private int getNthBlock(Metadata metadata, int blockNumber) {
        int currentBlock = metadata.getFirstBlock();
        int i = 0;

        while(currentBlock >= 0 && i <= blockNumber) {
            currentBlock = getNextBlock(currentBlock);
            ++i;
        }

        return currentBlock;
    }

    private int ensureNthBlock(Metadata metadata, int blockNumber) {
        int currentBlock = metadata.getFirstBlock();
        int i = 0;

        if(currentBlock < 0) {
            currentBlock = allocateBlock();
            metadata.setFirstBlock(currentBlock);
        }

        while(i <= blockNumber) {
            int nextBlock = getNextBlock(currentBlock);
            if(nextBlock < 0) {
                nextBlock = allocateNextBlock(currentBlock);
            }
            currentBlock = nextBlock;
            ++i;
        }

        return 0;
    }

    private int getNextBlock(int block) {
        int offset = blockTableOffset + block * 4;
        return byteStorage.getInt(offset);
    }

    private int allocateBlock() {
        return 0;
    }

    private int allocateNextBlock(int block) {
        int offset = blockTableOffset + block * 4;
        int allocatedBlock = allocateBlock();
        byteStorage.putInt(offset, allocatedBlock);
        return 0;
    }
}
