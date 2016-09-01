package com.company.vfs.exception;

import com.company.vfs.VirtualFileSystem;

import java.io.IOException;

public class VirtualFileSystemException extends IOException {
    VirtualFileSystemException() {

    }

    public VirtualFileSystemException(String msg) {
        super(msg);
    }
}
