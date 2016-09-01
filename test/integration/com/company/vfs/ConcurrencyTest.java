package com.company.vfs;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.company.vfs.Utils.FILESYSTEM_FILENAME;
import static com.company.vfs.Utils.removeFilesystemFile;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class ConcurrencyTest {

    private FileSystem fs;

    @Before
    public void setUp() throws Exception {
        fs = VirtualFileSystem.create(FILESYSTEM_FILENAME)
                .maxBlocks(4096)
                .maxEntries(4096)
                .build();
    }

    @Test
    public void multithreadedCreateDirectory() throws Exception {
        ArrayList<Thread> threads = new ArrayList<>();
        Set<String> testDirectories = new HashSet<>();

        ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
        for(int i = 0; i < 2000; ++i) {
            String directory = Integer.toString(i);
            testDirectories.add(directory);
            queue.add(directory);
        }

        for(int i = 0; i < 5; ++i) {
            Thread thread = new Thread(() -> {
                try {
                    while (!queue.isEmpty()) {
                        fs.createDirectory(queue.poll());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            threads.add(thread);
            thread.start();
        }

        Thread readerThread = new Thread(() -> {
            try {
                while(!queue.isEmpty()) {
                    fs.getDirectories("/");
                    Thread.sleep(1);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
        readerThread.run();

        for(Thread t: threads) {
            t.join();
        }
        readerThread.join();

        assertThat(new HashSet<>(fs.getDirectories("/")), is(testDirectories));
    }

    @After
    public void tearDown() throws Exception {
        removeFilesystemFile();
    }
}