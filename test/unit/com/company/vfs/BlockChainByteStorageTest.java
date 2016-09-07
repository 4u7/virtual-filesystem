package com.company.vfs;

import org.junit.Test;

import java.nio.ByteBuffer;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class BlockChainByteStorageTest {
    private static final int BLOCK_SIZE = 8;
    private static final int BLOCK_COUNT = 16;

    @Test
    public void Byte() throws Exception {
        byte[] data = "lorem ipsum dolor sit amet some random data 42".getBytes();
        ByteStorage storage = new ByteBufferByteStorage(ByteBuffer.allocate(BlockManager.size(BLOCK_COUNT)));

        ByteBuffer testBuffer = ByteBuffer.allocate(BLOCK_SIZE * BLOCK_COUNT);
        ByteStorage dataBlocksStorage = new ByteBufferByteStorage(testBuffer);
        BlockManager blockManager = new BlockManager(BLOCK_SIZE, BLOCK_COUNT, storage, dataBlocksStorage);
        int startBlock = blockManager.allocateBlockChain();

        BlockChainByteStorage blockChainByteStorage = new BlockChainByteStorage(dataBlocksStorage,
                blockManager, startBlock);

        for (int i = 0; i < data.length; ++i) {
            blockChainByteStorage.putByte(i, data[i]);
        }

        byte[] testData = new byte[data.length];
        testBuffer.position(0);
        testBuffer.get(testData);
        assertThat(testData, is(data));
        assertThat(testData, is(blockChainByteStorage.getBytes(0, data.length)));
    }

    @Test
    public void Int() throws Exception {
        int[] data = new int[] { 0, 42, 608923, 1314231, -277264, -505688, 1960213, -671149, -533795, 616252,
                -1837036, 1063578, 1355722, 1688571, -854407, -384451, -1202413, -900648 };
        ByteStorage storage = new ByteBufferByteStorage(ByteBuffer.allocate(BlockManager.size(BLOCK_COUNT)));

        ByteBuffer testBuffer = ByteBuffer.allocate(BLOCK_SIZE * BLOCK_COUNT);
        ByteStorage dataBlocksStorage = new ByteBufferByteStorage(testBuffer);
        BlockManager blockManager = new BlockManager(BLOCK_SIZE, BLOCK_COUNT, storage, dataBlocksStorage);
        int startBlock = blockManager.allocateBlockChain();

        BlockChainByteStorage blockChainByteStorage = new BlockChainByteStorage(dataBlocksStorage,
                blockManager, startBlock);

        for (int i = 0; i < data.length; ++i) {
            blockChainByteStorage.putInt(i * Integer.BYTES, data[i]);
        }

        testBuffer.position(0);
        for(int i = 0; i < data.length; ++i) {
            assertThat(testBuffer.getInt(i * Integer.BYTES), is(data[i]));
            assertThat(blockChainByteStorage.getInt(i * Integer.BYTES), is(data[i]));
        }
    }

    @Test
    public void Bytes() throws Exception {
        byte[] data = "lorem ipsum dolor sit amet some random data 42".getBytes();
        ByteStorage storage = new ByteBufferByteStorage(ByteBuffer.allocate(BlockManager.size(BLOCK_COUNT)));

        ByteBuffer testBuffer = ByteBuffer.allocate(BLOCK_SIZE * BLOCK_COUNT);
        ByteStorage dataBlocksStorage = new ByteBufferByteStorage(testBuffer);
        BlockManager blockManager = new BlockManager(BLOCK_SIZE, BLOCK_COUNT, storage, dataBlocksStorage);
        int startBlock = blockManager.allocateBlockChain();

        BlockChainByteStorage blockChainByteStorage = new BlockChainByteStorage(dataBlocksStorage,
                blockManager, startBlock);

        blockChainByteStorage.putBytes(0, data);

        testBuffer.position(0);
        byte[] testData = new byte[data.length];
        testBuffer.get(testData);
        assertThat(testData, is(data));
        assertThat(testData, is(blockChainByteStorage.getBytes(0, data.length)));
    }

}