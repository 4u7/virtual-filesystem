package com.company.vfs;

interface Metadata {

    int getDataLength();

    void updateDataLength(int length);

    int getFirstBlock();

    void setFirstBlock(int currentBlock);
}
