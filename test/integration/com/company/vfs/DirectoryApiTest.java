package com.company.vfs;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static com.company.vfs.Utils.FILESYSTEM_FILENAME;
import static com.company.vfs.Utils.removeFilesystemFile;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class DirectoryApiTest {

    private static final String[] DIRECTORIES = { "foo", "bar", "baz", "竜座" };

    private static final String[] INNER_DIRECTORIES = { "lorem ipsum dolor sit amet",
            "Новая Папка 547", "hyvää huomenta", "คนอ้วน" };

    @Before
    public void setUp() throws Exception {
        FileSystem fs = VirtualFileSystem.create(FILESYSTEM_FILENAME)
                .maxBlocks(1024)
                .maxEntries(1024)
                .build();

        for(String dir : DIRECTORIES) {
            fs.createDirectory(dir);
            for(String innerDir: INNER_DIRECTORIES) {
                fs.createDirectory(dir + "/" + innerDir);
            }
        }
    }

    @Test
    public void getDirectories() throws Exception {
        FileSystem fs = VirtualFileSystem.open(Utils.FILESYSTEM_FILENAME);
        assertThat(fs.getDirectories("/"), is(Arrays.asList(DIRECTORIES)));
        for(String dir : DIRECTORIES) {
            assertThat(fs.getDirectories(dir), is(Arrays.asList(INNER_DIRECTORIES)));
        }
    }

    @Test
    public void exists() throws Exception {
        FileSystem fs = VirtualFileSystem.open(Utils.FILESYSTEM_FILENAME);

        assertTrue(fs.exists("/"));
        assertTrue(fs.exists(""));

        for(String dir : DIRECTORIES) {
            assertTrue(fs.exists(dir));
        }
    }

    @Test
    public void isDirectory() throws Exception {
        FileSystem fs = VirtualFileSystem.open(Utils.FILESYSTEM_FILENAME);

        assertTrue(fs.isDirectory("/"));
        assertTrue(fs.isDirectory(""));

        for(String dir : DIRECTORIES) {
            assertTrue(fs.isDirectory(dir));
            for(String innerDir: INNER_DIRECTORIES) {
                assertTrue(fs.exists(dir + "/" + innerDir));
            }
        }
    }

    @After
    public void tearDown() throws Exception {
        removeFilesystemFile();
    }
}