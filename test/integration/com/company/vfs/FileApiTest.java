package com.company.vfs;

import com.company.vfs.exception.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.Arrays;

import static com.company.vfs.Utils.FILESYSTEM_FILENAME;
import static com.company.vfs.Utils.removeFilesystemFile;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class FileApiTest {

    private static final String[] DIRECTORIES = { "foo", "bar", "baz", "竜座" };

    private static final String[] INNER_DIRECTORIES = { "lorem ipsum dolor sit amet",
            "Новая Папка 547", "hyvää huomenta", "คนอ้วน" };

    private static final String DEMO_TEXT =
            "char*s=\"char*s=%c%s%c;main(){printf(s,34,s,34);}\";main(){printf(s,34,s,34);}";

    @Before
    public void setUp() throws Exception {
        removeFilesystemFile();

        FileSystem fs = VirtualFileSystem.create(FILESYSTEM_FILENAME, 1024);

        for(String dir : DIRECTORIES) {
            fs.createDirectory(dir);
            for(String innerDir: INNER_DIRECTORIES) {
                fs.createDirectory(dir + "/" + innerDir);
            }
        }

        try(OutputStream outputStream = fs.createFile("/foo/Новая Папка 547/quine.cpp");
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            BufferedWriter writer = new BufferedWriter(outputStreamWriter))
        {
            writer.write(DEMO_TEXT);
        }

        fs.createFile("base.db").close();
        fs.createFile("/foo/empty").close();
        fs.createFile("/bar/data").close();
    }

    @Test
    public void readFile() throws Exception {
        FileSystem fs = VirtualFileSystem.open(FILESYSTEM_FILENAME);
        try(InputStream inputStream = fs.readFile("/foo/Новая Папка 547/quine.cpp");
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(inputStreamReader)) {

            assertThat(reader.readLine(), is(DEMO_TEXT));
        }
    }

    @Test
    public void readSameFile() throws Exception {
        FileSystem fs = VirtualFileSystem.open(FILESYSTEM_FILENAME);
        try(InputStream inputStream1 = fs.readFile("/foo/Новая Папка 547/quine.cpp");
            InputStream inputStream2 = fs.readFile("/foo/Новая Папка 547/quine.cpp")) {

            while (inputStream1.available() > 0 && inputStream2.available() > 0) {
                assertEquals(inputStream1.read(), inputStream2.read());
            }
        }
    }

    @Test
    public void writeFile() throws Exception {
        FileSystem fs = VirtualFileSystem.open(FILESYSTEM_FILENAME);
        try(OutputStream outputStream = fs.writeFile("/bar/data", true);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            BufferedWriter writer = new BufferedWriter(outputStreamWriter))
        {
            writer.write("some string");
            writer.newLine();
            writer.write("another line");
        }

        try(InputStream inputStream = fs.readFile("/bar/data");
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(inputStreamReader)) {

            assertThat(reader.readLine(), is("some string"));
            assertThat(reader.readLine(), is("another line"));
        }


        try(OutputStream outputStream = fs.writeFile("/bar/data", false);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            BufferedWriter writer = new BufferedWriter(outputStreamWriter))
        {
            writer.write("some long string which should overwrite previous lines");
        }

        try(InputStream inputStream = fs.readFile("/bar/data");
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(inputStreamReader)) {

            assertThat(reader.readLine(), is("some long string which should overwrite previous lines"));
        }

    }

    @Test
    public void exists() throws Exception {
        FileSystem fs = VirtualFileSystem.open(FILESYSTEM_FILENAME);
        fs.createFile("log.txt").close();

        assertTrue(fs.exists("/foo/Новая Папка 547/quine.cpp"));
        assertTrue(fs.exists("/log.txt"));
        assertTrue(fs.exists("/base.db"));
        assertTrue(fs.exists("foo/empty"));
        assertTrue(fs.exists("bar/data"));
    }

    @Test
    public void isFile() throws Exception {
        FileSystem fs = VirtualFileSystem.open(FILESYSTEM_FILENAME);
        fs.createFile("log.txt").close();

        assertFalse(fs.isFile("/"));
        assertTrue(fs.isFile("/foo/Новая Папка 547/quine.cpp"));
        assertTrue(fs.isFile("/log.txt"));
        assertTrue(fs.isFile("/base.db"));
        assertTrue(fs.isFile("foo/empty"));
        assertTrue(fs.isFile("bar/data"));
    }

    @Test
    public void getFiles() throws Exception {
        FileSystem fs = VirtualFileSystem.open(FILESYSTEM_FILENAME);
        fs.createFile("somefile.txt").close();

        assertThat(fs.getFiles("/"), is(Arrays.asList("base.db", "somefile.txt")));
    }

    @Test(expected = FileAlreadyExistsException.class)
    public void createFileShouldThrow_When_FileAlreadyExists() throws Exception {
        FileSystem fs = VirtualFileSystem.open(FILESYSTEM_FILENAME);
        fs.createFile("base.db");
    }

    @Test(expected = FileAlreadyExistsException.class)
    public void createFileShouldThrow_When_TargetIsDirectory() throws Exception {
        FileSystem fs = VirtualFileSystem.open(FILESYSTEM_FILENAME);
        fs.createFile("foo");
    }

    @Test(expected = NotDirectoryException.class)
    public void getFilesShouldThrow_When_TargetIsNotDirectory() throws Exception {
        FileSystem fs = VirtualFileSystem.open(Utils.FILESYSTEM_FILENAME);
        fs.createFile("log.txt").close();
        fs.getFiles("log.txt");
    }

    @Test(expected = NoSuchFileException.class)
    public void getFilesShouldThrow_When_NoParent() throws Exception {
        FileSystem fs = VirtualFileSystem.open(Utils.FILESYSTEM_FILENAME);
        fs.getFiles("nothing/log.txt");
    }

    @Test(expected = NotDirectoryException.class)
    public void createFileShouldThrow_When_TargetIsNotDirectory() throws Exception {
        FileSystem fs = VirtualFileSystem.open(Utils.FILESYSTEM_FILENAME);
        fs.createFile("log.txt").close();
        fs.createFile("log.txt/file");
    }

    @Test(expected = NoSuchFileException.class)
    public void createFileShouldThrow_When_NoTargetDirectory() throws Exception {
        FileSystem fs = VirtualFileSystem.open(Utils.FILESYSTEM_FILENAME);
        fs.createFile("nothing/file");
    }

    @Test(expected = FileAlreadyExistsException.class)
    public void createFileShouldThrow_When_TargetIsRoot() throws Exception {
        FileSystem fs = VirtualFileSystem.open(Utils.FILESYSTEM_FILENAME);
        fs.createFile("/");
    }

    @Test(expected = NotFileException.class)
    public void readFileShouldThrow_When_TargetIsNotFile() throws Exception {
        FileSystem fs = VirtualFileSystem.open(Utils.FILESYSTEM_FILENAME);
        fs.readFile("foo");
    }

    @Test(expected = NotFileException.class)
    public void writeFileShouldThrow_When_TargetIsNotFile() throws Exception {
        FileSystem fs = VirtualFileSystem.open(Utils.FILESYSTEM_FILENAME);
        fs.writeFile("foo", true);
    }

    @Test(expected = NoSuchFileException.class)
    public void readFileShouldThrow_When_NoFile() throws Exception {
        FileSystem fs = VirtualFileSystem.open(Utils.FILESYSTEM_FILENAME);
        fs.readFile("foo/nothing/file");
    }

    @Test(expected = NoSuchFileException.class)
    public void writeFileShouldThrow_When_NoFile() throws Exception {
        FileSystem fs = VirtualFileSystem.open(Utils.FILESYSTEM_FILENAME);
        fs.writeFile("foo/nothing/file", true);
    }

    @Test(expected = ClosedStreamException.class)
    public void writingToClosedStreamShouldThrow() throws Exception {
        FileSystem fs = VirtualFileSystem.open(Utils.FILESYSTEM_FILENAME);
        OutputStream outputStream = fs.writeFile("base.db", true);
        outputStream.close();
        outputStream.write(42);
    }

    @Test(expected = ClosedStreamException.class)
    public void readingFromClosedStreamShouldThrow() throws Exception {
        FileSystem fs = VirtualFileSystem.open(Utils.FILESYSTEM_FILENAME);
        InputStream inputStream = fs.readFile("base.db");
        inputStream.close();
        inputStream.read();
    }

    @After
    public void tearDown() throws Exception {
        removeFilesystemFile();
    }
}
