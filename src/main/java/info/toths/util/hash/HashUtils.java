package info.toths.util.hash;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: steveftoth
 * Date: 10/12/13
 * Time: 9:10 AM
 * To change this template use File | Settings | File Templates.
 */
public class HashUtils {
//    static final int[] primesList;
//    static {
//        int end = 1024*1024;
//        ArrayList<Integer> primes=new ArrayList<Integer>();
//        primes.add(2);
//        outer: for(int i=3;i<end;i+=2) {
//            for(Integer j:primes) {
//                if(i%j==0) {
//                    continue outer;
//                }
//            }
//            primes.add(i);
//            //if(primes.size()%10000==0) {System.out.println("primes:"+ i);}
//        }
        /*int[] sieve=new int[end];
        int endCheck = (int)Math.sqrt(sieve.length);
        for(int toCheck=3; toCheck< endCheck;toCheck+=2) {
            for(int check=toCheck;check<end;toCheck+=check) {
                sieve[check]=1;
            }
        }
        for(int toCheck=3; toCheck<sieve.length;toCheck+=2) {
            if(sieve[toCheck]==0) {
                primes.add(toCheck);
            }
        }*/
//        primesList=new int[primes.size()];
//        for(int i=0;i<primes.size();++i) {
//            primesList[i]=primes.get(i);
//        }
//
//    }

//    public static final boolean isPrime(final int x) {
//        if(x%2==0)
//            return false;
//        for(int i=3;i<Math.sqrt(x);i+=2) {
//            if(x%i==0) return false;
//        }
//
//        return true;
//    }
//
//
//
//
//    public static final int nextPrime(int startingPoint) {
//        int val = Arrays.binarySearch(primesList,startingPoint);
//        if(-val+1==primesList.length) {
//            throw new NotImplementedException();
//        }
//        return primesList[Math.abs(val)+1];
//    }

    public static final int zeroBit(int i, int bitNum) {
        return i & (~(1<<bitNum));
    }

    public static final int orBit(int i,int bitNum) {
        return i | (1<<bitNum);
    }

    public static final int andBit(int i, int bitNum) {
        return i & (1<<bitNum);
    }

    public static String bitsToString(final int i) {
        StringBuilder rValue=new StringBuilder(32);
        for(int j=0;j<32;++j) {
            if( (i& (1<<(31-j)))==0) {
                rValue.append("0");
            } else {
                rValue.append("1");
            }
        }
        return rValue.toString();
    }

    static final int[] lowOnes = new int[33];
    static {
        for(int i=0;i<=32;++i) {
            lowOnes[i]=(-1 << (32-i)) >>> (32-i);
        }
    }

    public static int lowOnesMask(int positions) {
        return lowOnes[positions];

    }

    public static final int fastAbs(final int hc) {
        final int temp=(hc>>31);
        return (hc^temp) + (temp&1);
    }


    public static final int fastAbs2(final int hc) {
        return (hc^(hc>>31)) + ((hc>>31)&1);
    }
}
