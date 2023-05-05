package com.github.onlycrab.gbu.exception;

/**
 * This exception is thrown when an error occurred during communicating with Gitlab API.
 *
 * @author Roman Rynkovich
 */
public class ApiConnectorException extends Exception {
    public ApiConnectorException(String msg) {
        super(msg);
    }
}
