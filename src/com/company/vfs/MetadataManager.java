package com.company.vfs;

import com.company.vfs.Metadata.Type;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


class MetadataManager {

    private final static int MAP_BLOCK_CHAIN = 0;
    private final static int METADATA_BLOCK_CHAIN = 1;
    private final static int MAX_METADATA_OFFSET = 0;
    private final static int MAP_OFFSET = 4;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<Integer, WeakReference<MappedMetadata>> metadataCache = new HashMap<>();
    private final BitSet metadataMap;

    private final BlockManager blockManager;
    private final ByteStorage dataBlocksStorage;
    private final Metadata root;

    private int maxMetadata;

    private final int metadataPerBlock;

    MetadataManager(BlockManager blockManager, ByteStorage dataBlocksStorage) throws IOException {

        this.blockManager = blockManager;
        this.dataBlocksStorage = dataBlocksStorage;
        this.metadataPerBlock = blockManager.getBlockSize() / MappedMetadata.BYTES;

        maxMetadata = readMaxMetadata();
        int byteLength = (maxMetadata + 7) / 8;

        if(maxMetadata > 0) {
            byte[] mapBytes = new byte[byteLength];
            int mapOffset = blockManager.getGlobalOffset(MAP_BLOCK_CHAIN, MAP_OFFSET);
            dataBlocksStorage.getBytes(mapOffset, mapBytes);
            metadataMap = BitSet.valueOf(mapBytes);
        }
        else {
            metadataMap = new BitSet();
        }

        this.root = new MappedMetadata(0);
        if(!isAllocated(0)) {
            setAllocated(0);
            this.root.setType(Type.Directory);
            this.root.setDataLength(0);
            this.root.setFirstBlock(Metadata.NO_BLOCK);
        }
    }

    Metadata getRoot() {
        return root;
    }

    Metadata getMetadata(int metadataId) throws IOException {

        if(metadataId < 0) {
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

                MappedMetadata metadata = new MappedMetadata(metadataId);
                metadataCache.put(metadataId, new WeakReference<>(metadata));
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

            if(index >= maxMetadata) {
                maxMetadata = index + 1;
                writeMaxMetadata(maxMetadata);
            }

            setAllocated(index);

            MappedMetadata metadata = new MappedMetadata(index);
            metadata.setType(type);
            metadata.setDataLength(0);
            metadata.setFirstBlock(Metadata.NO_BLOCK);

            metadataCache.put(index, new WeakReference<>(metadata));
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
            metadataCache.remove(index);

            int max = metadataMap.previousSetBit(maxMetadata - 1) + 1;
            if(max < maxMetadata) {
                maxMetadata = max;
                writeMaxMetadata(maxMetadata);

                int mapByteLength = (maxMetadata + 7) / 8;
                blockManager.truncateBlockChain(MAP_BLOCK_CHAIN, MAP_OFFSET + mapByteLength);
                blockManager.truncateBlockChain(METADATA_BLOCK_CHAIN,
                        metadataOffset(maxMetadata) + MappedMetadata.BYTES);
            }

        }
        finally {
            lock.writeLock().unlock();
        }
    }

    int getMetadataCount() {
        lock.readLock().lock();
        try {
            return metadataMap.cardinality();
        }
        finally {
            lock.readLock().unlock();
        }
    }

    private boolean isAllocated(int index) {
        return metadataMap.get(index);
    }

    private void setAllocated(int index) throws IOException {
        metadataMap.set(index);
        // Set bit in storage
        int byteOffset = MAP_OFFSET + index / 8;
        int offset = blockManager.ensureGlobalOffset(MAP_BLOCK_CHAIN, byteOffset);

        byte mapByte = dataBlocksStorage.getByte(offset);
        mapByte |= 1 << (index % 8);
        dataBlocksStorage.putByte(offset, mapByte);
    }

    private void setDeallocated(int index) throws IOException {
        metadataMap.clear(index);
        // Set bit in storage
        int byteOffset = MAP_OFFSET + index / 8;
        int offset = blockManager.ensureGlobalOffset(MAP_BLOCK_CHAIN, byteOffset);

        byte mapByte = dataBlocksStorage.getByte(offset);
        mapByte &= ~(1 << (index % 8));
        dataBlocksStorage.putByte(offset, mapByte);
    }

    private int readMaxMetadata() throws IOException {
        int maxOffset = blockManager.getGlobalOffset(MAP_BLOCK_CHAIN, MAX_METADATA_OFFSET);
        return dataBlocksStorage.getInt(maxOffset);
    }

    private void writeMaxMetadata(int maxMetadata) throws IOException {
        int maxOffset = blockManager.getGlobalOffset(MAP_BLOCK_CHAIN, MAX_METADATA_OFFSET);
        dataBlocksStorage.putInt(maxOffset, maxMetadata);
    }

    private int metadataOffset(int index) {
        int blockIndex = index / metadataPerBlock;
        int metadataIndexInBlock = index % metadataPerBlock;
        return blockIndex * blockManager.getBlockSize() + metadataIndexInBlock * MappedMetadata.BYTES;
    }

    private class MappedMetadata implements Metadata {

        private static final int BYTES = 12;
        private static final int FIELD_SIZE = 4;

        private static final int TYPE_INDEX = 0;
        private static final int DATA_LENGTH_INDEX = 1;
        private static final int FIRST_BLOCK_INDEX = 2;

        private final int id;
        private final int offset;

        volatile private Integer dataLength;
        volatile private Integer firstBlock;
        volatile private Type type;

        MappedMetadata(int id) throws IOException {
            this.id = id;
            this.offset = blockManager.ensureGlobalOffset(METADATA_BLOCK_CHAIN, metadataOffset(id));
        }

        @Override
        public int getDataLength() throws IOException {
            if(dataLength == null) {
                dataLength = readField(DATA_LENGTH_INDEX);
            }
            return dataLength;
        }

        @Override
        public void setDataLength(int length) throws IOException {
            dataLength = length;
            writeField(DATA_LENGTH_INDEX, length);
        }

        @Override
        public int getFirstBlock() throws IOException {
            if(firstBlock == null) {
                firstBlock = readField(FIRST_BLOCK_INDEX);
            }
            return firstBlock;
        }

        @Override
        public void setFirstBlock(int block) throws IOException {
            firstBlock = block;
            writeField(FIRST_BLOCK_INDEX, block);
        }

        @Override
        public Type getType() throws IOException {
            if(type == null) {
                type = Type.valueOf(readField(TYPE_INDEX));
            }
            return type;
        }

        @Override
        public void setType(Type type) throws IOException {
            this.type = type;
            writeField(TYPE_INDEX, type.value);
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

        private int readField(int index) throws IOException {
            return dataBlocksStorage.getInt(offset + index * FIELD_SIZE);
        }

        private void writeField(int index, int value) throws IOException {
            dataBlocksStorage.putInt(offset + index * FIELD_SIZE, value);
        }
    }
}
