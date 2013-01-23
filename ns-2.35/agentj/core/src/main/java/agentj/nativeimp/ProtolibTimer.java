package agentj.nativeimp;

import agentj.thread.Controller;
import agentj.thread.Worker;
import agentj.thread.ReleaseSafeSync;
import agentj.thread.Callback;
import agentj.nativeimp.api.ProtolibTimerListener;
import agentj.AgentJVirtualMachine;
import agentj.AgentJAgent;
import proto.logging.api.Log;
import proto.logging.api.Logger;

import java.util.concurrent.locks.ReentrantLock;


/**
 * This class interfaces with a protolib timer. 
 *
 * <p/>
 * Created by scmijt
 * Date: Dec 23, 2007
 * Time: 2:48:39 PM
 */
public class ProtolibTimer implements Callback, Worker {

    // JNI Pointers to C++ objects created in this context
    public long _nsAgentPtr; // the pointer to our JNI C++ NS Agent that this socket lives on
    public long _timerPtr; // the reference to the native timer - set upon construction of the timer

    static int timerCount=0;

    public int _timerID;

    static Logger logger = Log.getLogger(ProtolibTimer.class);

    double delay;
    int repeat;

    ProtolibTimerListener listener=null;

    ReleaseSafeSync sync;

    public Controller controller;

    

    /**
     * Creates a Protlib Timer. We always set repeat to 0; Need to test out further if
     * used in other modes. i.e. Use Thread.sleep() to implement timers.
     *
     * @param controller
     * @param delay the delay in seconds
     * @param repeat - number of times to repeat after initial trigger i.e. 0 is one
     * trigger, -1 is repeat forever and , I think ...  10 is 10 repeats after trigger. 
     */
    public ProtolibTimer(Controller controller, double delay, int repeat) {
        logger.debug("Entering");
        this.controller = controller;
        try {
            setDelay(delay);
        } catch (Exception ee) {
            ee.printStackTrace();
            System.exit(0);
        }

        setRepeat(repeat);
        _timerID=timerCount;
        ++timerCount;
        logger.trace("AGENTJ STATS: Total Timers created = " + timerCount);
        this._nsAgentPtr = controller.getNsAgentPtr();

        sync = new ReleaseSafeSync(controller, ReleaseSafeSync.SyncType.TIMER);
        logger.debug("Exiting");

    }

    public void setDelay(double delay) throws Exception {
        if (delay<0.0) throw new Exception("Value passed to ProtolibTimer is negative, ns-2 is not a time machine !! - delay set to " + delay + " seconds!");
        this.delay=delay;
    }

    public void setRepeat(int repeat) {
        this.repeat=repeat;
    }

    public double getDelay() {
        return delay;
    }

    public double getRepeat() {
        return repeat;
    }


    /**
     * Called from Thread to set up a listener for the given tiemr, in order to
     * create a callback machanism for synchronisation of the controller and
     * the timer.
     */
    public void startTimerAndBlockUntilTimeout() {
        listener=null;
        logger.trace("Timer Starting on Node " + controller.getLocalHost());

        logger.trace("Timer being created, ID = " + _timerID + ", calling native create now");

        createTimer(delay,repeat);

        logger.trace("Timer Native Pointer is " + _timerPtr);

        logger.trace("Calling startTimer now");

        startTimer();
        logger.trace("Timer started, waiting ...");
        sync.blockAndGet();
        logger.trace("Timer wait released, continuing...");
    }

    /**
     * start the timer
     */
    public void startTimerAndNotifyOnTimeout(ProtolibTimerListener listener) {
        logger.trace("Timer Starting on Node " + controller.getLocalHost());
        this.listener=listener;
        createTimer(delay,repeat);
        logger.trace("Timer Details: delay = " + delay + " repeat = " + repeat);
        startTimer();
        logger.trace("Started Timer on Node " + controller.getLocalHost() + ", leaving method now");
    }

    /**
     * Called by the native function when the timer times out.
     */
    public void timerTriggered() {
        AgentJVirtualMachine.setCurrentNode(controller.getNs2node());
        logger.debug("Native Timer Callback from Ns-2");
        if (listener==null) {
            logger.debug("Release Sync on Timer");
            controller.executeCallback(this); // execute callback
          } else {
            controller.executeCallbackInThread(this);
           }
        logger.debug("Native Timer Callback Returning to Ns-2");
        deleteNativeTimer();
    }

    /**
     * For releasing asynchronous timer via sync - controller executes this via our sync class
     * @return
     */
    public Object executeCallback() {
        logger.trace("Returning to Release sync for Timer wait ...");
        logger.trace("============================================...");
        controller.setLocalReceive(false,null);
        return this; // just return something to kick off release on sync and block for asynchronous release
    }

    /**
     * Run in thread from controller
     * 
     * @param variables
     */
    public void doWork(String ... variables) {
        logger.trace("Timer notifying application ...");
        listener.timeOut();
        logger.trace("Timer application notifyication completed...");
    }
    /**
     * Returns sync for use by controller
     *
     * @return the sync for this object
     */
    public ReleaseSafeSync getReleaseSafeSync() {
        return sync;
    }


    /*protected void finalize () throws Throwable {
        deleteNativeTimer();
    }*/

    public void deleteTimer() {
        AgentJAgent.getLock().lock();

        try {
            deleteNativeTimer();
        } finally {
           AgentJAgent.getLock().unlock();
        }
    }

    public void createTimer(double delay, int repeat) {
        AgentJAgent.getLock().lock();

        try {
            createNativeTimer(delay, repeat);
        } finally {
            AgentJAgent.getLock().unlock();
        }
    }

    public void startTimer() {
        AgentJAgent.getLock().lock();

        try {
            startNativeTimer();
        } finally {
           AgentJAgent.getLock().unlock();
        }
    }


   public void cancel() {
        AgentJAgent.getLock().lock();

        try {
            cancelNativeTimer();
        } finally {
           AgentJAgent.getLock().unlock();
        }
    }

    /**
     * Create the native timer - created when used
     * @param delay
     * @param repeat
     */
    private native void createNativeTimer(double delay, int repeat);

    /**
     * starts the native timer
     */
    private native void startNativeTimer();

    /**
     * Deletes the native timer and its references
     */
    private native void deleteNativeTimer();

    private native void cancelNativeTimer();
}
