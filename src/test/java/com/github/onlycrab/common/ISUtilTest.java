package com.github.onlycrab.common;

import com.github.onlycrab.common.ISUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

/**
 * {@link ISUtil} test class.
 */
public class ISUtilTest {
    /**
     * {@link ISUtil#readBytes(InputStream)}.
     */
    @Test
    public void readBytes_InputStream() {
        streamByteTransform();
    }

    /**
     * {@link ISUtil#getInputStream(byte[])}.
     */
    @Test
    public void getInputStream() {
        streamByteTransform();
    }

    /**
     * Test transform from stream to bytes array and vice versa.
     */
    @Test
    public void streamByteTransform() {
        byte[] expected = "qwe123!@#".getBytes();
        byte[] actual;

        InputStream is;
        try {
            is = ISUtil.getInputStream(expected);
        } catch (IOException e) {
            Assert.fail(String.format("Cant put byte array to InputStream : %s.", e.getMessage()));
            return;
        }

        try {
            actual = ISUtil.readBytes(is);
        } catch (IOException e) {
            Assert.fail(String.format("Cant read byte array from InputStream : %s.", e.getMessage()));
            return;
        }

        Assert.assertArrayEquals(expected, actual);
    }
}