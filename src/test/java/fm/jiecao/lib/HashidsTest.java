package fm.jiecao.lib;

import java.util.Arrays;

import org.junit.Test;
import junit.framework.Assert;

import fm.jiecao.lib.Hashids;

public class HashidsTest {
		@Test
		public void test1(){
      Hashids a = null;
      try {
        a = new Hashids("this is my salt");
      } catch (Exception e) {
        e.printStackTrace();
      }
//      System.out.println(a.encrypt(1,99999, 0));
//			System.out.println(Arrays.toString(a.decrypt("6kCeoeRt")));
//      Assert.assertEquals(res1, hashStr);
		}
}
