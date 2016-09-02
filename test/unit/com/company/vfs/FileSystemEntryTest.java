package com.company.vfs;

import com.company.vfs.exception.FileFormatException;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import static org.junit.Assert.*;

public class FileSystemEntryTest {

    @Test
    public void readWrite() throws Exception {
        FileSystemEntry entry = new FileSystemEntry(42, "Batman");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);

        entry.write(dataOutputStream);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        FileSystemEntry result = FileSystemEntry.read(dataInputStream);

        assertEquals(entry.getName(), result.getName());
        assertEquals(entry.getMetadataId(), result.getMetadataId());
    }

    @Test(expected = FileFormatException.class)
    public void readShouldThrow_When_InvalidData() throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[] { 0, 0, 0, 0, 0, 0, 0, 100} );
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        FileSystemEntry.read(dataInputStream);
    }

    @Test(expected = FileFormatException.class)
    public void readShouldThrow_When_NotEnoughData() throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[] { } );
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        FileSystemEntry.read(dataInputStream);
    }

}