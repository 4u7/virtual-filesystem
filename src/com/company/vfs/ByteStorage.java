package com.company.vfs;

import java.io.IOException;

interface ByteStorage {

    void putByte(int offset, byte value) throws IOException;

    byte getByte(int offset) throws IOException;

    int getInt(int offset) throws IOException;

    void putInt(int offset, int value) throws IOException;

    void getBytes(int offset, byte[] dst) throws IOException;

    void getBytes(int offset, byte[] dst, int dstOffset, int length) throws IOException;

    void putBytes(int offset, byte[] source) throws IOException;

    void putBytes(int offset, byte[] source, int sourceOffset, int length) throws IOException;
}
