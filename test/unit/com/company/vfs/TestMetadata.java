package com.company.vfs;

import com.company.vfs.Metadata;

import java.io.IOException;

class TestMetadata implements Metadata {

    int dataLength = 0;
    int firstBlock = -1;
    Type type = Type.Undefined;

    @Override
    public int getDataLength() throws IOException {
        return dataLength;
    }

    @Override
    public void setDataLength(int length) throws IOException {
        dataLength = length;
    }

    @Override
    public void updateDataLength(int length) throws IOException {
        if(length > dataLength) {
            dataLength = length;
        }
    }

    @Override
    public int getFirstBlock() throws IOException {
        return firstBlock;
    }

    @Override
    public void setFirstBlock(int block) throws IOException {
        firstBlock = block;
    }

    @Override
    public Type getType() throws IOException {
        return null;
    }

    @Override
    public int getId() {
        return 0;
    }
}
