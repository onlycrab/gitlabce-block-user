package com.github.onlycrab.gbu.exception;

/**
 * This exception is thrown when an error occurred during communicating with LDAP.
 *
 * @author Roman Rynkovich
 */
public class LdapException extends Exception {
    public LdapException(String msg){ super(msg); }
}
