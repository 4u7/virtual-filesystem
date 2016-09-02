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

        FileSystem fs = VirtualFileSystem.create(FILESYSTEM_FILENAME)
                .maxBlocks(1024)
                .maxEntries(1024)
                .build();
    }

    @Test
    public void exists() throws Exception {
        FileSystem fs;

        fs = VirtualFileSystem.open(FILESYSTEM_FILENAME);
        fs.createFile("log.txt");
        fs.createDirectory("foo");
        fs.createFile("foo/log.txt");

        fs = VirtualFileSystem.open(FILESYSTEM_FILENAME);
        assertTrue(fs.exists("log.txt"));
        assertTrue(fs.exists("foo"));
        assertTrue(fs.exists("foo/log.txt"));
        assertTrue(fs.isDirectory("foo"));
    }

    @After
    public void tearDown() throws Exception {
        removeFilesystemFile();
    }
}