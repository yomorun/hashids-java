package fm.jiecao.lib;

import org.junit.Assert;
import org.junit.Test;

public class HashidsTest {
	@Test
	public void test_one_number(){
    final String expected = "NkK9";
    final long num_to_hash = 12345L;
    final Hashids a = new Hashids("this is my salt");

    final String res = a.encrypt(num_to_hash);
    Assert.assertEquals("encrypt", expected, res);

    final long[] res2 = a.decrypt(expected);
    Assert.assertArrayEquals("decrypt",new long[]{num_to_hash}, res2);
	}

	@Test
	public void test_serveral_numbers(){
    final String expected = "aBMswoO2UB3Sj";
    final long[] num_to_hash = {683L, 94108L, 123L, 5L};
    final Hashids a = new Hashids("this is my salt");

    final String res = a.encrypt(num_to_hash);
    Assert.assertEquals("encrypt", expected, res);

    final long[] res2 = a.decrypt(expected);
    Assert.assertArrayEquals("decrypt", num_to_hash, res2);
	}

  @Test
  public void test_specifying_custom_hash_length(){
    final String expected = "gB0NV05e";
    final long num_to_hash = 1L;
    final Hashids a = new Hashids("this is my salt", 8);

    final String res = a.encrypt(num_to_hash);
    Assert.assertEquals("encrypt", expected, res);

    final long[] res2 = a.decrypt(expected);
    Assert.assertArrayEquals("decrypt",new long[]{num_to_hash}, res2);
  }

  @Test
  public void test_specifying_custom_hash_alphabet(){
    final String expected = "b332db5";
    final long num_to_hash = 1234567L;
    final Hashids a = new Hashids("this is my salt", 0, "0123456789abcdef");

    final String res = a.encrypt(num_to_hash);
    Assert.assertEquals("encrypt", expected, res);

    final long[] res2 = a.decrypt(expected);
    Assert.assertArrayEquals("decrypt",new long[]{num_to_hash}, res2);
  }

    @Test
    public void test_randomness(){
      final String expected = "1Wc8cwcE";
      final long[] num_to_hash = {5L, 5L, 5L, 5L};
      final Hashids a = new Hashids("this is my salt");

      final String res = a.encrypt(num_to_hash);
      Assert.assertEquals("encrypt", expected, res);

      final long[] res2 = a.decrypt(expected);
      Assert.assertArrayEquals("decrypt", num_to_hash, res2);
    }

    @Test
    public void test_randomness_for_incrementing_numbers(){
      final String expected = "kRHnurhptKcjIDTWC3sx";
      final long[] num_to_hash = {1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L};
      final Hashids a = new Hashids("this is my salt");

      final String res = a.encrypt(num_to_hash);
      Assert.assertEquals("encrypt", expected, res);

      final long[] res2 = a.decrypt(expected);
      Assert.assertArrayEquals("decrypt", num_to_hash, res2);
    }

    @Test
    public void test_randomness_for_incrementing(){
      final Hashids a = new Hashids("this is my salt");
      Assert.assertEquals("1", "NV", a.encrypt(1L));
      Assert.assertEquals("2", "6m", a.encrypt(2L));
      Assert.assertEquals("3", "yD", a.encrypt(3L));
      Assert.assertEquals("4", "2l", a.encrypt(4L));
      Assert.assertEquals("5", "rD", a.encrypt(5L));
    }
}
