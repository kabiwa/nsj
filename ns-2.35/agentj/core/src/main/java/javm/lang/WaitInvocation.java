package javm.lang;

import agentj.thread.Controller;
import agentj.nativeimp.ProtolibTimer;
import agentj.nativeimp.api.ProtolibTimerListener;
import proto.logging.api.Logger;
import proto.logging.api.Log;

import java.lang.*;
import java.lang.System;


/**
 * Class that implements the wait functions in Object using NS2 timers
 */
class WaitInvocation implements ProtolibTimerListener {
    static Logger logger = Log.getLogger(StaticObject.class);
    ProtolibTimer timer;

    Controller controller;
    java.lang.Object callingObject;
    boolean waiting=false;

    boolean timerIsSet=false;

    private boolean wasInterrupted =false;
    private boolean hasTimedOut=false;
    private long millisElapsed;
    long millisAtAwait;
    private InterruptedException interruptedException;

    public WaitInvocation(Controller controller) {
        this.controller=controller;
        millisElapsed =-1;
        timer=new ProtolibTimer(controller,0, 0);
        logger.trace("Entering");
        logger.trace("Exiting");
    }

    public void agentJWait(java.lang.Object callingObject) throws InterruptedException {
        logger.trace("Entering");
        this.callingObject = callingObject;
        millisAtAwait = java.lang.System.currentTimeMillis();

        try {
            waiting=true;
            synchronized(this.callingObject) {
                this.callingObject.wait();
            }
        } catch (InterruptedException e) {
            wasInterrupted=true;
            interruptedException=e;
        }
        waiting=false;

        long after = System.currentTimeMillis();
        millisElapsed = after-millisAtAwait;
        logger.trace("Exiting");
    }

    public void kickOffWaitTimer(long millis) throws InterruptedException {
        logger.trace("Entering");

        double seconds = (double)millis/ 1000.0;
        try {
            timer.setDelay(seconds);
        } catch (Exception ee) {
            ee.printStackTrace();
            System.exit(0);            
        }

        timer.startTimerAndNotifyOnTimeout(this);

        timerIsSet=true;
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

    public long getMillisElapsed() {
        return millisElapsed;
    }

    /**
     * Returns whether the await was timed out
     *
     * @return whether await was timed out
     */
    boolean getWasTimedOut() {
        return hasTimedOut;
    }

    void cancelTimerIfSet() {
        if ((timerIsSet) && (!hasTimedOut)) {
            timer.cancel();
            timer.deleteTimer();
        }
    }

    /**
     * Called when timer is finished
     */
    public void timeOut() {
        logger.trace("Entering");
        if (!waiting) return;

        hasTimedOut=true;

        if (this.callingObject==null) return;
        
        synchronized(this.callingObject) {
            StaticObject.notify(this.callingObject); // timeout so release this monitor
        }
        logger.trace("Exiting");
    }
}