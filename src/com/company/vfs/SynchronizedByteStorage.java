package com.company.vfs;

import java.io.IOException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SynchronizedByteStorage implements ByteStorage {

    private final ByteStorage byteStorage;
    private final ReadWriteLock lock;

    public SynchronizedByteStorage(ByteStorage byteStorage) {

        this.byteStorage = byteStorage;
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public void putByte(int offset, byte value) throws IOException {
        lock.writeLock().lock();
        try {
            byteStorage.putByte(offset, value);
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public byte getByte(int offset) throws IOException {
        lock.readLock().lock();
        try {
            return byteStorage.getByte(offset);
        }
        finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int getInt(int offset) {
        return 0;
    }

    @Override
    public void putInt(int offset, int value) {

    }

    @Override
    public byte[] getBytes(int offset, int length) {
        return new byte[0];
    }

    @Override
    public ByteStorage slice(int offset, int length) {
        return null;
    }
}
