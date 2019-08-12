package com.ibm.airlock.common.util;

public class Strings {

    public static StringBuilder escapeCharactersForJSON(String aText) {
        final StringBuilder result = new StringBuilder();
        for (int i = 0; i < aText.length(); i++) {
            char character = aText.charAt(i);
            if (character == '\"') {
                result.append("\\\"");
            } else if (character == '\\') {
                result.append("\\\\");
            } else if (character == '/') {
                result.append("\\/");
            } else if (character == '\b') {
                result.append("\\b");
            } else if (character == '\f') {
                result.append("\\f");
            } else if (character == '\n') {
                result.append("\\n");
            } else if (character == '\r') {
                result.append("\\r");
            } else if (character == '\t') {
                result.append("\\t");
            } else {
                //the char is not a special one
                //add it to the result as is
                result.append(character);
            }
        }
        return result;
    }
}
