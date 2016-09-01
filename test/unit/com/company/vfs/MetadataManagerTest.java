package com.company.vfs;

import com.company.vfs.exception.FileSystemEntriesLimitExceededException;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class MetadataManagerTest {

    @Test
    public void allocate() throws Exception {
        ByteStorage storage = new ByteBufferByteStorage(ByteBuffer.allocate(MetadataManager.size(8)));
        MetadataManager manager = new MetadataManager(8, storage);

        Metadata root = manager.getRoot();
        assertThat(manager.getEntriesCount(), is(1));
        assertThat(manager.getMaxEntries(), is(8));

        assertEquals(root, manager.getMetadata(0));
        assertEquals(null, manager.getMetadata(1));

        for (int i = 0; i < 7; ++i) {
            manager.allocateMetadata(Metadata.Type.Directory);
            assertThat(manager.getEntriesCount(), is(i + 2));
        }
    }

    @Test
    public void deallocate() throws Exception {
        ByteStorage storage = new ByteBufferByteStorage(ByteBuffer.allocate(MetadataManager.size(8)));
        MetadataManager manager = new MetadataManager(8, storage);

        for (int i = 0; i < 20; ++i) {
            Metadata metadata = manager.allocateMetadata(Metadata.Type.Directory);
            manager.getMetadata(1);
            assertThat(manager.getMetadata(1), is(metadata));
            manager.deallocateMetadata(metadata);
            assertThat(manager.getEntriesCount(), is(1));
        }
    }

    @Test(expected = FileSystemEntriesLimitExceededException.class)
    public void allocateShouldThrow_When_LimitExceeded() throws Exception {
        ByteStorage storage = new ByteBufferByteStorage(ByteBuffer.allocate(MetadataManager.size(8)));
        MetadataManager manager = new MetadataManager(8, storage);

        for (int i = 0; i < 8; ++i) {
            manager.allocateMetadata(Metadata.Type.Directory);
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getMetadataShouldThrow_When_IdIsGreaterThanMax() throws Exception {
        ByteStorage storage = new ByteBufferByteStorage(ByteBuffer.allocate(MetadataManager.size(8)));
        MetadataManager manager = new MetadataManager(8, storage);
        manager.getMetadata(42);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getMetadataShouldThrow_When_IdIsNegative() throws Exception {
        ByteStorage storage = new ByteBufferByteStorage(ByteBuffer.allocate(MetadataManager.size(8)));
        MetadataManager manager = new MetadataManager(8, storage);
        manager.getMetadata(-1);
    }
}