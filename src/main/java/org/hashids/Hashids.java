
package org.hashids;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Character.isSpaceChar;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.copyOfRange;
import static org.hashids.CharUtils.*;

/**
 * Hashids designed for Generating short hashes from numbers (like YouTube and Bitly), obfuscate
 * database IDs, use them as forgotten password hashes, invitation codes, store shard numbers.
 * <p>
 * This is implementation of http://hashids.org v1.0.0 version.
 * <p>
 * This implementation is immutable, thread-safe, no lock is necessary.
 *
 * @author <a href="mailto:fanweixiao@gmail.com">fanweixiao</a>
 * @author <a href="mailto:terciofilho@gmail.com">Tercio Gaudencio Filho</a>
 * @since 0.3.3
 */
public class Hashids {
  /**
   * Max number that can be encoded with Hashids.
   */
  public static final long MAX_NUMBER = 9007199254740992L;

  private static final String SPACE = " ";
  private static final char[] DEFAULT_ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".toCharArray();
  private static final char[] DEFAULT_SEPS = "cfhistuCFHISTU".toCharArray();
  private static final char[] DEFAULT_SALT = new char[0];

  private static final int DEFAULT_MIN_HASH_LENGTH = 0;
  private static final int MIN_ALPHABET_LENGTH = 16;
  private static final double SEP_DIV = 3.5;
  private static final int GUARD_DIV = 12;
  private static final Pattern WORD_PATTERN = Pattern.compile("[\\w\\W]{1,12}");

  private final char[] salt;
  private final int minHashLength;
  private final char[] alphabet;
  private final char[] seps;
  private final char[] guards;
  private final String guardsRegExp;
  private final String sepsRegExp;
  private final char[] validChars;

  public Hashids() {
    this(DEFAULT_SALT, DEFAULT_MIN_HASH_LENGTH, DEFAULT_ALPHABET);
  }

  public Hashids(String salt) {
    this(salt, DEFAULT_MIN_HASH_LENGTH);
  }

  public Hashids(String salt, int minHashLength) {
    this((salt == null) ? null : salt.toCharArray(), minHashLength, DEFAULT_ALPHABET);
  }

  public Hashids(String salt, int minHashLength, String alphabet) {
    this((salt == null) ? null : salt.toCharArray(),
        minHashLength,
        (alphabet == null) ? null : alphabet.toCharArray());
  }

  private Hashids(char[] salt, int minHashLength, char[] alphabet) {
    if (salt == null) {
      throw new IllegalArgumentException("The salt cannot be null,");
    }

    if (minHashLength <= 0) {
      throw new IllegalArgumentException("Minimum hash length must be greater than zero,");
    }

    if (alphabet == null) {
      throw new IllegalArgumentException("The alphabet cannot be null,");
    }

    this.salt = salt;
    this.minHashLength = minHashLength;

    // alphabet
    validateAlphabet(alphabet);

    // seps should contain only characters present in alphabet;
    // alphabet should not contains seps
    char[] seps = cleanup(DEFAULT_SEPS, alphabet);
    alphabet = removeAll(alphabet, seps);

    seps = Hashids.consistentShuffle(seps, salt);

    if ((seps.length == 0) || (((float) alphabet.length / seps.length) > SEP_DIV)) {
      int seps_len = (int) Math.ceil(alphabet.length / SEP_DIV);

      if (seps_len == 1) {
        seps_len++;
      }

      if (seps_len > seps.length) {
        final int diff = seps_len - seps.length;
        seps = concatenate(seps, alphabet, 0, diff);
        alphabet = copyOfRange(alphabet, diff, alphabet.length);
      } else {
        seps = copyOf(seps, seps_len);
      }
    }

    alphabet = Hashids.consistentShuffle(alphabet, salt);
    // use double to round up
    final int guardCount = (int) Math.ceil((double) alphabet.length / GUARD_DIV);

    char[] guards;
    if (alphabet.length < 3) {
      guards = copyOf(seps, guardCount);
      seps = copyOfRange(seps, guardCount, seps.length);
    } else {
      guards = copyOf(alphabet, guardCount);
      alphabet = copyOfRange(alphabet, guardCount, alphabet.length);
    }

    this.guards = guards;
    this.alphabet = alphabet;
    this.seps = seps;
    this.validChars = concatenate(alphabet, guards, seps);
    this.guardsRegExp = '[' + String.valueOf(guards) + ']';
    this.sepsRegExp = '[' + String.valueOf(seps) + ']';
  }

