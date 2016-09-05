package com.company.vfs;

import java.io.IOException;

class BlockChainByteStorage implements ByteStorage {

    private final ByteStorage byteStorage;
    private final BlockManager blockManager;
    private final int firstBlock;

    BlockChainByteStorage(ByteStorage byteStorage, BlockManager blockManager, int firstBlock) {

        this.byteStorage = byteStorage;
        this.blockManager = blockManager;
        this.firstBlock = firstBlock;
    }

    @Override
    public void putByte(int offset, byte value) throws IOException {
        offset = blockManager.ensureGlobalOffset(firstBlock, offset);
        byteStorage.putByte(offset, value);
    }

    @Override
    public byte getByte(int offset) throws IOException {
        offset = blockManager.ensureGlobalOffset(firstBlock, offset);
        return byteStorage.getByte(offset);
    }

    @Override
    public int getInt(int offset) throws IOException {
        int ch1 = getByte(offset) & 0xFF;
        int ch2 = getByte(offset + 1) & 0xFF;
        int ch3 = getByte(offset + 2) & 0xFF;
        int ch4 = getByte(offset + 3) & 0xFF;

        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4));
    }

    @Override
    public void putInt(int offset, int value) throws IOException {
        putByte(offset, (byte) ((value >>> 24) & 0xFF));
        putByte(offset + 1, (byte) ((value >>> 16) & 0xFF));
        putByte(offset + 2, (byte) ((value >>>  8) & 0xFF));
        putByte(offset + 3, (byte) ((value) & 0xFF));
    }

    @Override
    public byte[] getBytes(int offset, int length) throws IOException {
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++)
            result[i] = getByte(offset + i);

        return result;
    }

    @Override
    public void putBytes(int offset, byte[] bytes) throws IOException {
        for(int i = 0; i < bytes.length; ++i) {
            putByte(offset + i, bytes[i]);
        }
    }
}
