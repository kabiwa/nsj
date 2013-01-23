package javm.util.concurrent.locks;

import agentj.nativeimp.api.ProtolibTimerListener;
import agentj.nativeimp.ProtolibTimer;

import agentj.thread.Controller;

import sun.misc.Unsafe;

/**
 * Experimental, not integrated yet ...
 *
 * <p/>
 * Created by scmijt
 * Date: Sep 19, 2008
 * Time: 10:00:34 AM
 */
public class ParkTimeout implements ProtolibTimerListener {
    Controller controller;
    ProtolibTimer timer;
    // Hotspot implementation via intrinsics API
    private static final Unsafe unsafe =  Unsafe.getUnsafe();

    Thread threadparking;
    String threadName;

    public ParkTimeout(Controller controller, Thread threadparking) {
        this.controller = controller;
        this.threadparking = threadparking;
        timer=new ProtolibTimer(controller,0, 0);
        threadName = threadparking.getClass().getName();
    }

    /**
     * Awaits for a specific time or unparks if time elapses.
     */
    public void parkNanos(long nanos) {
        double seconds = (double)nanos/ 1000000000.0;
        try {
            timer.setDelay(seconds);
        } catch (Exception ee) {
            ee.printStackTrace();
            System.exit(1);
        }
        timer.startTimerAndNotifyOnTimeout(this);
        unsafe.park(false, 0L);
    }

    /**
     * @param nanos to wait for
     * @return indicates whether the deadline has elapsed
     */
    public void parkUntil(long nanos) {
        long now = System.currentTimeMillis();
        long waitnanos = nanos-now;
        if (waitnanos<=0) return;
        double seconds = (double)waitnanos/ 1000000000.0;
        try {
            timer.setDelay(seconds);
        } catch (Exception ee) {
            ee.printStackTrace();
            System.exit(1);
        }
        timer.startTimerAndNotifyOnTimeout(this);
        unsafe.park(true, 0L);
    }


    /**
     * When there's a timeout unpark (life).
     */
    public void timeOut() {
        unsafe.unpark(threadparking); // release this lock
        controller.getThreadMonitor().threadContinued(threadName); // to sync up with park release...
    }
}