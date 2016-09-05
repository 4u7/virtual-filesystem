package com.company.vfs;

import com.company.vfs.exception.BlockLimitExceededException;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class BlockManagerTest {

    @Test(expected = IndexOutOfBoundsException.class)
    public void getBlockOffsetShouldThrow_When_NoBlocks() throws Exception {
        ByteStorage storage = new ByteBufferByteStorage(ByteBuffer.allocate(BlockManager.size(8)));
        BlockManager manager = new BlockManager(4096, 8, storage);

        manager.getGlobalOffset(Metadata.NO_BLOCK, 100);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getBlockOffsetShouldThrow_When_OneBlock() throws Exception {
        ByteStorage storage = new ByteBufferByteStorage(ByteBuffer.allocate(BlockManager.size(8)));
        BlockManager manager = new BlockManager(4096, 8, storage);

        int firstBlock = manager.allocateBlockChain();
        manager.ensureGlobalOffset(firstBlock, 42);
        manager.getGlobalOffset(firstBlock, 4200);
    }

    @Test(expected = BlockLimitExceededException.class)
    public void ensureBlockOffsetShould_When_LimitExceeded() throws Exception {
        ByteStorage storage = new ByteBufferByteStorage(ByteBuffer.allocate(BlockManager.size(8)));
        BlockManager manager = new BlockManager(4096, 8, storage);
        int firstBlock = manager.allocateBlockChain();
        manager.ensureGlobalOffset(firstBlock, 40960);
    }

    @Test
    public void blockOffset() throws Exception {
        ByteStorage storage = new ByteBufferByteStorage(ByteBuffer.allocate(BlockManager.size(8)));
        BlockManager manager = new BlockManager(4096, 8, storage);
        assertThat(manager.getBlockCount(), is(0));

        int firstBlock = manager.allocateBlockChain();

        int offset = manager.ensureGlobalOffset(firstBlock, 42);
        assertThat(offset, is(42));
        assertThat(manager.getBlockCount(), is(1));
        assertThat(firstBlock, is(0));

        offset = manager.getGlobalOffset(firstBlock, 100);
        assertThat(offset, is(100));

        offset = manager.ensureGlobalOffset(firstBlock, 4200);
        assertThat(offset, is(4200));
        assertThat(manager.getBlockCount(), is(2));
    }

    @Test
    public void deallocateBlocks() throws Exception {
        ByteStorage storage = new ByteBufferByteStorage(ByteBuffer.allocate(BlockManager.size(8)));
        BlockManager manager = new BlockManager(4096, 8, storage);
        assertThat(manager.getBlockCount(), is(0));

        int firstBlock = manager.allocateBlockChain();

        manager.ensureGlobalOffset(firstBlock, 10000);
        assertThat(manager.getBlockCount(), is(3));

        manager.deallocateBlockChain(firstBlock);
        assertThat(manager.getBlockCount(), is(0));
    }

    @Test
    public void truncateToSize() throws Exception {
        ByteStorage storage = new ByteBufferByteStorage(ByteBuffer.allocate(BlockManager.size(8)));
        BlockManager manager = new BlockManager(4096, 8, storage);
        assertThat(manager.getBlockCount(), is(0));

        int firstBlock = manager.allocateBlockChain();

        manager.ensureGlobalOffset(firstBlock, 10000);
        assertThat(manager.getBlockCount(), is(3));

        manager.truncateBlockChain(firstBlock, 10000);
        assertThat(manager.getBlockCount(), is(3));

        manager.truncateBlockChain(firstBlock, 8000);
        assertThat(manager.getBlockCount(), is(2));

        manager.truncateBlockChain(firstBlock, 0);
        assertThat(manager.getBlockCount(), is(0));
    }
}