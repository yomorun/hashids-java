package org.hashids;

import static java.util.Arrays.copyOfRange;

public final class CharUtils {

    private CharUtils() {
        throw new UnsupportedOperationException();
    }

    public static char[] concatenate(char a, char[] arrB, char[] arrC, int maxSize) {
        if (maxSize == 0) {
            return new char[0];
        }

        final char[] result = new char[maxSize];
        int i = 0;

        result[i++] = a;

        if (i == maxSize) {
            return result;
        }

        for (final char c : arrB) {
            result[i++] = c;

            if (i == maxSize) {
                return result;
            }
        }

        for (final char c : arrC) {
            result[i++] = c;

            if (i == maxSize) {
                return result;
            }
        }

        return result;
    }

    public static char[] concatenate(char[] arrA, char[] arrB, char[] arrC) {
        final char[] result = new char[arrA.length + arrB.length + arrC.length];
        int i = 0;

        for (final char c : arrA) {
            result[i++] = c;
        }

        for (final char c : arrB) {
            result[i++] = c;
        }

        for (final char c : arrC) {
            result[i++] = c;
        }

        return result;
    }

    public static char[] concatenate(char[] arrA, char[] arrB, int bFrom, int bTo) {
        final char[] result = new char[arrA.length + bTo - bFrom];
        int i = 0;

        for (final char c : arrA) {
            result[i++] = c;
        }

        for (int j = bFrom; j < bTo; j++) {
            result[i++] = arrB[j];
        }

        return result;
    }

    public static int indexOf(char[] source, char c) {
        int i = 0;

        for (final char s : source) {
            if (s == c) {
                break;
            }
            i++;
        }

        return i;
    }

    public static char[] cleanup(char[] source, char[] allowedChars) {
        if ((source == null) || (allowedChars == null)) {
            return source;
        }

        final char[] result = new char[source.length];
        int i = 0;

        for (final char s : source) {
            for (final char a : allowedChars) {
                if (s == a) {
                    result[i++] = s;
                    break;
                }
            }
        }

        return copyOfRange(result, 0, i);
    }

    public static char[] removeAll(char[] source, char[] charsToRemove) {
        if ((source == null) || (charsToRemove == null)) {
            return source;
        }

        final char[] result = new char[source.length];
        int i = 0;
        boolean found;

        for (final char s : source) {
            found = false;

            for (final char c : charsToRemove) {
                if (s == c) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                result[i++] = s;
            }
        }

        return copyOfRange(result, 0, i);
    }

    public static boolean validate(char[] source, char[] allowedChars) {
        boolean found;

        for (final char s : source) {
            found = false;

            for (final char a : allowedChars) {
                if (s == a) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                return false;
            }
        }

        return true;
    }
}
