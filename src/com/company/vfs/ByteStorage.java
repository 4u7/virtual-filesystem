package com.company.vfs;

interface ByteStorage {

    void putByte(int offset, byte value);

    byte getByte(int offset);

    int getInt(int offset);

    void putInt(int offset, int value);
}
