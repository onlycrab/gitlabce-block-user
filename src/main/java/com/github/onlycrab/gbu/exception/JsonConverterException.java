package com.github.onlycrab.gbu.exception;

/**
 * This exception is thrown when an error occurred during converting data to JSON or from JSON.
 *
 * @author Roman Rynkovich
 */
public class JsonConverterException extends Exception {
    public JsonConverterException(String msg){ super(msg); }
}
