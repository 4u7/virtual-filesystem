package com.company.vfs;

import com.company.vfs.Metadata.Type;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class TypeTest {
    @Test
    public void valueOf() throws Exception {
        assertThat(Type.valueOf(0), is(Type.Undefined));
        assertThat(Type.valueOf(1), is(Type.Directory));
        assertThat(Type.valueOf(2), is(Type.File));
    }

    @Test(expected = IllegalArgumentException.class)
    public void valueOfShouldThrow() throws Exception {
        Type.valueOf(42);
    }
}