package com.github.onlycrab.gbu.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Gitlab API error response.
 *
 * @author Roman Rynkovich
 */
@Getter
@Setter
@NoArgsConstructor
public class ErrorResponse {
    private String error;
    private String errorDescription;
}
