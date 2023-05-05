package com.github.onlycrab.gbu.model;

/**
 * Possible operations needed to synchronize the state of a Gitlab user with its entry in LDAP.
 *
 * @author Roman Rynkovich
 */
@SuppressWarnings("unused")
public enum UserChangeState {
    NONE,
    BLOCK,
    UNBLOCK
}
