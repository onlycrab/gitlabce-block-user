package com.github.onlycrab.gbu.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;
import java.util.Objects;

/**
 * Gitlab user.
 *
 * @author Roman Rynkovich
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private long id;
    private String username;
    private String state;
    private Identity[] identities;

    public User(String username) {
        this.username = username;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id == user.id &&
                Objects.equals(username, user.username) &&
                Objects.equals(state, user.state) &&
                Arrays.equals(identities, user.identities);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, username, state);
        result = 31 * result + Arrays.hashCode(identities);
        return result;
    }
}
