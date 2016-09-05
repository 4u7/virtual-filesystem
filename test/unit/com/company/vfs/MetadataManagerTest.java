package com.company.vfs;

import com.company.vfs.Metadata.Type;
import com.company.vfs.exception.BlockLimitExceededException;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class MetadataManagerTest {

    @Test
    public void allocate() throws Exception {
        ByteStorage storage = new ByteBufferByteStorage(ByteBuffer.allocate(BlockManager.size(8)));
        ByteStorage dataBlocksStorage = new ByteBufferByteStorage(ByteBuffer.allocate(4096 * 8));
        BlockManager blockManager = new BlockManager(4096, 8, storage, dataBlocksStorage);
        blockManager.allocateBlockChain();
        blockManager.allocateBlockChain();

        MetadataManager manager = new MetadataManager(blockManager, dataBlocksStorage);

        Metadata root = manager.getRoot();
        assertThat(manager.getMetadataCount(), is(1));

        assertEquals(root, manager.getMetadata(0));
        assertEquals(null, manager.getMetadata(1));

        for (int i = 0; i < 7; ++i) {
            manager.allocateMetadata(Type.Directory);
            assertThat(manager.getMetadataCount(), is(i + 2));
        }
    }

    @Test
    public void deallocate() throws Exception {
        ByteStorage storage = new ByteBufferByteStorage(ByteBuffer.allocate(BlockManager.size(8)));
        ByteStorage dataBlocksStorage = new ByteBufferByteStorage(ByteBuffer.allocate(4096 * 8));
        BlockManager blockManager = new BlockManager(4096, 8, storage, dataBlocksStorage);
        blockManager.allocateBlockChain();
        blockManager.allocateBlockChain();

        MetadataManager manager = new MetadataManager(blockManager, dataBlocksStorage);

        for (int i = 0; i < 20; ++i) {
            Metadata metadata = manager.allocateMetadata(Type.Directory);
            manager.getMetadata(1);
            assertThat(manager.getMetadata(1), is(metadata));
            manager.deallocateMetadata(metadata);
            assertThat(manager.getMetadataCount(), is(1));
        }
    }

    @Test(expected = BlockLimitExceededException.class)
    public void allocateShouldThrow_When_LimitExceeded() throws Exception {
        ByteStorage storage = new ByteBufferByteStorage(ByteBuffer.allocate(BlockManager.size(8)));
        ByteStorage dataBlocksStorage = new ByteBufferByteStorage(ByteBuffer.allocate(4096 * 8));
        BlockManager blockManager = new BlockManager(4096, 8, storage, dataBlocksStorage);
        blockManager.allocateBlockChain();
        blockManager.allocateBlockChain();

        MetadataManager manager = new MetadataManager(blockManager, dataBlocksStorage);

        for (int i = 0; i < 10000; ++i) {
            manager.allocateMetadata(Type.Directory);
        }
    }


    @Test(expected = IndexOutOfBoundsException.class)
    public void getMetadataShouldThrow_When_IdIsNegative() throws Exception {
        ByteStorage storage = new ByteBufferByteStorage(ByteBuffer.allocate(BlockManager.size(8)));
        ByteStorage dataBlocksStorage = new ByteBufferByteStorage(ByteBuffer.allocate(4096 * 8));
        BlockManager blockManager = new BlockManager(4096, 8, storage, dataBlocksStorage);
        blockManager.allocateBlockChain();
        blockManager.allocateBlockChain();

        MetadataManager manager = new MetadataManager(blockManager, dataBlocksStorage);
        manager.getMetadata(-1);
    }
}