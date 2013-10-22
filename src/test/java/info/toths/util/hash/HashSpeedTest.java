package info.toths.util.hash;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 */
@RunWith(Parameterized.class)
public class HashSpeedTest {


    @Parameterized.Parameters
    public static Collection<Object[]> speedData() {
        Collection<Object[]> tests=new ArrayList<>();
        tests.add(new Object[]{"HopscotchHash Default",new HopscotchHash<>(),keyValueGenerator(1024*1024,0),0});
        tests.add(new Object[]{"ConcurrentHashMap",new ConcurrentHashMap<>(),keyValueGenerator(1024*1024,0),0});
        return tests;
    }

    public static Iterator<Integer> keyValueGenerator(final int numberOfItems,final int seed) {
        final Random r = new Random(seed);
        return new Iterator<Integer>(){
            private int generated;

            @Override
            public boolean hasNext() {
                return numberOfItems>generated;
            }

            @Override
            public Integer next() {
                generated++;
                return r.nextInt();
            }

            @Override
            public void remove() {
            }
        };
    }

    private final Map<Integer,Integer> hash;
    private final String hashName;
    private final Iterator<Integer> keyValues;
    private final int randomSeed;

    public HashSpeedTest(String hashName,Map<Integer,Integer> hash,Iterator<Integer> keyValues, int randomSeed) {
        this.hashName=hashName;
        this.hash=hash;
        this.keyValues=keyValues;
        this.randomSeed=randomSeed;
    }

    @Test
    public void timingTestOfHash() throws Exception {
        System.out.println("hashName:" + hashName);
        final long start=System.currentTimeMillis();
        List<Integer> keys=new ArrayList<>();
        Random r = new Random(randomSeed);

        while(keyValues.hasNext()) {
            Integer key=keyValues.next();
            hash.put(key,key);
            keys.add(key);
        }

        System.out.println("put time:"+ (System.currentTimeMillis()-start));
        double[] ratios =  {.3,.3,.7};//delete,put,get

        long gets=0,puts=0,removes=0;
        int numberOfItems = keys.size();

        for(int i =0 ;i < 10*numberOfItems; ++i ) {
            double randomNumber = r.nextDouble();
            final int indexOfOperate=r.nextInt(keys.size());
            Integer operateOn = keys.get(indexOfOperate);
            randomNumber-=ratios[0];
            if(randomNumber<0) {
                //delete
                hash.remove(operateOn);

                ++removes;
            } else {
                randomNumber -= ratios[1];
                if(randomNumber<0) {
                    hash.put(operateOn,operateOn);

                    ++puts;
                } else {
                    hash.get(operateOn);
                    ++gets;
                }
            }
        }
        final long time = System.currentTimeMillis()-start;

        System.out.println(hashName + " : "+ time + " gets:"+ gets+ " puts:"+ puts+ " removes:"+removes + " size:"+hash.size());
        hash.clear();
        System.gc();
    }


}
