package javm.lang;

import proto.logging.api.Logger;
import proto.logging.api.Log;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;


public class ObjectSync {
    static Logger logger = Log.getLogger(ObjectSync.class);

    private Lock releaselock = new ReentrantLock();
    private Condition releaseCondition = releaselock.newCondition(); //Get the condition variable.

    // create a static worker lock. Used per node, one node at a time so one lock needed

    private int released=0;

    private Lock waitlock = new ReentrantLock();
    private Condition waitCondition = waitlock.newCondition(); //Get the condition variable.

    // create a static worker lock. Used per node, one node at a time so one lock needed

    private int waitsReleased=0;

    public ObjectSync() {
    }


    /**
     * Blocks until this object is (or has been released)
     *
     */
    public void awaitForWait() {
        logger.debug("Entering ");

        releaselock.lock();

        logger.debug("Awaiting");
        if (released==0)
            releaseCondition.awaitUninterruptibly();
        logger.debug("Unblocked await");

        --released;

        releaselock.unlock();

        logger.debug("Exiting ");
    }



    /**
     * Releases the condition and passes the data to the waiting object.
     *
     */
    public void releaseNotify() {
        logger.debug("Entering ");
        releaselock.lock();

        ++released;
        releaseCondition.signal();
        releaselock.unlock();

        logger.debug("Exiting ");
    }

    /**
     * Blocks until this object is (or has been released)
     *
     */
    public void block()  throws InterruptedException {
        logger.debug("Entering ");

        try {
            waitlock.lock();


            logger.debug("Awaiting");
            if (released==0)
                waitCondition.await();
            logger.debug("Unblocked await");
            --waitsReleased;
            waitlock.unlock();

        } finally {
            logger.debug("Finally");
            --waitsReleased;
            waitlock.unlock();
        }
        logger.debug("Exiting ");
    }



    /**
     * Releases the condition and passes the data to the waiting object.
     *
     */
    public void release() {
        logger.debug("Entering ");
        waitlock.lock();

        ++waitsReleased;
        waitCondition.signal();
        waitlock.unlock();

        logger.debug("Exiting ");
    }
    /**
     * Releases the condition and passes the data to the waiting object.
     *
     */
    public void releaseAll() {
        logger.debug("Entering ");
        waitlock.lock();

        ++waitsReleased;
        waitCondition.signalAll();
        waitlock.unlock();

        logger.debug("Exiting ");
    }

}