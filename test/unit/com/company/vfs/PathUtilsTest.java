package com.company.vfs;

import org.junit.Test;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class PathUtilsTest {
    @Test
    public void getName() throws Exception {
        assertThat(PathUtils.getName("/"), is(""));
        assertThat(PathUtils.getName(""), is(""));
        assertThat(PathUtils.getName("abc"), is("abc"));
        assertThat(PathUtils.getName("/abc"), is("abc"));
        assertThat(PathUtils.getName("/foo"), is("foo"));
        assertThat(PathUtils.getName("/foo/bar"), is("bar"));
        assertThat(PathUtils.getName("/foo"), is("foo"));
        assertThat(PathUtils.getName("/foo/bar/log.txt"), is("log.txt"));
    }

    @Test
    public void getPathTo() throws Exception {
        assertThat(PathUtils.getPathTo("/"), is(""));
        assertThat(PathUtils.getPathTo(""), is(""));
        assertThat(PathUtils.getPathTo("abc"), is(""));
        assertThat(PathUtils.getPathTo("/abc"), is(""));
        assertThat(PathUtils.getPathTo("/foo"), is(""));
        assertThat(PathUtils.getPathTo("/foo/bar"), is("/foo"));
        assertThat(PathUtils.getPathTo("/foo"), is(""));
        assertThat(PathUtils.getPathTo("/foo/bar/log.txt"), is("/foo/bar"));
    }

    @Test
    public void isRoot() throws Exception {
        assertTrue(PathUtils.isRoot(""));
        assertTrue(PathUtils.isRoot("/"));

        assertFalse(PathUtils.isRoot(" /"));
        assertFalse(PathUtils.isRoot(" / "));
        assertFalse(PathUtils.isRoot("/ "));
        assertFalse(PathUtils.isRoot("/anything"));
    }

    @Test
    public void getPathComponents() throws Exception {
        assertTrue(PathUtils.getPathComponents("").isEmpty());
        assertTrue(PathUtils.getPathComponents("/").isEmpty());
        assertThat(PathUtils.getPathComponents("/foo"), is(Collections.singletonList("foo")));
        assertThat(PathUtils.getPathComponents("bar"), is(Collections.singletonList("bar")));
        assertThat(PathUtils.getPathComponents("/foo/bar"), is(Arrays.asList("foo", "bar")));
        assertThat(PathUtils.getPathComponents("foo/bar"), is(Arrays.asList("foo", "bar")));
        assertThat(PathUtils.getPathComponents("/foo/bar/log.txt"), is(Arrays.asList("foo", "bar", "log.txt")));
        assertThat(PathUtils.getPathComponents("foo/bar/log.txt"), is(Arrays.asList("foo", "bar", "log.txt")));
    }

}