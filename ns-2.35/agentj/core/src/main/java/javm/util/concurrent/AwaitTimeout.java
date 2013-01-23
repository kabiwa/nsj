package javm.util.concurrent;

import agentj.nativeimp.api.ProtolibTimerListener;
import agentj.nativeimp.ProtolibTimer;

import agentj.thread.Controller;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * The ... class ...
 * <p/>
 * Created by scmijt
 * Date: Sep 19, 2008
 * Time: 10:00:34 AM
 */
public class AwaitTimeout implements ProtolibTimerListener {
    Controller controller;
    ProtolibTimer timer;
    AbstractQueuedSynchronizer.ConditionObject conditionObject;

    private boolean wasInterrupted =false;
    private boolean hasTimedOut=false;
    private long nanosAtAwait;
    private InterruptedException interruptedException;

    private long nanosRetValue;

    public AwaitTimeout(Controller controller) {
        this.controller = controller;
        timer=new ProtolibTimer(controller,0, 0);
        AgentJQueuedSynchronizer queue = new AgentJQueuedSynchronizer();
        this.conditionObject = queue.newCondition();
    }

    /**
     * Awaits for a specific time - if it was interrupted or timeout out during this time
     * then the object is set or nanos is returned to the value specified i Conditon.await
     *
     * @param nanos time to wait for
     *
     * @return A value less than or equal to zero if the wait has
     * timed out; otherwise an estimate, that
     * is strictly less than the <tt>nanosTimeout</tt> argument,
     * of the time still remaining when this method returned.
     */
    public void awaitNanos(long nanos) {
        nanosAtAwait =-1;

        double seconds = (double)nanos/ 1000000000.0;

        try {
            timer.setDelay(seconds);
        } catch (Exception ee) {
            ee.printStackTrace();
        }

        timer.startTimerAndNotifyOnTimeout(this);

        long before = System.currentTimeMillis();

        try {
            this.conditionObject.await();
        } catch (InterruptedException e) {
            wasInterrupted=true;
            interruptedException=e;
        }

        if (nanosAtAwait ==-1) {  // no timeout
            long after = System.currentTimeMillis();
            long nanosElapsed = nanos - ((after-before)*1000000);
            if (nanosElapsed > 0) nanosRetValue=nanosElapsed;
            else
                System.out.print("ERROR - nanosElapsed in AwaitNanos in AwaitTimeout.java is negative - check");
        } else
            nanosRetValue=0; // timed out
    }

    /**
     * @param seconds to wait for
     * @return indicates whether the deadline has elapsed
     */
    public void awaitSecs(long seconds) {
        try {
            timer.setDelay(seconds);
        } catch (Exception ee) {
            ee.printStackTrace();
        }
        
        timer.startTimerAndNotifyOnTimeout(this);

        try {
            this.conditionObject.await();
        } catch (InterruptedException e) {
            wasInterrupted=true;
            interruptedException=e;
        }
    }

    /**
     * Detects whether the await was interrupted or not
     *
     * @return whether the await was interrupted
     */
    boolean getWasInterrupted() {
        return wasInterrupted;
    }

    public InterruptedException getInterruptedException() {
        return interruptedException;
    }

    public long getNanosRetValue() {
        return nanosRetValue;
    }

    /**
     * Returns whether the await was timed out
     *
     * @return whether await was timed out
     */
    boolean getWasTimedOut() {
        return hasTimedOut;
    }

    /**
     * When there's a timeout.
     */
    public void timeOut() {
        hasTimedOut=true;
        nanosAtAwait = System.currentTimeMillis()*1000000;
        this.conditionObject.signal(); // release this lock
    }
}
