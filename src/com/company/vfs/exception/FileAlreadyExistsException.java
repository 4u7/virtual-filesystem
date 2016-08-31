package com.company.vfs.exception;

public class FileAlreadyExistsException extends VirtualFileSystemException {
    public FileAlreadyExistsException(String path) {
        super(path);
    }
}
