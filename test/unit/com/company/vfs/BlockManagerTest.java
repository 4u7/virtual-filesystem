package com.company.vfs;

import com.company.vfs.exception.BlockLimitExceededException;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class BlockManagerTest {

    @Test(expected = IndexOutOfBoundsException.class)
    public void getBlockOffsetShouldThrow_When_NotEnoughBlocks() throws Exception {
        ByteStorage storage = new ByteBufferByteStorage(ByteBuffer.allocate(BlockManager.size(8)));
        Metadata metadata = new TestMetadata();
        BlockManager manager = new BlockManager(4096, 8, storage);

        manager.getBlockOffset(metadata, 100);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getBlockOffsetShouldThrow_When_MetadataHasOneBlock() throws Exception {
        ByteStorage storage = new ByteBufferByteStorage(ByteBuffer.allocate(BlockManager.size(8)));
        Metadata metadata = new TestMetadata();
        BlockManager manager = new BlockManager(4096, 8, storage);

        manager.ensureBlockOffset(metadata, 42);
        manager.getBlockOffset(metadata, 4200);
    }

    @Test(expected = BlockLimitExceededException.class)
    public void ensureBlockOffsetShould_When_LimitExceeded() throws Exception {
        ByteStorage storage = new ByteBufferByteStorage(ByteBuffer.allocate(BlockManager.size(8)));
        Metadata metadata = new TestMetadata();
        BlockManager manager = new BlockManager(4096, 8, storage);
        manager.ensureBlockOffset(metadata, 40960);
    }

    @Test
    public void blockOffset() throws Exception {
        ByteStorage storage = new ByteBufferByteStorage(ByteBuffer.allocate(BlockManager.size(8)));
        Metadata metadata = new TestMetadata();
        BlockManager manager = new BlockManager(4096, 8, storage);
        assertThat(manager.getBlockCount(), is(0));

        int offset = manager.ensureBlockOffset(metadata, 42);
        assertThat(offset, is(42));
        assertThat(manager.getBlockCount(), is(1));
        assertThat(metadata.getFirstBlock(), is(0));

        offset = manager.getBlockOffset(metadata, 100);
        assertThat(offset, is(100));

        offset = manager.ensureBlockOffset(metadata, 4200);
        assertThat(offset, is(4200));
        assertThat(manager.getBlockCount(), is(2));
    }

    @Test
    public void deallocateBlocks() throws Exception {
        ByteStorage storage = new ByteBufferByteStorage(ByteBuffer.allocate(BlockManager.size(8)));
        Metadata metadata = new TestMetadata();
        BlockManager manager = new BlockManager(4096, 8, storage);
        assertThat(manager.getBlockCount(), is(0));

        manager.ensureBlockOffset(metadata, 10000);
        assertThat(manager.getBlockCount(), is(3));

        manager.deallocateBlocks(metadata);
        assertThat(manager.getBlockCount(), is(0));
    }

    @Test
    public void truncateToSize() throws Exception {
        ByteStorage storage = new ByteBufferByteStorage(ByteBuffer.allocate(BlockManager.size(8)));
        Metadata metadata = new TestMetadata();
        BlockManager manager = new BlockManager(4096, 8, storage);
        assertThat(manager.getBlockCount(), is(0));

        manager.ensureBlockOffset(metadata, 10000);
        assertThat(manager.getBlockCount(), is(3));

        manager.truncateBlocksToSize(new TestMetadata());
        assertThat(manager.getBlockCount(), is(3));

        metadata.setDataLength(8000);
        manager.truncateBlocksToSize(metadata);
        assertThat(manager.getBlockCount(), is(2));

        metadata.setDataLength(0);
        manager.truncateBlocksToSize(metadata);
        assertThat(manager.getBlockCount(), is(0));
        assertThat(metadata.getFirstBlock(), is(Metadata.NO_BLOCK));
    }
}