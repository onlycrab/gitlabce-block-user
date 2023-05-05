package com.github.onlycrab.common;

import com.github.onlycrab.common.SimpleIniOper;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * {@link SimpleIniOper} test class.
 */
@SuppressWarnings("SuspiciousMethodCalls")
public class SimpleIniOperTest {
    private static final String TEST_FILE_NAME = "SimpleIniOperTest.tmp";

    /**
     * Test for {@link SimpleIniOper#getTmpDir()}.
     */
    @Test
    public void getTmpDir(){
        String key = "java.io.tmpdir";
        String property = null;
        try {
            property = System.getProperty(key);

            System.setProperty(key, "someTmpPath");
            Assert.assertEquals("someTmpPath" + File.separator, SimpleIniOper.getTmpDir());
        } catch (SecurityException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            if (property != null) {
                System.setProperty(key, property);
            }
        }
    }

    /**
     * Test for {@link SimpleIniOper#read(String, boolean, String)} and {@link SimpleIniOper#read(String[], boolean, String)}.
     */
    @Test
    public void read(){
        //READ FROM FILE
        deleteTestFile();

        Map<String, String> expected = new HashMap<>(), actual = null;
        expected.put("k1", "v1");
        expected.put("k2", "v2");
        expected.put("k3", "v3");
        try {
            //write test file
            SimpleIniOper.write(SimpleIniOper.getTmpDir() + TEST_FILE_NAME, expected, "=");
        } catch (IOException e) {
            Assert.fail("Unexpected error occurred : " + e.getMessage());
        }
        try {
            //read test file
            actual = SimpleIniOper.read(SimpleIniOper.getTmpDir() + TEST_FILE_NAME, true, "=");
        } catch (IOException e) {
            Assert.fail("Unexpected error occurred : " + e.getMessage());
        }

        if (actual != null) {
            assertEquals(expected.size(), actual.size());
            for (HashMap.Entry entry : expected.entrySet()) {
                assertTrue(actual.containsKey(entry.getKey()));
                assertEquals(entry.getValue(), actual.get(entry.getKey()));
            }
        }

        deleteTestFile();

        //READ FROM ARRAY
        actual = SimpleIniOper.read(new String[]{"k1=v1", "k2=v2", "k3=v3"}, false, "=");
        assertEquals(expected.size(), actual.size());
        for (HashMap.Entry entry : expected.entrySet()) {
            assertTrue(actual.containsKey(entry.getKey()));
            assertEquals(entry.getValue(), actual.get(entry.getKey()));
        }

    }

    /**
     * Delete test file for read and write methods.
     */
    private void deleteTestFile(){
        File file = new File(SimpleIniOper.getTmpDir() + TEST_FILE_NAME);
        if (file.exists()){
            try {
                if (!file.delete()) {
                    Assert.fail(String.format("File <%s> was not been deleted", file.getAbsolutePath()));
                }
            } catch (SecurityException e){
                Assert.fail("Unexpected error occurred : " + e.getMessage());
            }
        }
    }

    /**
     * Test for {@link SimpleIniOper#parseLine(String, boolean, String)}.
     */
    @Test
    public void parseLine(){
        checkPair(new String[]{"k1", "v1"}, "k1=v1", true, null);
        checkPair(null, "k1=", true, null);
        checkPair(null, "k1= ", true, null);
        checkPair(new String[]{"k1", ""}, "k1=", false, null);
        checkPair(new String[]{"k1", " "}, "k1= ", false, null);

        checkPair(null, "=v1", true, null);
        checkPair(null, "=v1", false, null);
        checkPair(null, "v1", true, null);
        checkPair(null, "v1", false, null);
        checkPair(null, "=", true, null);
        checkPair(null, "=", false, null);
        checkPair(null, "", true, null);
        checkPair(null, "", false, null);
        checkPair(null, null, true, null);
        checkPair(null, null, false, null);

        checkPair(new String[]{"k1", "v1"}, "k1|v1", true, "\\|");
    }

    /**
     * Method for check parse string line with key-value pair by {@link SimpleIniOper#parseLine(String, boolean, String)}.
     *
     * @param expected expected key-value pair
     * @param actual actual key-value pair
     * @param skipEmpty skip empty values
     * @param delimiter delimiter between key and value
     */
    private void checkPair(String[] expected, String actual, boolean skipEmpty, String delimiter){
        String[] pair = SimpleIniOper.parseLine(actual, skipEmpty, delimiter);
        if (expected == null){
            if (pair != null){
                Assert.fail("Expected : <null>, actual : " + printArr(pair));
            }
        } else {
            if (pair == null){
                Assert.fail("Expected "  + printArr(expected) + ", actual : <null>");
                return;
            }
            Assert.assertEquals(printArr(expected), printArr(pair));
        }
    }

    /**
     * Returns array as string.
     *
     * @param arr array for printing
     * @return array as string
     */
    private String printArr(String[] arr){
        if (arr == null){
            return "<null>";
        }
        StringBuilder sb = new StringBuilder("<");
        sb.append("[0]=").append(arr[0]);
        if (arr.length > 1){
            for (int i = 1; i < arr.length; i++){
                sb.append(" , ").append("[").append(i).append("]=").append(arr[i]);
            }
        }
        return sb.append(">").toString();
    }

    /**
     * Test for {@link SimpleIniOper#write(String, Map, String)}.
     */
    @Test
    public void write(){
        String absPath = SimpleIniOper.getTmpDir() + TEST_FILE_NAME;
        Map<String, String> expected = new HashMap<>(), actual;
        expected.put("k1", "v1");
        expected.put("k2", "v2");
        expected.put("k3", "v3");
        try {
            SimpleIniOper.write(absPath, expected, "=");
            actual = SimpleIniOper.read(absPath, true, "=");
            assertEquals(expected.size(), actual.size());
            for (HashMap.Entry entry : expected.entrySet()) {
                assertTrue(actual.containsKey(entry.getKey()));
                assertEquals(entry.getValue(), actual.get(entry.getKey()));
            }

            deleteTestFile();

            SimpleIniOper.write(absPath, expected, ".");
            actual = SimpleIniOper.read(absPath, true, "\\.");
            assertEquals(expected.size(), actual.size());
            for (HashMap.Entry entry : expected.entrySet()) {
                assertTrue(actual.containsKey(entry.getKey()));
                assertEquals(entry.getValue(), actual.get(entry.getKey()));
            }
        } catch (IOException e) {
            Assert.fail("Unexpected error occurred : " + e.getMessage());
        }

        deleteTestFile();
    }
}
