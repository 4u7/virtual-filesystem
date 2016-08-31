package com.company.vfs.exception;

public class NoSuchFileException extends VirtualFileSystemException {
    public NoSuchFileException(String pathTo) {
        super(pathTo);
    }
}
