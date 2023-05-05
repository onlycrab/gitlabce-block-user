package com.github.onlycrab.common;

import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Class for reading and writing data to {@code .ini} file.
 *
 * @author Roman Rynkovich
 */
@SuppressWarnings("WeakerAccess")
public class SimpleIniOper {
    private static final String[] COMMENT = {"#", ";"};
    /**
     * Key-value pair default delimiter.
     */
    public static final String DELIMITER_DEFAULT = "=";

    /**
     * Returns value of system property {@code java.io.tmpdir}, or {@code "null"} if no such property found
     *
     * @return value of system property {@code java.io.tmpdir}, or {@code "null"} if no such property found
     * @throws SecurityException if a security manager exists and its doesn't allow access to the specified
     *                          system property
     */
    public static String getTmpDir() throws SecurityException{
        String tmp;
        try {
            tmp = System.getProperty("java.io.tmpdir");
        } catch (SecurityException e){
            throw new SecurityException(String.format("Access denied for getting system property <java.io.tmpdir> : %s.", e.getMessage()));
        }
        if (tmp == null){
            return "null" + File.separator;
        }
        if (tmp.lastIndexOf(File.separator) == tmp.length() - 1){
            return tmp;
        } else {
            return tmp + File.separator;
        }
    }

    /**
     * Get key-value set from file. If {@code skipEmpty} is {@code true} - pairs with empty value will be skipped.
     *
     * @param absPath absolute path to file
     * @param skipEmpty skip empty values
     * @param delimiter delimiter between key and value
     * @return map of key-value pairs
     * @throws IOException if some I\O error occurred while reading file
     */
    public static Map<String, String> read(String absPath, boolean skipEmpty, String delimiter) throws IOException {
        Map<String, String> dataMap = new HashMap<>();
        File file = new File(absPath);
        if (!file.exists()) {
            return dataMap;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            String[] pair;
            while ((line = reader.readLine()) != null) {
                pair = parseLine(line, skipEmpty, delimiter);
                if (pair != null){
                    dataMap.put(pair[0], pair[1]);
                }
            }
        } catch (IOException e) {
            throw new IOException(String.format("Cant read file <%s> : %s.", file.getAbsolutePath(), e.getMessage()));
        }
        return dataMap;
    }

    /**
     * Get key-value set from array. If {@code skipEmpty} is {@code true} - pairs with empty value will be skipped.
     *
     * @param lines key-value array
     * @param skipEmpty skip empty values
     * @param delimiter delimiter between key and value
     * @return map of key-value pairs
     */
    public static Map<String, String> read(String[] lines, boolean skipEmpty, String delimiter) {
        Map<String, String> dataMap = new HashMap<>();
        if (lines == null){
            return dataMap;
        }
        String[] pair;
        for (String line : lines){
            pair = parseLine(line, skipEmpty, delimiter);
            if (pair != null){
                dataMap.put(pair[0], pair[1]);
            }
        }
        return dataMap;
    }

    /**
     * Returns key-value pair parsed from string.
     *
     * @param line key-value string
     * @param skipEmpty skip empty values
     * @param delimiter delimiter between key and value
     * @return key-value pair parsed from string
     */
    @Nullable
    public static String[] parseLine(String line, boolean skipEmpty, String delimiter){
        if (line == null){
            return null;
        } else if (delimiter == null){
            delimiter = DELIMITER_DEFAULT;
        }
        //skip comments
        if (line.length() > 0) {
            for (String commentSymbol : COMMENT){
                if (line.startsWith(commentSymbol)){
                    return null;
                }
            }
        }

        String[] parsed = line.split(delimiter);
        if (parsed.length == 1) {
            if (!skipEmpty && parsed[0].trim().length() != 0 && line.contains("=")){
                return new String[]{parsed[0], ""};
            }
        } else if (parsed.length == 2){
            if (parsed[0].trim().length() > 0) {
                if (!skipEmpty || parsed[1].trim().length() != 0) {
                    return new String[]{parsed[0], parsed[1]};
                }
            }
        } else if (parsed.length > 2){
            if (parsed[0].trim().length() > 0) {
                StringBuilder value = new StringBuilder();
                value.append(parsed[1]);
                for (int i = 2; i < parsed.length; i++){
                    value.append("=").append(parsed[i]);
                }
                return new String[]{parsed[0], value.toString()};
            }
        }
        return null;
    }

    /**
     * Write data map to file.
     *
     * @param absPath absolute path to file
     * @param dataMap key-value pairs
     * @param delimiter delimiter between key and value
     * @throws IOException if file cant be deleted;
     *                  if file cant be created.
     * @throws SecurityException if a security manager exists and denies access to the file
     */
    public static void write(String absPath, Map<String, String> dataMap, String delimiter) throws IOException, SecurityException{
        File file = new File(absPath);
        try {
            if (file.exists()) {
                if (!file.delete()) {
                    throw new IOException(String.format("File <%s> was not been deleted.", absPath));
                }
            }
            if (!file.createNewFile()) {
                throw new IOException(String.format("File <%s> was not been created.", absPath));
            }
        } catch (SecurityException e){
            throw new SecurityException(String.format("Access denied to file <%s> : %s.", absPath, e.getMessage()));
        }
        if (dataMap == null){
            return;
        } else if (delimiter == null){
            delimiter = DELIMITER_DEFAULT;
        }
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(file))){
            for (Map.Entry<String, String> entry : dataMap.entrySet()){
                if (entry.getKey() != null) {
                    writer.write(entry.getKey());
                    writer.write(delimiter);
                    if (entry.getValue() != null) {
                        writer.write(entry.getValue());
                    }
                    writer.write("\n");
                }
            }
            writer.flush();
        }
    }
}
