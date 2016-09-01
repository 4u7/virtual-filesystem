package com.company.vfs;

import com.company.vfs.exception.AccessDeniedException;
import com.company.vfs.exception.DirectoryNotEmptyException;
import com.company.vfs.exception.NoSuchFileException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;

import static com.company.vfs.Utils.FILESYSTEM_FILENAME;
import static com.company.vfs.Utils.removeFilesystemFile;
import static org.junit.Assert.*;

public class DeleteTest {

    private static final String[] DIRECTORIES = { "foo", "bar", "baz", "竜座" };

    private static final String[] INNER_DIRECTORIES = { "lorem ipsum dolor sit amet",
            "Новая Папка 547", "hyvää huomenta", "คนอ้วน" };

    private static final String DEMO_TEXT =
            "char*s=\"char*s=%c%s%c;main(){printf(s,34,s,34);}\";main(){printf(s,34,s,34);}";

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
    public void delete() throws Exception {
        FileSystem fs = VirtualFileSystem.open(FILESYSTEM_FILENAME);

        assertTrue(fs.exists("base.db"));
        fs.delete("base.db");
        assertFalse(fs.exists("base.db"));

        assertTrue(fs.exists("/bar/data"));
        fs.delete("/bar/data");
        assertFalse(fs.exists("/bar/data"));
        assertTrue(fs.exists("/bar"));

        for(String d: fs.getDirectories("bar")) {
            fs.delete("/bar/" + d);
        }

        fs.delete("/bar");
        assertFalse(fs.exists("/bar"));
    }

    @Test(expected = DirectoryNotEmptyException.class)
    public void deleteShouldThrow_When_DirectoryNotEmpty() throws Exception {
        FileSystem fs = VirtualFileSystem.open(FILESYSTEM_FILENAME);
        fs.delete("foo");
    }

    @Test(expected = AccessDeniedException.class)
    public void deleteShouldThrow_When_CalledOnRoot() throws Exception {
        FileSystem fs = VirtualFileSystem.open(FILESYSTEM_FILENAME);
        fs.delete("/");
    }

    @Test(expected = AccessDeniedException.class)
    public void deleteShouldThrow_When_CalledOnOpenedFile() throws Exception {
        FileSystem fs = VirtualFileSystem.open(FILESYSTEM_FILENAME);
        InputStream inputStream = fs.readFile("base.db");
        fs.delete("base.db");
    }

    @Test(expected = NoSuchFileException.class)
    public void deleteShouldThrow_When_NoSuchFile() throws Exception {
        FileSystem fs = VirtualFileSystem.open(FILESYSTEM_FILENAME);
        fs.delete("doesn't exist");
    }

    @After
    public void tearDown() throws Exception {
        removeFilesystemFile();
    }

}