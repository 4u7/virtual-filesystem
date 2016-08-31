package com.company.vfs;

import com.company.vfs.Metadata.Type;
import com.company.vfs.exception.FileSystemEntriesLimitExceededException;

import java.io.IOException;
import java.util.BitSet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.company.vfs.Metadata.Type.valueOf;

public class MetadataManager {

    private final int maxEntries;
    private final ByteStorage byteStorage;
    private final MappedMetadata root;
    private final BitSet metadataMap;
    private final int metadataMapOffset;
    private final int metadataOffset;
    private final ReadWriteLock lock;

    MetadataManager(int maxEntries, ByteStorage byteStorage) throws IOException {

        this.maxEntries = maxEntries;
        this.byteStorage = new SynchronizedByteStorage(byteStorage);
        this.metadataMapOffset = 0;

        int metadataMapLength = (maxEntries + 7) / 8;
        byte[] blockMapBytes = byteStorage.getBytes(metadataMapOffset, metadataMapLength);
        this.metadataMap = BitSet.valueOf(blockMapBytes);

        this.metadataOffset = metadataMapLength;
        this.root = new MappedMetadata(0, byteStorage, metadataOffset);

        // no root allocated
        if(!metadataMap.get(0)) {
            metadataMap.set(0);
            this.root.setType(Type.Directory);
            this.root.setFirstBlock(-1);
        }

        lock = new ReentrantReadWriteLock();
    }

    public static int size(int maxEntries) {
        int metadataMapLength = (maxEntries + 7) / 8;
        return metadataMapLength + maxEntries * MappedMetadata.SIZE;
    }

    public Metadata getRoot() {
        return root;
    }

    public Metadata getMetadata(int metadataId) {
        lock.readLock().lock();
        try {
            if (metadataMap.get(metadataId)) {
                return new MappedMetadata(metadataId, byteStorage, metadataOffset);
            }
            return null;
        }
        finally {
            lock.readLock().lock();
        }
    }

    public Metadata allocateMetadata(Type type) throws IOException {
        lock.writeLock().lock();
        try {
            int index = metadataMap.nextClearBit(0);
            if (index > maxEntries) {
                throw new FileSystemEntriesLimitExceededException();
            }

            metadataMap.set(index);
            // Set bit in storage
            int byteOffset = metadataMapOffset + index / 8;
            byte mapByte = byteStorage.getByte(byteOffset);
            mapByte |= 1 << (index % 8);
            byteStorage.putByte(byteOffset, mapByte);

            MappedMetadata metadata = new MappedMetadata(index, byteStorage, metadataOffset);
            metadata.setType(type);
            metadata.setDataLength(0);
            metadata.setFirstBlock(-1);

            return metadata;
        }
        finally {
            lock.writeLock().unlock();
        }
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
        public int getDataLength() throws IOException {
            return readField(DATA_LENGTH_INDEX);
        }

        @Override
        public void updateDataLength(int length) throws IOException {
            // TODO: check thread safety
            int dataLength = getDataLength();
            if(length > dataLength) {
                writeField(DATA_LENGTH_INDEX, length);
            }
        }

        @Override
        public int getFirstBlock() throws IOException {
            return readField(FIRST_BLOCK_INDEX);
        }

        @Override
        public void setFirstBlock(int block) throws IOException {
            writeField(FIRST_BLOCK_INDEX, block);
        }

        @Override
        public Type getType() throws IOException {
            return valueOf(readField(TYPE_INDEX));
        }

        @Override
        public int getId() {
            return id;
        }

        private void setType(Type type) throws IOException {
            writeField(TYPE_INDEX, type.value);
        }

        private void setDataLength(int length) throws IOException {
            writeField(DATA_LENGTH_INDEX, length);
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
