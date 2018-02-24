package org.hashids;

import static java.lang.System.arraycopy;
import static java.util.Arrays.copyOf;

public final class CharUtils {

  private CharUtils() {
    throw new UnsupportedOperationException();
  }

  public static char[] concatenate(char[] arrA, char[] arrB, char[] arrC) {
    final char[] result = new char[arrA.length + arrB.length + arrC.length];

    arraycopy(arrA, 0, result, 0, arrA.length);
    arraycopy(arrB, 0, result, arrA.length, arrB.length);
    arraycopy(arrC, 0, result, arrA.length + arrB.length, arrC.length);

    return result;
  }

  public static char[] concatenate(char[] arrA, char[] arrB, int bFrom, int bTo) {
    final int bCopyLength = bTo - bFrom;
    final char[] result = new char[arrA.length + bCopyLength];

    arraycopy(arrA, 0, result, 0, arrA.length);
    arraycopy(arrB, bFrom, result, arrA.length, bCopyLength);

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

    return copyOf(result, i);
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

    return copyOf(result, i);
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
