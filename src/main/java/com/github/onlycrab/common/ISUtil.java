package com.github.onlycrab.common;

import java.io.*;

@SuppressWarnings("WeakerAccess")
public class ISUtil {
    /**
     * Read data from InputStream to byte array.
     *
     * @param stream InputStream to read data from
     * @return data like byte array
     * @throws IOException if an I/O error occurs
     */
    public static byte[] readBytes(InputStream stream) throws IOException {
        if (stream == null) {
            throw new IOException("InputStream is <null>");
        }
        int size = stream.available();
        byte[] data = new byte[size];
        if (stream.read(data, 0, size) == -1) {
            throw new IOException("There is no data in InputStream");
        }
        try {
            stream.close();
        } catch (IOException ignored) { }
        return data;
    }

    /**
     * Convert byte array to InputStream.
     *
     * @param data data like byte array
     * @return data like InputStream
     * @throws IOException if an I/O error occurs
     */
    public static InputStream getInputStream(byte[] data) throws IOException {
        if (data == null) {
            throw new IOException("Byte array is <null>");
        }
        return new ByteArrayInputStream(data, 0, data.length);
    }
}
