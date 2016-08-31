package com.company.vfs;

import java.io.IOException;
import java.io.OutputStream;

class EntryOutputStream extends OutputStream {

    private final Metadata metadata;
    private final ByteStorage blockByteStorage;
    private final BlockManager blockManager;

    private int position = 0;

    EntryOutputStream(Metadata metadata, ByteStorage blockByteStorage, BlockManager blockManager, boolean append)
            throws IOException {
        this.metadata = metadata;
        this.blockByteStorage = blockByteStorage;
        this.blockManager = blockManager;

        if(append) {
            position = metadata.getDataLength();
        }
    }

    @Override
    public void write(int b) throws IOException {
        int offset = blockManager.ensureBlockOffset(metadata, position);
        blockByteStorage.putByte(offset, (byte)b);
        ++position;

        metadata.updateDataLength(position);
    }
}
