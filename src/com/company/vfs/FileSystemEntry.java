package com.company.vfs;

import com.company.vfs.exception.FileFormatException;

import java.io.*;

class FileSystemEntry {

    private final int metadataId;
    private final String name;

    FileSystemEntry(int metadataId, String name) {

        this.name = name;
        this.metadataId = metadataId;
    }

    String getName() {
        return name;
    }

    int getMetadataId() {
        return metadataId;
    }

    static FileSystemEntry read(DataInputStream dataInputStream) throws IOException {
        try {
            int metadataId = dataInputStream.readInt();
            int nameByteLength = dataInputStream.readInt();
            byte nameBytes[] = new byte[nameByteLength];
            if (dataInputStream.read(nameBytes) == -1) {
                throw new FileFormatException("Invalid file system entry format.");
            }
            return new FileSystemEntry(metadataId, new String(nameBytes, "UTF-8"));
        }
        catch (EOFException e) {
            throw new FileFormatException("Invalid file system entry format.");
        }
    }

    void write(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeInt(metadataId);
        byte[] nameBytes = name.getBytes("UTF-8");
        dataOutputStream.writeInt(nameBytes.length);
        dataOutputStream.write(nameBytes);
    }
}
