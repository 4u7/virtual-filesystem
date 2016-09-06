package com.company.vfs;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;

import static com.company.vfs.Utils.FILESYSTEM_FILENAME;
import static com.company.vfs.Utils.removeFilesystemFile;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class LongFileTest {

    @Before
    public void setUp() throws Exception {
        removeFilesystemFile();
    }

    @Test
    public void longFileWriteReadTest() throws Exception {
        FileSystem fs = VirtualFileSystem.create(FILESYSTEM_FILENAME, 1024);

        String filename = "huge.txt";
        String data = "line of text";
        int writeTimes = 100000;

        try(OutputStream outputStream = fs.createFile(filename);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            BufferedWriter writer = new BufferedWriter(outputStreamWriter))
        {
            for(int i = 0; i < writeTimes; ++i) {
                writer.write(data);
                writer.newLine();
            }
        }

        fs = VirtualFileSystem.open(FILESYSTEM_FILENAME);
        try(InputStream inputStream = fs.readFile(filename);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(inputStreamReader)) {

            int expectedLength = (data.getBytes().length + "\n".getBytes().length) * writeTimes;
            assertThat(inputStream.available(), is(expectedLength));

            for(int i = 0; i < writeTimes; ++i) {
                assertThat(reader.readLine(), is(data));
            }
        }
    }

    @After
    public void tearDown() throws Exception {
        removeFilesystemFile();
    }

}