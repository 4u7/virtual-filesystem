package com.company.vfs;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.company.vfs.Utils.FILESYSTEM_FILENAME;
import static com.company.vfs.Utils.removeFilesystemFile;
import static org.junit.Assert.*;

public class VirtualFileSystemTest {

    @Before
    public void setUp() throws Exception {
        removeFilesystemFile();

        VirtualFileSystem.create(FILESYSTEM_FILENAME)
                .maxBlocks(1024)
                .maxEntries(1024)
                .build();
    }

    @Test
    public void exists() throws Exception {

        FileSystem fs1 = VirtualFileSystem.open(FILESYSTEM_FILENAME);
        fs1.createFile("log.txt");
        fs1.createDirectory("foo");
        fs1.createFile("foo/log.txt");

        FileSystem fs2 = VirtualFileSystem.open(FILESYSTEM_FILENAME);
        assertTrue(fs2.exists("log.txt"));
        assertTrue(fs2.exists("foo"));
        assertTrue(fs2.exists("foo/log.txt"));
        assertTrue(fs2.isDirectory("foo"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void openShouldThrow() throws Exception {
        VirtualFileSystem.open(null);
    }


    @Test(expected = IllegalArgumentException.class)
    public void createShouldThrow() throws Exception {
        VirtualFileSystem.create(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getDirectoriesShouldThrow() throws Exception {
        FileSystem fs = VirtualFileSystem.open(FILESYSTEM_FILENAME);
        fs.getDirectories(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getFilesShouldThrow() throws Exception {
        FileSystem fs = VirtualFileSystem.open(FILESYSTEM_FILENAME);
        fs.getFiles(null);

    }

    @Test(expected = IllegalArgumentException.class)
    public void existsShouldThrow() throws Exception {
        FileSystem fs = VirtualFileSystem.open(FILESYSTEM_FILENAME);
        fs.exists(null);

    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteShouldThrow() throws Exception {
        FileSystem fs = VirtualFileSystem.open(FILESYSTEM_FILENAME);
        fs.delete(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void isDirectoryShouldThrow() throws Exception {
        FileSystem fs = VirtualFileSystem.open(FILESYSTEM_FILENAME);
        fs.isDirectory(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void isFileShouldThrow() throws Exception {
        FileSystem fs = VirtualFileSystem.open(FILESYSTEM_FILENAME);
        fs.isFile(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createDirectoryShouldThrow() throws Exception {
        FileSystem fs = VirtualFileSystem.open(FILESYSTEM_FILENAME);
        fs.createDirectory(null);
    }
    @Test(expected = IllegalArgumentException.class)
    public void createFileShouldThrow() throws Exception {
        FileSystem fs = VirtualFileSystem.open(FILESYSTEM_FILENAME);
        fs.createFile(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void writeFileShouldThrow() throws Exception {
        FileSystem fs = VirtualFileSystem.open(FILESYSTEM_FILENAME);
        fs.writeFile(null, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void readFileShouldThrow() throws Exception {
        FileSystem fs = VirtualFileSystem.open(FILESYSTEM_FILENAME);
        fs.readFile(null);
    }

    @After
    public void tearDown() throws Exception {
        removeFilesystemFile();
    }
}