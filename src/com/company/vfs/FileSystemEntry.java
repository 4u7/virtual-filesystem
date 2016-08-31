package com.company.vfs;

import com.company.vfs.exception.FileFormatException;

import java.io.*;

public class FileSystemEntry {

    private final int metadataId;
    private final String name;

    public FileSystemEntry(int metadataId, String name) {

        this.name = name;
        this.metadataId = metadataId;
    }

    public String getName() {
        return name;
    }

    public int getMetadataId() {
        return metadataId;
    }

    public static FileSystemEntry read(InputStream inputStream) throws IOException {
        try(DataInputStream dataInputStream = new DataInputStream(inputStream)) {
            int metadataId = dataInputStream.readInt();
            int nameByteLength = dataInputStream.readInt();
            byte nameBytes[] = new byte[nameByteLength];
            if(dataInputStream.read(nameBytes) == -1) {
                throw new FileFormatException("Invalid file system entry format.");
            }
            return new FileSystemEntry(metadataId, new String(nameBytes, "UTF-8"));
        }
    }

    public void write(OutputStream outputStream) throws IOException {
        try(DataOutputStream dataOutputStream = new DataOutputStream(outputStream)) {
            dataOutputStream.writeInt(metadataId);

            byte[] nameBytes = name.getBytes("UTF-8");
            dataOutputStream.writeInt(nameBytes.length);
            dataOutputStream.write(nameBytes);
        }
    }
}
