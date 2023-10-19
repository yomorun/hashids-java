package org.hashids;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final String DEFAULT_ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
    private static final String DEFAULT_SEPS = "cfhistuCFHISTU";
    private static final String DEFAULT_SALT = "";

    private static final int DEFAULT_MIN_HASH_LENGTH = 0;
    private static final int MIN_ALPHABET_LENGTH = 16;
    private static final double SEP_DIV = 3.5;
    private static final int GUARD_DIV = 12;

    private final String salt;
    private final int minHashLength;
    private final String alphabet;
    private final String seps;
    private final String guards;

    public Hashids() {
        this(DEFAULT_SALT);
    }

    public Hashids(String salt) {
        this(salt, 0);
    }

    public Hashids(String salt, int minHashLength) {
        this(salt, minHashLength, DEFAULT_ALPHABET);
    }

    public Hashids(String salt, int minHashLength, String alphabet) {
        this.salt = salt != null ? salt : DEFAULT_SALT;
        this.minHashLength = minHashLength > 0 ? minHashLength : DEFAULT_MIN_HASH_LENGTH;

        final StringBuilder uniqueAlphabet = new StringBuilder();
        for (int i = 0; i < alphabet.length(); i++) {
            if (uniqueAlphabet.indexOf(String.valueOf(alphabet.charAt(i))) == -1) {
                uniqueAlphabet.append(alphabet.charAt(i));
            }
        }

        alphabet = uniqueAlphabet.toString();

        if (alphabet.length() < MIN_ALPHABET_LENGTH) {
            throw new IllegalArgumentException(
                    "alphabet must contain at least " + MIN_ALPHABET_LENGTH + " unique characters");
        }

        if (alphabet.contains(" ")) {
            throw new IllegalArgumentException("alphabet cannot contains spaces");
        }

        // seps should contain only characters present in alphabet;
        // alphabet should not contains seps
        String seps = DEFAULT_SEPS;
        for (int i = 0; i < seps.length(); i++) {
            final int j = alphabet.indexOf(seps.charAt(i));
            if (j == -1) {
                seps = seps.substring(0, i) + " " + seps.substring(i + 1);
            } else {
                alphabet = alphabet.substring(0, j) + " " + alphabet.substring(j + 1);
            }
        }

        alphabet = alphabet.replaceAll("\\s+", "");
        seps = seps.replaceAll("\\s+", "");
        seps = Hashids.consistentShuffle(seps, this.salt);

        if ((seps.isEmpty()) || (((float) alphabet.length() / seps.length()) > SEP_DIV)) {
            int seps_len = (int) Math.ceil(alphabet.length() / SEP_DIV);

            if (seps_len == 1) {
                seps_len++;
            }

            if (seps_len > seps.length()) {
                final int diff = seps_len - seps.length();
                seps += alphabet.substring(0, diff);
                alphabet = alphabet.substring(diff);
            } else {
                seps = seps.substring(0, seps_len);
            }
        }

        alphabet = Hashids.consistentShuffle(alphabet, this.salt);
        // use double to round up
        final int guardCount = (int) Math.ceil((double) alphabet.length() / GUARD_DIV);

        String guards;
        if (alphabet.length() < 3) {
            guards = seps.substring(0, guardCount);
            seps = seps.substring(guardCount);
        } else {
            guards = alphabet.substring(0, guardCount);
            alphabet = alphabet.substring(guardCount);
        }
        this.guards = guards;
        this.alphabet = alphabet;
        this.seps = seps;
    }

    public static int checkedCast(long value) {
        final int result = (int) value;
        if (result != value) {
            // don't use checkArgument here, to avoid boxing
            throw new IllegalArgumentException("Out of range: " + value);
        }
        return result;
    }

    private static String toSplitString(String str) {
        return String.join("|", str.split(""));
    }

    private static String consistentShuffle(String alphabet, String salt) {
        if (salt.isEmpty()) {
            return alphabet;
        }
        int saltLen = salt.length();

        int asc_val, j;
        final char[] tmpArr = alphabet.toCharArray();
        for (int i = tmpArr.length - 1, v = 0, p = 0; i > 0; i--, v++) {
            v %= saltLen;
            asc_val = salt.charAt(v);
            p += asc_val;
            j = (asc_val + v + p) % i;
            final char tmp = tmpArr[j];
            tmpArr[j] = tmpArr[i];
            tmpArr[i] = tmp;
        }

        return new String(tmpArr);
    }

    private static String hash(long input, String alphabet) {
        StringBuilder hash = new StringBuilder();
        final int alphabetLen = alphabet.length();

        do {
            final int index = (int) (input % alphabetLen);
            if (index >= 0 && index < alphabet.length()) {
//                hash = alphabet.charAt(index) + hash;
                hash.insert(0, alphabet.charAt(index));
            }
            input /= alphabetLen;
        } while (input > 0);

        return hash.toString();
    }

    private static Long unhash(String input, String alphabet) {
        long number = 0, pos;
        final int alphabetLen = alphabet.length();

        for (int i = 0; i < input.length(); i++) {
            pos = alphabet.indexOf(input.charAt(i));
            number = number * alphabetLen + pos;
        }

        return number;
    }

    /* Private methods */

    /**
     * Encode numbers to string
     *
     * @param numbers the numbers to encode
     * @return the encoded string
     */
    public String encode(long... numbers) {
        if (numbers.length == 0) {
            return "";
        }

        for (final long number : numbers) {
            if (number < 0) {
                return "";
            }
            if (number > MAX_NUMBER) {
                throw new IllegalArgumentException("number can not be greater than " + MAX_NUMBER + "L");
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

        String validChars = this.alphabet + this.guards + this.seps;
        for (int i = 0; i < hash.length(); i++) {
            if (validChars.indexOf(hash.charAt(i)) == -1) {
                return new long[0];
            }
        }

        return this._decode(hash, this.alphabet);
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

        final List<Long> matched = new ArrayList<>();
        final Matcher matcher = Pattern.compile("[\\w\\W]{1,12}").matcher(hexa);

        while (matcher.find()) {
            matched.add(Long.parseLong("1" + matcher.group(), 16));
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

    private String _encode(long... numbers) {
        long numberHashInt = 0;
        for (int i = 0; i < numbers.length; i++) {
            numberHashInt += (numbers[i] % (i + 100));
        }
        String alphabet = this.alphabet;
        final int alphabetLen = alphabet.length();
        final char ret = alphabet.charAt((int) (numberHashInt % alphabetLen));

        final StringBuilder ret_str = new StringBuilder(this.minHashLength).append(ret);

        for (int i = 0; i < numbers.length; i++) {
//            buffer = ret + this.salt + alphabet;
            alphabet = Hashids.consistentShuffle(alphabet,
                    new StringBuilder().append(ret).append(this.salt).append(alphabet).substring(0, alphabetLen));
            final String last = Hashids.hash(numbers[i], alphabet);

            ret_str.append(last);

            if (i + 1 < numbers.length) {
                long sepsIndex = 0;
                if (!last.isEmpty()) {
                    sepsIndex = numbers[i] % (last.charAt(0) + i) % this.seps.length();
                }
                ret_str.append(this.seps.charAt((int) sepsIndex));
            }
        }

        if (ret_str.length() < this.minHashLength) {
            long guardIndex = (numberHashInt + (ret_str.charAt(0))) % this.guards.length();
            char guard = this.guards.charAt((int) guardIndex);
//            ret_str = guard + ret_str;
            ret_str.insert(0, guard);

            if (ret_str.length() < this.minHashLength) {
                guardIndex = (numberHashInt + (ret_str.charAt(2))) % this.guards.length();
                guard = this.guards.charAt((int) guardIndex);
//                ret_str += guard;
                ret_str.append(guard);
            }
        }

        final int halfLen = alphabet.length() / 2;
        while (ret_str.length() < this.minHashLength) {
            alphabet = Hashids.consistentShuffle(alphabet, alphabet);
//            ret_str = alphabet.substring(halfLen) + ret_str + alphabet.substring(0, halfLen);
            ret_str.insert(0, alphabet.substring(halfLen)).append(alphabet, 0, halfLen);
            final int excess = ret_str.length() - this.minHashLength;
            if (excess > 0) {
                final int start_pos = excess / 2;
//                ret_str = ret_str.substring(start_pos, start_pos + this.minHashLength);
                ret_str.delete(start_pos + this.minHashLength, ret_str.length()).delete(0, start_pos);
            }
        }

        return ret_str.toString();
    }

    private long[] _decode(String hash, String alphabet) {
        final ArrayList<Long> ret = new ArrayList<>();
        final int alphabetLen = alphabet.length();

        int i = 0;
        String[] hashArray = hash.split(Hashids.toSplitString(this.guards));

        if (hashArray.length == 3 || hashArray.length == 2) {
            i = 1;
        }

        if (hashArray.length > 0) {
            if (!hashArray[i].isEmpty()) {
                final char lottery = hashArray[i].charAt(0);
                hashArray = hashArray[i]
                        .substring(1)
                        .split(Hashids.toSplitString(this.seps));
                for (final String subHash : hashArray) {
                    alphabet = Hashids.consistentShuffle(alphabet,
                            new StringBuilder().append(lottery).append(this.salt).append(alphabet).substring(0, alphabetLen));
                    ret.add(Hashids.unhash(subHash, alphabet));
                }
            }
        }

        // transform from List<Long> to long[]
        long[] arr = new long[ret.size()];
        for (int k = 0; k < arr.length; k++) {
            arr[k] = ret.get(k);
        }

        if (!this.encode(arr).equals(hash)) {
            arr = new long[0];
        }

        return arr;
    }

    /**
     * Get Hashid algorithm version.
     *
     * @return Hashids algorithm version implemented.
     */
    public String getVersion() {
        return "1.0.0";
    }
}
