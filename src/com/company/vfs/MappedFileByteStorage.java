package com.company.vfs;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

class MappedFileByteStorage implements ByteStorage {


    private final String path;
    private final int offset;
    private final int length;
    private ByteBuffer byteBuffer;

    MappedFileByteStorage(String path, int offset, int length) {

        this.path = path;
        this.offset = offset;
        this.length = length;
    }

    @Override
    public void putByte(int offset, byte value) throws IOException {
        ensureMapping();
        byteBuffer.put(offset, value);
    }

    @Override
    public byte getByte(int offset) throws IOException {
        ensureMapping();
        return byteBuffer.get(offset);
    }

    @Override
    public int getInt(int offset) throws IOException {
        ensureMapping();
        return byteBuffer.getInt(offset);
    }

    @Override
    public void putInt(int offset, int value) throws IOException {
        ensureMapping();
        byteBuffer.putInt(offset, value);
    }

    @Override
    public byte[] getBytes(int offset, int length) throws IOException {
        ensureMapping();

        byte[] result = new byte[length];
        for (int i = 0; i < length; i++)
            result[i] = byteBuffer.get(offset + i);

        return result;
    }

    @Override
    public void putBytes(int offset, byte[] bytes) throws IOException {
        ensureMapping();
        for (int i = 0; i < bytes.length; ++i) {
            byteBuffer.put(offset + i, bytes[i]);
        }
    }

    private void ensureMapping() throws IOException {
        if(byteBuffer != null) {
            return;
        }

        try(RandomAccessFile randomAccessFile = new RandomAccessFile(path, "rw");
            FileChannel channel = randomAccessFile.getChannel()) {
            if(randomAccessFile.length() < offset + length) {
                randomAccessFile.setLength(offset + length);
            }
            byteBuffer = channel.map(FileChannel.MapMode.READ_WRITE, offset, length);
        }
    }
}
