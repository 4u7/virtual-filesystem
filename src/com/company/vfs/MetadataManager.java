package com.company.vfs;

import com.company.vfs.Metadata.Type;
import com.company.vfs.exception.FileSystemEntriesLimitExceededException;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


class MetadataManager {

    private final int maxEntries;
    private final ByteStorage byteStorage;
    private final MappedMetadata root;
    private final BitSet metadataMap;
    private final int metadataMapOffset;
    private final int metadataOffset;
    private final ReadWriteLock lock;
    private final ArrayList<WeakReference<MappedMetadata>> metadataCache;

    MetadataManager(int maxEntries, ByteStorage byteStorage) throws IOException {

        this.lock = new ReentrantReadWriteLock();
        this.maxEntries = maxEntries;
        this.byteStorage = new SynchronizedByteStorage(byteStorage);
        this.metadataMapOffset = 0;

        int metadataMapLength = (maxEntries + 7) / 8;
        byte[] blockMapBytes = byteStorage.getBytes(metadataMapOffset, metadataMapLength);
        this.metadataMap = BitSet.valueOf(blockMapBytes);

        this.metadataOffset = metadataMapLength;
        this.root = new MappedMetadata(0, byteStorage, metadataOffset);

        this.metadataCache = new ArrayList<>(Collections.nCopies(maxEntries, null));

        // no root allocated
        if(!metadataMap.get(0)) {
            setAllocated(0);
            this.root.setType(Type.Directory);
            this.root.setFirstBlock(Metadata.NO_BLOCK);
        }
    }

    static int size(int maxEntries) {
        int metadataMapLength = (maxEntries + 7) / 8;
        return metadataMapLength + maxEntries * MappedMetadata.SIZE;
    }

    Metadata getRoot() {
        return root;
    }

    Metadata getMetadata(int metadataId) {

        if(metadataId < 0 || metadataId >= maxEntries) {
            throw new IndexOutOfBoundsException();
        }

        if(metadataId == 0) {
            return root;
        }

        lock.readLock().lock();
        try {
            if (metadataMap.get(metadataId)) {

                WeakReference<MappedMetadata> cachedMetadata = metadataCache.get(metadataId);
                if(cachedMetadata != null && cachedMetadata.get() != null)
                {
                    return cachedMetadata.get();
                }

                MappedMetadata metadata = new MappedMetadata(metadataId, byteStorage, metadataOffset);
                metadataCache.set(metadataId, new WeakReference<>(metadata));
                return metadata;
            }

            return null;
        }
        finally {
            lock.readLock().unlock();
        }
    }

    Metadata allocateMetadata(Type type) throws IOException {
        lock.writeLock().lock();
        try {
            int index = metadataMap.nextClearBit(0);
            if (index >= maxEntries) {
                throw new FileSystemEntriesLimitExceededException();
            }

            setAllocated(index);

            MappedMetadata metadata = new MappedMetadata(index, byteStorage, metadataOffset);
            metadata.setType(type);
            metadata.setDataLength(0);
            metadata.setFirstBlock(Metadata.NO_BLOCK);

            metadataCache.set(index, new WeakReference<>(metadata));
            return metadata;
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    void deallocateMetadata(Metadata metadata) throws IOException {
        lock.writeLock().lock();
        try {
            int index = metadata.getId();
            setDeallocated(index);
            metadataCache.set(index, null);
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    int getEntriesCount() {
        lock.readLock().lock();
        try {
            return metadataMap.cardinality();
        }
        finally {
            lock.readLock().unlock();
        }
    }

    int getMaxEntries() {
        return maxEntries;
    }

    private void setAllocated(int index) throws IOException {
        metadataMap.set(index);
        // Set bit in storage
        int byteOffset = metadataMapOffset + index / 8;
        byte mapByte = byteStorage.getByte(byteOffset);
        mapByte |= 1 << (index % 8);
        byteStorage.putByte(byteOffset, mapByte);
    }

    private void setDeallocated(int index) throws IOException {
        metadataMap.clear(index);
        // Set bit in storage
        int byteOffset = metadataMapOffset + index / 8;
        byte mapByte = byteStorage.getByte(byteOffset);
        mapByte &= ~(1 << (index % 8));
        byteStorage.putByte(byteOffset, mapByte);
    }

    private static class MappedMetadata implements Metadata {

        private static final int SIZE = 12;
        private static final int FIELD_SIZE = 4;

        private static final int TYPE_INDEX = 0;
        private static final int DATA_LENGTH_INDEX = 1;
        private static final int FIRST_BLOCK_INDEX = 2;

        private final int id;
        private final ByteStorage byteStorage;
        private final int metadataOffset;

        MappedMetadata(int id, ByteStorage byteStorage, int metadataOffset) {

            this.id = id;
            this.byteStorage = byteStorage;
            this.metadataOffset = metadataOffset;
        }

        @Override
        synchronized public int getDataLength() throws IOException {
            return readField(DATA_LENGTH_INDEX);
        }

        @Override
        synchronized public void setDataLength(int length) throws IOException {
            writeField(DATA_LENGTH_INDEX, length);
        }

        @Override
        synchronized public void updateDataLength(int length) throws IOException {
            int dataLength = getDataLength();
            if(length > dataLength) {
                writeField(DATA_LENGTH_INDEX, length);
            }
        }

        @Override
        synchronized public int getFirstBlock() throws IOException {
            return readField(FIRST_BLOCK_INDEX);
        }

        @Override
        synchronized public void setFirstBlock(int block) throws IOException {
            writeField(FIRST_BLOCK_INDEX, block);
        }

        @Override
        synchronized public Type getType() throws IOException {
            return Type.valueOf(readField(TYPE_INDEX));
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public boolean equals(Object obj) {
            return  obj != null &&
                    obj instanceof Metadata &&
                    id == ((Metadata) obj).getId();

        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }

        synchronized private void setType(Type type) throws IOException {
            writeField(TYPE_INDEX, type.value);
        }

        private int offset() {
            return metadataOffset + id * SIZE;
        }

        private int readField(int index) throws IOException {
            return byteStorage.getInt(offset() + index * FIELD_SIZE);
        }

        private void writeField(int index, int value) throws IOException {
            byteStorage.putInt(offset() + index * FIELD_SIZE, value);
        }

    }
}
