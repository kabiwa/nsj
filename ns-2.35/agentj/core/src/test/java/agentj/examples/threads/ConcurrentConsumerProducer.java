package agentj.examples.threads;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

/**
 * The ... class ...
 * <p/>
 * Created by scmijt
 * Date: Sep 15, 2008
 * Time: 4:05:31 PM
 */
public class ConcurrentConsumerProducer {
    ArrayList<String> ds = new ArrayList<String>();
    private static Lock lock;
    static Condition condition;

    public ConcurrentConsumerProducer() {
        System.out.println("Consumer Producer Lock Being Created");
        System.out.println("Thread when lock is created is =  " + Thread.currentThread());

        lock = new ReentrantLock();
        condition = lock.newCondition();
    }

    public void put(String val) {
        System.out.println("Putting data...");

        lock.lock();
        
        System.out.println("Putting data...");

        ds.add(val);

        System.out.println("Putting data - signalling ...");

        condition.signal();

        System.out.println("Putting data unlocking...");

        lock.unlock();
    }


    public String get() throws InterruptedException {
        String ret = null;

        System.out.println("Getting data...");

        lock.lock();

        System.out.println("Get size is " + ds.size());
        if(ds.size()==0) {
            System.out.println(" Waiting ...");
            condition.await();
        } else {
            System.out.println(" Continuing...");
        }

        System.out.println("Getting data from store...");

        ret = ds.get(0);

        ds.remove(ret);

        lock.unlock();

        System.out.println("Unlocking now ...");

        return ret;

    }

}
