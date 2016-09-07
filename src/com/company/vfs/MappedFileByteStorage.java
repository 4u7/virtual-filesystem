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

    MappedFileByteStorage(String path, int offset, int length) throws IOException {

        this.path = path;
        this.offset = offset;
        this.length = length;

        ensureMapping();
    }

    @Override
    public void putByte(int offset, byte value) throws IOException {
        byteBuffer.put(offset, value);
    }

    @Override
    public byte getByte(int offset) throws IOException {
        return byteBuffer.get(offset);
    }

    @Override
    public int getInt(int offset) throws IOException {
        return byteBuffer.getInt(offset);
    }

    @Override
    public void putInt(int offset, int value) throws IOException {
        byteBuffer.putInt(offset, value);
    }

    @Override
    public byte[] getBytes(int offset, int length) throws IOException {
        byteBuffer.position(offset);
        byte[] result = new byte[length];
        byteBuffer.get(result);
        return result;
    }

    @Override
    public void putBytes(int offset, byte[] bytes) throws IOException {
        byteBuffer.position(offset);
        byteBuffer.put(bytes);
    }

    @Override
    public void putBytes(int offset, byte[] bytes, int offsetInBytes, int length) {
        byteBuffer.position(offset);
        byteBuffer.put(bytes, offsetInBytes, length);
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
