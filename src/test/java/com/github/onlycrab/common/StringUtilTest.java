package com.github.onlycrab.common;

import com.github.onlycrab.common.StringUtil;
import org.junit.Assert;
import org.junit.Test;

/**
 * {@link StringUtil} test class.
 */
public class StringUtilTest {
    /**
     * {@link StringUtil#isEmptyOrNull(String)}.
     */
    @Test
    public void isEmptyOrNull() {
        Assert.assertTrue(StringUtil.isEmptyOrNull(""));
        Assert.assertTrue(StringUtil.isEmptyOrNull(null));
        Assert.assertFalse(StringUtil.isEmptyOrNull("t"));
    }

    /**
     * {@link StringUtil#isEmptyOrNullAtLeastOne(String...)}.
     */
    @Test
    public void isEmptyOrNullAtLeastOne() {
        Assert.assertFalse(StringUtil.isEmptyOrNullAtLeastOne("t", "s", "t"));
        Assert.assertTrue(StringUtil.isEmptyOrNullAtLeastOne("", "s", "t"));
        Assert.assertTrue(StringUtil.isEmptyOrNullAtLeastOne("t", null, "t"));
        Assert.assertTrue(StringUtil.isEmptyOrNullAtLeastOne(null, "s", ""));
    }
}