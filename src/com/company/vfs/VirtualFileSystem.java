package com.company.vfs;

public class VirtualFileSystem {


    public static VirtualFileSystem open(String path) {
        return null;
    }

    public static Builder create(String path) {
        return new Builder(path);
    }


    public static class Builder {

        private static final int DEFAULT_BLOCK_SIZE = 4096;
        private static final int DEFAULT_MAX_BLOCKS = 32768;
        private static final int DEFAULT_MAX_ENTRIES = 32768;

        private int blockSize = DEFAULT_BLOCK_SIZE;
        private int maxEntries = DEFAULT_MAX_ENTRIES;
        private int maxBlocks = DEFAULT_MAX_BLOCKS;

        public Builder(String path) {

        }

        public VirtualFileSystem build() {
            return null;
        }


        public void maxEntries(int maxEntries) {
            this.maxEntries = maxEntries;
        }

        public void maxBlocks(int maxBlocks) {
            this.maxBlocks = maxBlocks;
        }
    }
}
