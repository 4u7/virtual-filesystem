package com.company.vfs;

import java.io.IOException;

class SynchronizedByteStorage implements ByteStorage {

    private final ByteStorage byteStorage;

    SynchronizedByteStorage(ByteStorage byteStorage) {
        this.byteStorage = byteStorage;
    }

    @Override
    synchronized public void putByte(int offset, byte value) throws IOException {
        byteStorage.putByte(offset, value);
    }

    @Override
    synchronized public byte getByte(int offset) throws IOException {
        return byteStorage.getByte(offset);
    }

    @Override
    synchronized public int getInt(int offset) throws IOException {
        return byteStorage.getInt(offset);
    }

    @Override
    synchronized public void putInt(int offset, int value) throws IOException {
        byteStorage.putInt(offset, value);
    }

    @Override
    synchronized public void getBytes(int offset, byte[] dst) throws IOException {
        byteStorage.getBytes(offset, dst);
    }

    @Override
    synchronized public void getBytes(int offset, byte[] dst, int dstOffset, int length) throws IOException {
        byteStorage.getBytes(offset, dst, dstOffset, length);
    }

    @Override
    synchronized public void putBytes(int offset, byte[] source) throws IOException {
        byteStorage.putBytes(offset, source);
    }

    @Override
    synchronized public void putBytes(int offset, byte[] source, int sourceOffset, int length) throws IOException {
        byteStorage.putBytes(offset, source, sourceOffset, length);
    }
}
