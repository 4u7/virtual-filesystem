package com.company.vfs;

import org.junit.After;
import org.junit.Test;

import java.io.*;

import static com.company.vfs.Utils.FILESYSTEM_FILENAME;
import static com.company.vfs.Utils.removeFilesystemFile;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class LongFileTest {

    @Test
    public void longFileWriteReadTest() throws Exception {
        FileSystem fs = VirtualFileSystem.create(FILESYSTEM_FILENAME)
                .maxBlocks(1024)
                .maxEntries(1024)
                .build();


        try(OutputStream outputStream = fs.createFile("huge.txt");
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            BufferedWriter writer = new BufferedWriter(outputStreamWriter))
        {
            for(int i = 0; i < 100000; ++i) {
                writer.write("line of text");
                writer.newLine();
            }
        }

        fs = VirtualFileSystem.open(FILESYSTEM_FILENAME);
        try(InputStream inputStream = fs.readFile("huge.txt");
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(inputStreamReader)) {

            for(int i = 0; i < 100000; ++i) {
                assertThat(reader.readLine(), is("line of text"));
            }
        }
    }

    @After
    public void tearDown() throws Exception {
        removeFilesystemFile();
    }

}