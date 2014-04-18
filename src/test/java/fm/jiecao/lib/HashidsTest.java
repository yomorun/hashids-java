package fm.jiecao.lib;

import java.util.Arrays;

import org.junit.Test;
import junit.framework.Assert;

import fm.jiecao.lib.Hashids;

public class HashidsTest {
	@Test
	public void test_one_number(){
    Hashids a = null;
    String expected = "Xaar", res= "";
    long num_to_hash = 1983L;
    try {
      a = new Hashids("this is my salt");
      res = a.encrypt(num_to_hash);
    	Assert.assertEquals(res, expected);
    } catch (Exception e) {
      e.printStackTrace();
    }
	}

	@Test
	public void test_serveral_numbers(){
    Hashids a = null;
    String expected = "XaarHBB3SYYK", res= "";
    long[] num_to_hash = {1983L, 1984L, 2005L};
    try {
      a = new Hashids("this is my salt");
      res = a.encrypt(num_to_hash);
    	Assert.assertEquals(res, expected);
    } catch (Exception e) {
      e.printStackTrace();
    }
	}
}