  private void validateAlphabet(char[] alphabet) {
    if (alphabet.length < MIN_ALPHABET_LENGTH) {
      throw new IllegalArgumentException(
          "The alphabet must contain at least " + MIN_ALPHABET_LENGTH + " unique characters.");
    }

    for (int i = 0; i < alphabet.length; i++) {
      if (isSpaceChar(alphabet[i])) {
        throw new IllegalArgumentException("The alphabet cannot contain spaces.");
      }

      for (int j = i + 1; j < alphabet.length; j++) {
        if (alphabet[i] == alphabet[j]) {
          throw new IllegalArgumentException("The alphabet cannot contain duplicates.");
        }
      }
    }
  }

  // ---------

  /**
   * Encode numbers to string
   *
   * @param numbers the numbers to encode
   * @return the encoded string
   */
  public String encode(long... numbers) {
    if (numbers.length == 0) {
      throw new IllegalArgumentException("At least one number must be specified.");
    }

    for (final long number : numbers) {
      if (number < 0) {
        return ""; // we must throw an exception here (like the case when we compare with MAX_NUMBER)
      }

      if (number > MAX_NUMBER) {
        throw new IllegalArgumentException("Number can not be greater than " + MAX_NUMBER + '.');
      }
    }

    return this._encode(numbers);
  }

  /**
   * Decode string to numbers
   *
   * @param hash the encoded string
   * @return decoded numbers
   */
  public long[] decode(String hash) {
    if (hash.isEmpty()) {
      return new long[0];
    }

    if (!validate(hash.toCharArray(), validChars)) {
      return new long[0];
    }

    return _decode(hash, alphabet);
  }

  /**
   * Encode hexa to string
   *
   * @param hexa the hexa to encode
   * @return the encoded string
   */
  public String encodeHex(String hexa) {
    if (!hexa.matches("^[0-9a-fA-F]+$")) {
      return "";
    }

    final List<Long> matched = new ArrayList<Long>();
    final Matcher matcher = WORD_PATTERN.matcher(hexa);

    while (matcher.find()) {
      matched.add(Long.parseLong('1' + matcher.group(), 16));
    }

    // conversion
    final long[] result = new long[matched.size()];
    for (int i = 0; i < matched.size(); i++) {
      result[i] = matched.get(i);
    }

    return this.encode(result);
  }

  /**
   * Decode string to numbers
   *
   * @param hash the encoded string
   * @return decoded numbers
   */
  public String decodeHex(String hash) {
    final StringBuilder result = new StringBuilder();
    final long[] numbers = this.decode(hash);

    for (final long number : numbers) {
      result.append(Long.toHexString(number).substring(1));
    }

    return result.toString();
  }

  public static int checkedCast(long value) {
    final int result = (int) value;
    if (result != value) {
      // don't use checkArgument here, to avoid boxing
      throw new IllegalArgumentException("Out of range: " + value);
    }
    return result;
  }

  /* Private methods */

