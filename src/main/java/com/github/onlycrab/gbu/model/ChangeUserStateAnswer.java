package com.github.onlycrab.gbu.model;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Gitlab API response for a user change state request.
 *
 * @author Roman Rynkovich
 */
@Getter
@Setter
@NoArgsConstructor
public class ChangeUserStateAnswer {
    @SerializedName("to_state")
    private UserChangeState toState;
    private UserChangeStateResult result;
    private User user;
}
