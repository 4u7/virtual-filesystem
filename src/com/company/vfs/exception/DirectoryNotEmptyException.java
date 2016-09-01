package com.company.vfs.exception;

public class DirectoryNotEmptyException extends VirtualFileSystemException {
    public DirectoryNotEmptyException(String s) {
        super(s);
    }
}