  private String _encode(long... numbers) {
    long numberHashInt = 0;
    for (int i = 0; i < numbers.length; i++) {
      numberHashInt += (numbers[i] % (i + 100));
    }

    char[] newAlphabet = alphabet;
    final char ret = newAlphabet[(int) (numberHashInt % newAlphabet.length)];

    long num;
    long sepsIndex, guardIndex;
    final StringBuilder buffer = new StringBuilder();
    final StringBuilder ret_strB = new StringBuilder();
    ret_strB.append(ret);
    char guard;

    for (int i = 0; i < numbers.length; i++) {
      num = numbers[i];
      buffer.setLength(0);
      buffer.append(ret)
          .append(salt)
          .append(newAlphabet);

      newAlphabet = Hashids.consistentShuffle(newAlphabet, buffer.substring(0, newAlphabet.length).toCharArray());
      final String last = Hashids.hash(num, newAlphabet);

      ret_strB.append(last);

      if (i + 1 < numbers.length) {
        if (last.length() > 0) {
          num %= last.charAt(0) + i;
          sepsIndex = (int) (num % seps.length);
        } else {
          sepsIndex = 0;
        }

        ret_strB.append(seps[(int) sepsIndex]);
      }
    }

    if (ret_strB.length() < minHashLength) {
      guardIndex = (numberHashInt + (ret_strB.charAt(0))) % guards.length;
      guard = guards[(int) guardIndex];

      ret_strB.insert(0, guard);

      if (ret_strB.length() < minHashLength) {
        guardIndex = (numberHashInt + (ret_strB.charAt(2))) % guards.length;
        guard = guards[(int) guardIndex];

        ret_strB.append(guard);
      }
    }

    final int halfLen = newAlphabet.length / 2;
    while (ret_strB.length() < minHashLength) {
      newAlphabet = Hashids.consistentShuffle(newAlphabet, newAlphabet);
      ret_strB.insert(0, newAlphabet, halfLen, newAlphabet.length - halfLen)
          .append(newAlphabet, 0, halfLen);

      final int excess = ret_strB.length() - minHashLength;
      if (excess > 0) {
        final int start_pos = excess / 2;
        ret_strB.replace(0, ret_strB.length(), ret_strB.substring(start_pos, start_pos + minHashLength));
      }
    }

    return ret_strB.toString();
  }

  private long[] _decode(String hash, char[] alphabet) {
    long[] arr = new long[hash.length()];
    int retIdx = 0;

    int i = 0;
    String hashBreakdown = hash.replaceAll(guardsRegExp, SPACE);
    String[] hashArray = hashBreakdown.split(SPACE);

    if ((hashArray.length == 2) || (hashArray.length == 3)) {
      i = 1;
    }

    if (hashArray.length > 0) {
      hashBreakdown = hashArray[i];
      if (!hashBreakdown.isEmpty()) {
        final char[] lottery = new char[] { hashBreakdown.charAt(0) };

        hashBreakdown = hashBreakdown.substring(1);
        hashBreakdown = hashBreakdown.replaceAll(sepsRegExp, SPACE);
        hashArray = hashBreakdown.split(SPACE);

        String subHash;
        for (final String aHashArray : hashArray) {
          subHash = aHashArray;
          alphabet = Hashids.consistentShuffle(
                  alphabet,
                  copyOf(concatenate(lottery, salt, alphabet), alphabet.length));
          arr[retIdx++] = Hashids.unhash(subHash, alphabet);
        }
      }
    }

    arr = copyOf(arr, retIdx);

    if (!encode(arr).equals(hash)) {
      return new long[0];
    }

    return arr;
  }

  private static char[] consistentShuffle(char[] alphabet, char[] salt) {
    if (salt.length <= 0) {
      return alphabet.clone();
    }

    int asc_val, j;
    final char[] result = alphabet.clone();
    for (int i = result.length - 1, v = 0, p = 0; i > 0; i--, v++) {
      v %= salt.length;
      asc_val = salt[v];
      p += asc_val;
      j = (asc_val + v + p) % i;

      final char tmp = result[j];
      result[j] = result[i];
      result[i] = tmp;
    }

    return result;
  }

  private static String hash(long input, char[] alphabet) {
    final StringBuilder hash = new StringBuilder();
    final int alphabetLen = alphabet.length;

    do {
      final int index = (int) (input % alphabetLen);
      if (index >= 0 && index < alphabet.length) {
        hash.insert(0, alphabet[index]);
      }
      input /= alphabetLen;
    } while (input > 0);

    return hash.toString();
  }

  private static long unhash(String input, char[] alphabet) {
    long number = 0, pos;

    for (int i = 0; i < input.length(); i++) {
      pos = indexOf(alphabet, input.charAt(i));
      number = number * alphabet.length + pos;
    }

    return number;
  }

  /**
   * Get Hashid algorithm version.
   *
   * @return Hashids algorithm version implemented.
   */
  public String getVersion() {
    return "1.0.4-SNAPSHOT";
  }
}
