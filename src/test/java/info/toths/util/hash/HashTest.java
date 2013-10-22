package info.toths.util.hash;

import org.codehaus.plexus.digest.Hex;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA.
 * User: steveftoth
 * Date: 9/29/13
 * Time: 8:40 AM
 * To change this template use File | Settings | File Templates.
 */
public class HashTest {

    @Test
    public void testAbs() {
        Assert.assertEquals(1,HashUtils.fastAbs(1));
        Assert.assertEquals(1,HashUtils.fastAbs(-1));
        Assert.assertEquals(100,HashUtils.fastAbs(100));
        Assert.assertEquals(1203,HashUtils.fastAbs(-1203));
    }

    @Test
    public void testHopscotchRehash() {
        HopscotchHash<String,String> hash = new HopscotchHash<String, String>(1);

        String oldValue = hash.put("test","value");

        Assert.assertNull("oldValue",oldValue);

        oldValue = hash.put("test2","value2");
        Assert.assertNull("oldValue",oldValue);

        String value = hash.get("test");
        Assert.assertEquals("put/get","value",value);

        value = hash.get("test2");
        Assert.assertEquals("put/get 2","value2",value);

    }

    @Test
    public void testHopscotchPut() {
        HopscotchHash<String,String> hash=new HopscotchHash<String, String>(11);

        String oldValue = hash.put("test","value");

        Assert.assertNull("oldValue",oldValue);

        String value = hash.get("test");

        Assert.assertEquals("put/get","value",value);

        value = hash.put("test","value2");

        Assert.assertEquals("return of old value","value",value);

        value = hash.get("test");

        Assert.assertEquals("return of new value","value2",value);
    }

    @Test
    public void testBits() {
        Assert.assertEquals(32, Integer.bitCount(-1));
    }

    @Test
    public void testHopscotchGet() throws Throwable {
        HopscotchHash<String, Integer> hash = new HopscotchHash<String, Integer>();
        try {
            final int numberOfItems = 1 << 10;

            for (int i = 0; i < numberOfItems; ++i) {
                String key = Integer.toString(i);
                hash.put(key, key.hashCode());
                Integer out = hash.get(key);
                //should not be null
                Assert.assertNotNull(key, out);
                Assert.assertEquals(key, Integer.valueOf(key.hashCode()), out);
                Assert.assertTrue(key,hash.containsKey(key));

            }
            for (int j = 0; j < 2; ++j) {
                for (int i = 0; i < numberOfItems; ++i) {
                    String operateOn = Integer.toString(i);
                    //delete
                    Integer out = hash.remove(operateOn);
                    Assert.assertFalse(operateOn + " should have been removed",hash.containsKey(operateOn));
                    if (j == 0) {
                        //should not be null
                        Assert.assertNotNull(operateOn, out);
                        Assert.assertEquals(operateOn, Integer.valueOf(operateOn.hashCode()), out);
                    }
                }
            }
            Assert.assertEquals("not empty",0,hash.size());
            Assert.assertTrue("not empty", hash.isEmpty());
        } catch (Throwable ae) {
            System.out.println("hashState");
            hash.printState(System.out);
            throw ae;
        }
    }


    @Test
    public void testHopscotchPutAndRemove() {
        HopscotchHash<String,String> hash=new HopscotchHash<String, String>(11);

        String oldValue=hash.put("testa","atest");
        Assert.assertEquals("size",1,hash.size());
        Assert.assertNull("oldValue",oldValue);
        oldValue=hash.put("nextTest","atest");
        Assert.assertEquals("size",2,hash.size());
        Assert.assertNull("oldValue",oldValue);
        Assert.assertNull("remove non-existant",hash.remove("xyz"));
        Assert.assertEquals("remove nextTest",hash.remove("testa"),"atest");
        Assert.assertNull("remove nextTest again",hash.remove("testa"));

        oldValue = hash.put("test","value");

        Assert.assertNull("oldValue",oldValue);

        String value = hash.get("test");

        Assert.assertEquals("put/get","value",value);

        value = hash.put("test","value2");

        Assert.assertEquals("return of old value","value",value);

        value = hash.get("test");

        Assert.assertEquals("return of new value","value2",value);


    }

    @Test
    public void testBitsSet() {
        Assert.assertEquals("1", Integer.bitCount(1),1);
        Assert.assertEquals("8",8, Integer.bitCount(0x000000FF));
        Assert.assertEquals("32",32, Integer.bitCount(0xFFFFFFFF));
        Assert.assertEquals("16",16, Integer.bitCount(0x0F0F0F0F));
        Assert.assertEquals("8",8, Integer.bitCount(0x11111111));
        Assert.assertEquals("8",8, Integer.bitCount(0x12111112));
        Assert.assertEquals("5",5, Integer.bitCount(0x03000310));
    }

    @Test
    public void testLinearPut() {
        LinearIndexedHash<String,String> hash=new LinearIndexedHash<String, String>(11);

        String oldValue = hash.put("test","value");

        Assert.assertNull("oldValue",oldValue);

        String value = hash.get("test");

        Assert.assertEquals("put/get","value",value);

        value = hash.put("test","value2");

        Assert.assertEquals("return of old value","value",value);

        value = hash.get("test");

        Assert.assertEquals("return of new value","value2",value);


    }

    @Test
    public void testLinearPut2() {
        LinearIndexedHash<String,String> hash=new LinearIndexedHash<String, String>(11);

        hash.put("testa","atest");
        Assert.assertEquals("size",1,hash.size());
        hash.put("nextTest","atest");
        Assert.assertEquals("size",2,hash.size());
        Assert.assertNull("remove non-existant",hash.remove("xyz"));
        Assert.assertEquals("remove nextTest",hash.remove("testa"),"atest");
        Assert.assertNull("remove nextTest again",hash.remove("testa"));

        String oldValue = hash.put("test","value");

        Assert.assertNull("oldValue",oldValue);

        String value = hash.get("test");

        Assert.assertEquals("put/get","value",value);

        value = hash.put("test","value2");

        Assert.assertEquals("return of old value","value",value);

        value = hash.get("test");

        Assert.assertEquals("return of new value","value2",value);


    }




    public static final String toMD5(String key) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] d = digest.digest(key.getBytes());
        return Hex.encode(d);
    }

    @Test
    public void testHopscotchCreateRandomData() throws Throwable {
        HopscotchHash<String, Integer> hash = new HopscotchHash<String, Integer>();
        try {
            final int numberOfItems = 1 << 10;
            int uniqueItems=0;
            Random r = new Random(0);
            for (int i = 0; i < numberOfItems; ++i) {
                String key = toMD5(Integer.toString(r.nextInt()));

                if(hash.put(key, key.hashCode())==null) {
                    uniqueItems++;
                }
                Integer out = hash.get(key);
                //should not be null
                Assert.assertNotNull(key, out);
                Assert.assertEquals(key, Integer.valueOf(key.hashCode()), out);
                Assert.assertTrue(key,hash.containsKey(key));

            }

            Assert.assertEquals("not empty",uniqueItems,hash.size());
            Assert.assertFalse("not empty",hash.isEmpty());
            hash.printState(System.out);

        } catch (Throwable ae) {
            System.out.println("hashState");
            hash.printState(System.out);
            throw ae;
        }
    }


}
