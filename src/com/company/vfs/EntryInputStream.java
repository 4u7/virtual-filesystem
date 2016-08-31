package com.company.vfs;

import java.io.IOException;
import java.io.InputStream;

public class EntryInputStream extends InputStream {

    private final Metadata metadata;
    private final ByteStorage blockByteStorage;
    private final BlockManager blockManager;

    private int position = 0;

    EntryInputStream(Metadata metadata, ByteStorage blockByteStorage, BlockManager blockManager) {

        this.metadata = metadata;
        this.blockByteStorage = blockByteStorage;
        this.blockManager = blockManager;
    }

    @Override
    public int read() throws IOException {

        if(position >= metadata.getDataLength()) {
            return -1;
        }

        int offset = blockManager.getGlobalOffset(metadata, position);
        int result = blockByteStorage.getByte(offset) & 0xFF;
        ++position;

        return result;
    }
}
