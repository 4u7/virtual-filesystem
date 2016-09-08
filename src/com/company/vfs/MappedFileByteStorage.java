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
    public void getBytes(int offset, byte[] dst) throws IOException {
        byteBuffer.position(offset);
        byteBuffer.get(dst);
    }

    @Override
    public void getBytes(int offset, byte[] dst, int dstOffset, int length) {
        byteBuffer.position(offset);
        byteBuffer.get(dst, dstOffset, length);
    }

    @Override
    public void putBytes(int offset, byte[] source) throws IOException {
        byteBuffer.position(offset);
        byteBuffer.put(source);
    }

    @Override
    public void putBytes(int offset, byte[] source, int sourceOffset, int length) {
        byteBuffer.position(offset);
        byteBuffer.put(source, sourceOffset, length);
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
