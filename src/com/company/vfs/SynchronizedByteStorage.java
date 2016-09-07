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
    synchronized public byte[] getBytes(int offset, int length) throws IOException {
        return byteStorage.getBytes(offset, length);
    }

    @Override
    synchronized public void putBytes(int offset, byte[] bytes) throws IOException {
        byteStorage.putBytes(offset, bytes);
    }

    @Override
    synchronized public void putBytes(int offset, byte[] bytes, int offsetInBytes, int length) throws IOException {
        byteStorage.putBytes(offset, bytes, offsetInBytes, length);
    }
}
