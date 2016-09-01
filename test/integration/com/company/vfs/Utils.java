package com.company.vfs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Utils {
    public static final String FILESYSTEM_FILENAME = "filesystem";

    public static void removeFilesystemFile() throws IOException {
        Path path = Paths.get(FILESYSTEM_FILENAME);
        if(Files.exists(path)) {
            Files.delete(path);
        }
    }
}
