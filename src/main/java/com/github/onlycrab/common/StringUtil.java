package com.github.onlycrab.common;

/**
 * Some string utilities methods.
 *
 * @author Roman Rynkovich
 */
public class StringUtil {
    /**
     * Is string empty or null.
     *
     * @param s target string
     * @return {@code true} if string is empty or null, otherwise return {@code false}.
     */
    public static boolean isEmptyOrNull(String s){
        if (s == null){
            return true;
        } else {
            return s.trim().length() == 0;
        }
    }

    /**
     * Is at least one string empty or null.
     *
     * @param s target string
     * @return {@code true} if at least one string is empty or null, otherwise return {@code false}.
     */
    public static boolean isEmptyOrNullAtLeastOne(String... s){
        for (String str : s){
            if (isEmptyOrNull(str)){
                return true;
            }
        }
        return false;
    }
}
