package com.github.onlycrab.gbu.connector;

/**
 * Gitlab API request method.
 *
 * @author Roman Rynkovich
 */
public enum RequestMethod {
    GET("GET"),
    POST("POST");

    private final String code;

    RequestMethod(String code) {
        this.code = code;
    }

    public String getCode(){
        return code;
    }
}
