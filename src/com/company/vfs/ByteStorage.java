package com.company.vfs;

import java.io.IOException;

interface ByteStorage {

    void putByte(int offset, byte value) throws IOException;

    byte getByte(int offset) throws IOException;

    int getInt(int offset) throws IOException;

    void putInt(int offset, int value) throws IOException;

    byte[] getBytes(int offset, int length) throws IOException;

    ByteStorage slice(int offset, int length);
}
