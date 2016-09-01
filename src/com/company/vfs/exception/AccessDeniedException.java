package com.company.vfs.exception;

public class AccessDeniedException extends VirtualFileSystemException {
    public AccessDeniedException(String s) {
        super(s);
    }
}
