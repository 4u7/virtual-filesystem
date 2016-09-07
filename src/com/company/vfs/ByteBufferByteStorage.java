package com.company.vfs;

import java.io.IOException;
import java.nio.ByteBuffer;

class ByteBufferByteStorage implements ByteStorage {

    private final ByteBuffer buffer;

    ByteBufferByteStorage(ByteBuffer buffer) {

        this.buffer = buffer;
    }

    @Override
    public void putByte(int offset, byte value) throws IOException {
        buffer.put(offset, value);
    }

    @Override
    public byte getByte(int offset) throws IOException {
        return buffer.get(offset);
    }

    @Override
    public int getInt(int offset) throws IOException {
        return buffer.getInt(offset);
    }

    @Override
    public void putInt(int offset, int value) throws IOException {
        buffer.putInt(offset, value);
    }

    @Override
    public byte[] getBytes(int offset, int length) throws IOException {
        buffer.position(offset);
        byte[] result = new byte[length];
        buffer.get(result);
        return result;
    }

    @Override
    public void putBytes(int offset, byte[] bytes) throws IOException {
        buffer.position(offset);
        buffer.put(bytes);
    }

    @Override
    public void putBytes(int offset, byte[] bytes, int offsetInBytes, int length) {
        buffer.position(offset);
        buffer.put(bytes, offsetInBytes, length);
    }
}
