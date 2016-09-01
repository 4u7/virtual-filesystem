package com.company.vfs;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class PathUtils {
    static String getName(String path) {
        if(isRoot(path)) {
            return "";
        }

        int lastSlashIndex = path.lastIndexOf('/');
        return path.substring(lastSlashIndex + 1);
    }

    static String getPathTo(String path) {
        if(isRoot(path)) {
            return "";
        }

        int lastSlashIndex = path.lastIndexOf('/');
        if(lastSlashIndex < 0) {
            return "";
        }

        return path.substring(0, lastSlashIndex);
    }

    static List<String> getPathComponents(String path) {
        return Arrays.stream(path.split("/"))
                .filter(p -> !p.isEmpty())
                .collect(Collectors.toList());
    }

    static boolean isRoot(String path) {
        return path.isEmpty() || path.equals("/");
    }

    static String normalize(String path) {
        if(!path.startsWith("/")) {
            return "/" + path;
        }

        return path;
    }
}
