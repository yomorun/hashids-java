package fm.jiecao.lib;

import java.util.*;

/**
 * Hashids designed for Generating short hashes from numbers (like YouTube and Bitly), obfuscate
 * database IDs, use them as forgotten password hashes, invitation codes, store shard numbers
 * This is implementation of http://hashids.org v0.3.3 version.
 *
 * @author fanweixiao <fanweixiao@gmail.com>
 * @since 0.3.3
 */
public class Hashids {
  private static final String VERSION = "0.3.3";
  private static final String DEFAULT_ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
  private static final String DEFAULT_SEPS = "cfhistuCFHISTU";
  private static final double SEP_DIV = 3.5;
  private static final int GUARD_DIV = 12;
  private static final int MIN_ALPHABET_LENGTH = 16;

  private final String salt;
  private final String alphabet;
  private final String seps;
  private final int minHashLength;
  private final String guards;

  public Hashids() {
    this("");
  }

  public Hashids(final String salt) {
    this(salt, 0);
  }

  public Hashids(final String salt, final int minHashLength) {
    this(salt, minHashLength, DEFAULT_ALPHABET);
  }

  public Hashids(final String salt, final int minHashLength, final String alphabet) {
    this.salt = salt;
    if(minHashLength < 0)
      this.minHashLength = 0;
    else
      this.minHashLength = minHashLength;

    String t_alphabet = "";
    // collect unique characters
    for(int i = 0; i < alphabet.length(); i++){
      if(!t_alphabet.contains("" + alphabet.charAt(i))){
        t_alphabet += "" + alphabet.charAt(i);
      }
    }

    if(t_alphabet.length() < MIN_ALPHABET_LENGTH){
      throw new IllegalArgumentException("alphabet must contain at least " + MIN_ALPHABET_LENGTH + " unique characters");
    }

    if(t_alphabet.contains(" ")){
      throw new IllegalArgumentException("alphabet cannot contain spaces");
    }

    // seps should contain only characters present in alphabet;
    // alphabet should not contains seps
    String t_seps = DEFAULT_SEPS;
    for(int i = 0; i < t_seps.length(); i++){
      final int j = t_alphabet.indexOf(t_seps.charAt(i));
      if(j == -1){
        t_seps = t_seps.substring(0, i) + " " + t_seps.substring(i + 1);
      } else {
        t_alphabet = t_alphabet.substring(0, j) + " " + t_alphabet.substring(j + 1);
      }
    }

    t_alphabet = t_alphabet.replaceAll("\\s+", "");
    t_seps = t_seps.replaceAll("\\s+", "");
    t_seps = consistentShuffle(t_seps, this.salt);

    if((t_seps.equals("")) || ((t_alphabet.length() / t_seps.length()) > SEP_DIV)){
      int seps_len = (int)Math.ceil(t_alphabet.length() / SEP_DIV);

      if(seps_len == 1){
        seps_len++;
      }

      if(seps_len > t_seps.length()){
        final int diff = seps_len - t_seps.length();
        t_seps += t_alphabet.substring(0, diff);
        t_alphabet = t_alphabet.substring(diff);
      } else {
        t_seps = t_seps.substring(0, seps_len);
      }
    }

    t_alphabet = consistentShuffle(t_alphabet, this.salt);
    final int guardCount = (int)Math.ceil(t_alphabet.length() / GUARD_DIV);

    if(t_alphabet.length() < 3){
      this.guards = t_seps.substring(0, guardCount);
      this.seps = t_seps.substring(guardCount);
      this.alphabet = t_alphabet;
    } else {
      this.guards = t_alphabet.substring(0, guardCount);
      this.seps = t_seps;
      this.alphabet = t_alphabet.substring(guardCount);
    }
  }

  /**
   * Encrypt numbers to string
   *
   * @param numbers the numbers to encrypt
   * @return the encrypt string
   */
  public String encrypt(final long... numbers){
    if(numbers.length == 0) {
      return "";
    }

    return this.encode(numbers);
  }

  /**
   * Decrypt string to numbers
   *
   * @param hash the encrypt string
   * @return decryped numbers
   */
  public long[] decrypt(final String hash){
    if(hash.equals("")) {
      return new long[0];
    }

    return this.decode(hash, this.alphabet);
  }

