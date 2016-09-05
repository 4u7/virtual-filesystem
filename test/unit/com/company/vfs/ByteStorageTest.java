package com.company.vfs;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class ByteStorageTest {

    @Test
    public void reading() throws Exception {
        byte[] data = "lorem ipsum dolor sit amet some random data to test".getBytes();
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        ByteBufferByteStorage storage = new ByteBufferByteStorage(byteBuffer);
        SynchronizedByteStorage synchronizedStorage = new SynchronizedByteStorage(storage);

        assertEquals(byteBuffer.get(5), synchronizedStorage.getByte(5));
        assertEquals(byteBuffer.getInt(10), synchronizedStorage.getInt(10));

        byte[] buff = new byte[20];
        byteBuffer.get(buff);

        assertThat(buff, is(synchronizedStorage.getBytes(0, 20)));
    }

    @Test
    public void writing() throws Exception {
        byte[] data = new byte[100];
        ByteBuffer testBuffer = ByteBuffer.allocate(100);

        ByteBufferByteStorage storage = new ByteBufferByteStorage(ByteBuffer.wrap(data));
        SynchronizedByteStorage synchronizedStorage = new SynchronizedByteStorage(storage);

        synchronizedStorage.putInt(0, 42);
        byte[] testData = "lorem ipsum dolor sit amet some random data to test".getBytes();
        for(int i = 0; i < testData.length; ++i) {
            synchronizedStorage.putByte(i + 4, testData[i]);
        }

        testBuffer.putInt(0, 42);
        for(int i = 0; i < testData.length; ++i) {
            testBuffer.put(i + 4, testData[i]);
        }

        assertThat(testBuffer.array(), is(data));
    }

    @Test
    public void slice() throws Exception {
        byte[] data = "lorem ipsum dolor sit amet some random data to test".getBytes();
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        ByteStorage storage = new ByteBufferByteStorage(byteBuffer);
        ByteStorage synchronizedStorage = new SynchronizedByteStorage(storage);

        byteBuffer.position(5);
        byteBuffer = byteBuffer.slice();
        storage = storage.slice(5, 20);
        synchronizedStorage = synchronizedStorage.slice(5, 20);
        for (int i = 0; i < 20; ++i) {
            assertEquals(byteBuffer.get(i), synchronizedStorage.getByte(i));
            assertEquals(byteBuffer.get(i), storage.getByte(i));
        }

        for (int i = 0; i < 20; ++i) {
            assertEquals(byteBuffer.getInt(i), synchronizedStorage.getInt(i));
            assertEquals(byteBuffer.getInt(i), storage.getInt(i));
        }
    }

}