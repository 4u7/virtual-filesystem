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

    @After
    public void tearDown() throws Exception {
        removeFilesystemFile();
    }
}