  private String encode(final long... numbers){
    int numberHashInt = 0;
    for(int i = 0; i < numbers.length; i++){
      numberHashInt += (numbers[i] % (i+100));
    }
    String alphabet = this.alphabet;
    final char ret = alphabet.toCharArray()[numberHashInt % alphabet.length()];
    final char lottery = ret;
    long num;
    int sepsIndex, guardIndex;
    String buffer, ret_str = ret + "";
    char guard;

    for(int i = 0; i < numbers.length; i++){
      num = numbers[i];
      buffer = lottery + this.salt + alphabet;

      alphabet = consistentShuffle(alphabet, buffer.substring(0, alphabet.length()));
      final String last = hash((int) num, alphabet);

      ret_str += last;

      if(i + 1 < numbers.length){
        num %= ((int)last.toCharArray()[0] + i);
        sepsIndex = (int)(num % this.seps.length());
        ret_str += this.seps.toCharArray()[sepsIndex];
      }
    }

    if(ret_str.length() < this.minHashLength){
      guardIndex = (numberHashInt + (int)(ret_str.toCharArray()[0])) % this.guards.length();
      guard = this.guards.toCharArray()[guardIndex];

      ret_str = guard + ret_str;

      if(ret_str.length() < this.minHashLength){
        guardIndex = (numberHashInt + (int)(ret_str.toCharArray()[2])) % this.guards.length();
        guard = this.guards.toCharArray()[guardIndex];

        ret_str += guard;
      }
    }

    final int halfLen = alphabet.length() / 2;
    while(ret_str.length() < this.minHashLength){
      alphabet = consistentShuffle(alphabet, alphabet);
      ret_str = alphabet.substring(halfLen) + ret_str + alphabet.substring(0, halfLen);
      final int excess = ret_str.length() - this.minHashLength;
      if(excess > 0){
        final int start_pos = excess / 2;
        ret_str = ret_str.substring(start_pos, start_pos + this.minHashLength);
      }
    }

    return ret_str;
  }

  private long[] decode(final String hash, String alphabet){
    final ArrayList<Long> ret = new ArrayList<Long>();

    int i = 0;
    final String regexp = "[" + this.guards + "]";
    String hashBreakdown = hash.replaceAll(regexp, " ");
    String[] hashArray = hashBreakdown.split(" ");

    if(hashArray.length == 3 || hashArray.length == 2){
      i = 1;
    }

    hashBreakdown = hashArray[i];

    final char lottery = hashBreakdown.toCharArray()[0];
    hashBreakdown = hashBreakdown.substring(1);
    hashBreakdown = hashBreakdown.replaceAll("[" + this.seps + "]", " ");
    hashArray = hashBreakdown.split(" ");

    for (final String subHash : hashArray) {
      final String buffer = lottery + this.salt + alphabet;
      alphabet = consistentShuffle(alphabet, buffer.substring(0, alphabet.length()));
      ret.add(unhash(subHash, alphabet));
    }

    //transform from List<Long> to long[]

    final long[] arr = new long[ret.size()];
    for(int k = 0; k < arr.length; k++){
      arr[k] = ret.get(k);
    }

    return arr;
  }

  /* Private methods */
  private static String consistentShuffle(String alphabet, final String salt){
    if(salt.length() <= 0)
      return alphabet;

    final char[] arr = salt.toCharArray();
    int asc_val, j;
    char tmp;
    for(int i = alphabet.length() - 1, v = 0, p = 0; i > 0; i--, v++){
      v %= salt.length();
      asc_val = (int)arr[v];
      p += asc_val;
      j = (asc_val + v + p) % i;

      tmp = alphabet.charAt(j);
      alphabet = alphabet.substring(0, j) + alphabet.charAt(i) + alphabet.substring(j + 1);
      alphabet = alphabet.substring(0, i) + tmp + alphabet.substring(i + 1);
    }

    return alphabet;
  }

  private static String hash(int input, final String alphabet){
    String hash = "";
    final int alphabetLen = alphabet.length();
    final char[] arr = alphabet.toCharArray();

    do {
      hash = arr[input % alphabetLen] + hash;
      input /= alphabetLen;
    } while(input > 0);

    return hash;
  }

  private static Long unhash(final String input, final String alphabet){
    int number = 0, pos;
    final char[] input_arr = input.toCharArray();

    for(int i = 0; i < input.length(); i++){
      pos = alphabet.indexOf(input_arr[i]);
      number += pos * Math.pow(alphabet.length(), input.length() - i - 1);
    }

    return Long.valueOf(number);
  }

  public static int checkedCast(final long value) {
    final int result = (int) value;
    if (result != value) {
      // don't use checkArgument here, to avoid boxing
      throw new IllegalArgumentException("Out of range: " + value);
    }
    return result;
  }

  public static String getVersion() {
    return VERSION;
  }
}
