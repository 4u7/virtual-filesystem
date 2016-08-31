package com.company.vfs.exception;

public class NotDirectoryException extends VirtualFileSystemException {
    public NotDirectoryException(String pathTo) {
        super(pathTo);
    }
}
