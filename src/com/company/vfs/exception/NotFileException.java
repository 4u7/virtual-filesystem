package com.company.vfs.exception;

public class NotFileException extends VirtualFileSystemException {
    public NotFileException(String path) {
        super(path);
    }
}
