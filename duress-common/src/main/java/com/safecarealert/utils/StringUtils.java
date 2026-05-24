package com.safecarealert.utils;

public  class StringUtils {


    /***
     * remove accidental double quotes everywhere
     * @param o the string containing multiple double quotes
     * @return the same string enclosed by 1 pair of double quotes
     */
    public static String normalize(Object o) {
        String s = String.valueOf(o).trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

}
