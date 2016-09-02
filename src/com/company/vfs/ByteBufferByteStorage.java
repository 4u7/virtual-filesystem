package com.company.vfs;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ByteBufferByteStorage implements ByteStorage {

    private final ByteBuffer buffer;

    public ByteBufferByteStorage(ByteBuffer buffer) {

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
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++)
            result[i] = buffer.get(offset + i);

        return result;
    }

    @Override
    public ByteStorage slice(int offset, int length) {
        buffer.position(offset);
        ByteStorage result =  new ByteBufferByteStorage(buffer.slice());
        buffer.position(0);
        return result;
    }
}
