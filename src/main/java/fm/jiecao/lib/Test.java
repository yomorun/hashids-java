package fm.jiecao.lib;

/**
 * Created by CC on 4/18/14.
 */
public class Test {
  public static void main(String[] args){
    System.out.println("Start test ...");
    Hashids h = null;
    try {
      h = new Hashids("this is my salt");
    } catch (Exception e) {
      e.printStackTrace();
    }
//    System.out.println("version=" + h.getVersion());
//    System.out.println("minAlphabetLength=" + h.getMinAlphabetLength());
//    System.out.println("sepDiv=" + h.getSepDiv());
//    System.out.println("guardDiv=" + h.getGuardDiv());
//    System.out.println("alphabet=" + h.getAlphabet());
//    System.out.println("seps=" + h.getSeps());
//    System.out.println("minHashLength=" + h.getMinHashLength());
//    System.out.println("salt=" + h.getSalt());
//    System.out.println("guard=" + h.getGuards());
    System.out.println("encrypt 12345 >> " + h.encrypt(12345));

    long[] arr = h.decrypt("NkK9");
    for(int i = 0; i < arr.length; i++) {
      System.out.println("decrypt NkK9 >> " + arr[i]);
    }

    /*
    String guards = "AdG0", hash = "NAkdKG901";
    String regexp = "[" + guards + "]";
    String bd = hash.replaceAll(regexp, " ");
    System.out.println("bd=" + bd);
    */
  }
}
