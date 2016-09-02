package com.company.vfs;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.company.vfs.Utils.FILESYSTEM_FILENAME;
import static com.company.vfs.Utils.removeFilesystemFile;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class ConcurrencyTest {

    private volatile boolean finishedCreating;

    @Before
    public void setUp() throws Exception {
        removeFilesystemFile();
    }

    @Test
    public void createDirectory() throws Exception {
        FileSystem fs = VirtualFileSystem.create(FILESYSTEM_FILENAME)
                .maxBlocks(4096)
                .maxEntries(4096)
                .build();

        fs.createDirectory("foo");
        int numberOfThreads = 5;
        int numberOfItems = 2000;

        ArrayList<Thread> threads = new ArrayList<>();
        Set<String> testDirectories = new HashSet<>();

        ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
        for(int i = 0; i < numberOfItems; ++i) {
            String directory = "foo/" + Integer.toString(i);
            testDirectories.add(Integer.toString(i));
            queue.add(directory);
        }

        for(int i = 0; i < numberOfThreads; ++i) {
            Thread thread = new Thread(() -> {
                try {
                    while (!queue.isEmpty()) {
                        String name = queue.poll();
                        if(name != null) {
                            fs.createDirectory(name);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    fail();
                }
            });

            threads.add(thread);
            thread.start();
        }

        Thread readerThread = new Thread(() -> {
            try {
                while(!queue.isEmpty()) {
                    fs.getDirectories("/foo");
                }
            } catch (IOException e) {
                e.printStackTrace();
                fail();
            }
        });

        readerThread.start();

        for(Thread t: threads) {
            t.join();
        }

        readerThread.join();

        assertThat(new HashSet<>(fs.getDirectories("/foo")), is(testDirectories));
    }

    @Test
    public void writeMultipleFiles() throws Exception {
        FileSystem fs = VirtualFileSystem.create(FILESYSTEM_FILENAME)
                .maxBlocks(4096)
                .maxEntries(4096)
                .build();

        ArrayList<Thread> threads = new ArrayList<>();
        byte[] data = "data".getBytes();
        int threadsNumber = 10;
        int writeTimes = 50000;

        for(int i = 0; i < threadsNumber; ++i) {
            String filename = "data" + i;
            Thread thread = new Thread(() -> {
                try (OutputStream outputStream = fs.createFile(filename)){
                    for(int j = 0; j < writeTimes; ++j) {
                        outputStream.write(data);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    fail();
                }
            });

            threads.add(thread);
            thread.start();
        }

        for(Thread t: threads) {
            t.join();
        }

        for(int i = 0; i < threadsNumber; ++i) {
            try(InputStream inputStream = fs.readFile("data" + i)) {
                assertThat(inputStream.available(), is(writeTimes * data.length));
                byte buff[] = new byte[data.length];
                while (inputStream.read(buff) > 0) {
                    assertThat(buff, is(data));
                }
            }
        }
    }

    @Test
    public void createAndDeleteDirectory() throws Exception {
        VirtualFileSystem fs = VirtualFileSystem.create(FILESYSTEM_FILENAME)
                .maxBlocks(4096)
                .maxEntries(4096)
                .build();

        int numberOfThreads = 5;
        int numberOfItems = 1000;

        ArrayList<Thread> threads = new ArrayList<>();

        ConcurrentLinkedQueue<String> addQueue = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<String> deleteQueue = new ConcurrentLinkedQueue<>();
        for(int i = 0; i < numberOfItems; ++i) {
            String directory = Integer.toString(i);
            addQueue.add(directory);
        }

        for(int i = 0; i < numberOfThreads; ++i) {
            Thread thread = new Thread(() -> {
                try {
                    while (!addQueue.isEmpty()) {
                        String name = addQueue.poll();
                        if(name != null) {
                            fs.createDirectory(name);
                            deleteQueue.add(name);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    fail();
                }
            });

            threads.add(thread);
            thread.start();
        }

        finishedCreating = false;
        Thread deleterThread = new Thread(() -> {
            try {
                while(!(finishedCreating && deleteQueue.isEmpty())) {
                    String name = deleteQueue.poll();
                    if(name != null) {
                        fs.delete(name);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                fail();
            }
        });

        deleterThread.start();

        for(Thread t: threads) {
            t.join();
        }

        finishedCreating = true;
        deleterThread.join();

        assertThat(fs.getDirectories("/"), is(Collections.emptyList()));
        assertThat(fs.getEntriesCount(), is(1));
        assertThat(fs.getBlocksCount(), is(0));
    }

    @Test
    public void createDirectoryAndWriteFiles() throws Exception {
        FileSystem fs = VirtualFileSystem.create(FILESYSTEM_FILENAME)
                .maxBlocks(4096)
                .maxEntries(4096)
                .build();

        fs.createDirectory("foo");
        int numberOfThreads = 5;
        int numberOfItems = 2000;

        ArrayList<Thread> directoryThreads = new ArrayList<>();
        ArrayList<Thread> fileThreads = new ArrayList<>();
        Set<String> testDirectories = new HashSet<>();

        byte[] data = "data".getBytes();
        int writeTimes = 30000;

        ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
        for(int i = 0; i < numberOfItems; ++i) {
            String directory = "foo/" + Integer.toString(i);
            testDirectories.add(Integer.toString(i));
            queue.add(directory);
        }

        for(int i = 0; i < numberOfThreads; ++i) {
            Thread directoryThread = new Thread(() -> {
                try {
                    while (!queue.isEmpty()) {
                        String name = queue.poll();
                        if(name != null) {
                            fs.createDirectory(name);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    fail();
                }
            });

            String filename = "data" + i;
            Thread fileThread = new Thread(() -> {
                try (OutputStream outputStream = fs.createFile(filename)){
                    for(int j = 0; j < writeTimes; ++j) {
                        outputStream.write(data);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    fail();
                }
            });

            fileThreads.add(fileThread);
            fileThread.start();

            directoryThreads.add(directoryThread);
            directoryThread.start();
        }

        for(Thread t: fileThreads) {
            t.join();
        }

        for(Thread t: directoryThreads) {
            t.join();
        }

        assertThat(new HashSet<>(fs.getDirectories("/foo")), is(testDirectories));

        for(int i = 0; i < numberOfThreads; ++i) {
            try(InputStream inputStream = fs.readFile("data" + i)) {
                assertThat(inputStream.available(), is(writeTimes * data.length));
                byte buff[] = new byte[data.length];
                while (inputStream.read(buff) > 0) {
                    assertThat(buff, is(data));
                }
            }
        }
    }

    @After
    public void tearDown() throws Exception {
        removeFilesystemFile();
    }
}