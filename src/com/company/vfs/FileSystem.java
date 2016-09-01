package com.company.vfs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface FileSystem {
    List<String> getDirectories(String path) throws IOException;
    List<String> getFiles(String path) throws IOException;
    boolean exists(String path) throws IOException;
    void delete(String path) throws IOException;
    boolean isDirectory(String path) throws IOException;
    boolean isFile(String path) throws IOException;
    void createDirectory(String path) throws IOException;
    OutputStream createFile(String path) throws IOException;
    OutputStream writeFile(String path, boolean append) throws IOException;
    InputStream readFile(String path) throws IOException;
}
