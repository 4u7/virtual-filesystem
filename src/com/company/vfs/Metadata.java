package com.company.vfs;

import java.io.IOException;

interface Metadata {

    int getDataLength() throws IOException;

    void updateDataLength(int length) throws IOException;

    int getFirstBlock() throws IOException;

    void setFirstBlock(int currentBlock) throws IOException;

    Type getType() throws IOException;

    int getId();

    public enum Type {
        Undefined(0),
        Directory(1),
        File(2);

        public final int value;

        Type(int value) {
            this.value = value;
        }

        public static Type valueOf(int value) {
            for(Type type: Type.values()) {
                if(type.value == value) {
                    return type;
                }
            }

            throw new IllegalArgumentException("Invalid type value.");
        }
    }
}
