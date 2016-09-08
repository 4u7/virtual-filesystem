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
    public void getBytes(int offset, byte[] dst) throws IOException {
        buffer.position(offset);
        buffer.get(dst);
    }

    @Override
    public void getBytes(int offset, byte[] dst, int dstOffset, int length) throws IOException {
        buffer.position(offset);
        buffer.get(dst, dstOffset, length);
    }

    @Override
    public void putBytes(int offset, byte[] source) throws IOException {
        buffer.position(offset);
        buffer.put(source);
    }

    @Override
    public void putBytes(int offset, byte[] source, int sourceOffset, int length) {
        buffer.position(offset);
        buffer.put(source, sourceOffset, length);
    }
}
