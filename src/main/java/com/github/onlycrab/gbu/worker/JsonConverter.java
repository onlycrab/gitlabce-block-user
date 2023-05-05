package com.github.onlycrab.gbu.worker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.github.onlycrab.gbu.model.ChangeUserStateAnswer;
import com.github.onlycrab.gbu.model.ErrorResponse;
import com.github.onlycrab.gbu.model.User;

/**
 * Class to convert JSON data.
 *
 * @author Roman Rynkovich
 */
@SuppressWarnings("WeakerAccess")
public class JsonConverter {
    /**
     * Google JSON converter.
     */
    private final Gson gson;

    /**
     * Initialize converter.
     */
    public JsonConverter(){
        gson = new GsonBuilder().serializeNulls().disableHtmlEscaping().create();
    }

    /**
     * Convert users JSON data to {@link User} array.
     *
     * @param json data in JSON format
     * @return JSON data as {@link User} array
     * @throws RuntimeException exception from {@link Gson}
     */
    public User[] fromJson(String json) throws RuntimeException {
        return gson.fromJson(json, User[].class);
    }

    /**
     * Convert Gitlab API error response to {@link ErrorResponse}.
     *
     * @param json data in JSON format
     * @return JSON data as {@link ErrorResponse}
     * @throws RuntimeException exception from {@link Gson}
     */
    public ErrorResponse getError(String json) throws RuntimeException {
        return gson.fromJson(json, ErrorResponse.class);
    }

    /**
     * Convert result of changing state of Gitlab users to JSON
     *
     * @param answer results of changing state of Gitlab users
     * @return result of changing state of Gitlab users as JSON
     * @throws RuntimeException exception from {@link Gson}
     */
    public String toJson(ChangeUserStateAnswer[] answer) throws RuntimeException {
        return gson.toJson(answer, ChangeUserStateAnswer[].class);
    }
}
