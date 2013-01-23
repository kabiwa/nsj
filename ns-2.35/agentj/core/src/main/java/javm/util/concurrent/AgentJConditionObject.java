package javm.util.concurrent;

import proto.logging.api.Log;
import proto.logging.api.Logger;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import agentj.thread.Controller;
import agentj.AgentJVirtualMachine;
import agentj.nativeimp.ProtolibTimer;
import util.Logging;

/**
 * The class is called instead of ConditionObject in java.util.concurrent when the
 * bytecode is rewritten ... See AgentjQueuedSynchronizer to understand how
 * this works.
 *
 * <p/>
 * Created by Ian Taylor
 * Date: Sep 15, 2008
 * Time: 2:05:16 PM
 */
public class AgentJConditionObject implements Condition {
    static Logger logger = Log.getLogger(AgentJConditionObject.class);

    int waiting=0;
    
    Condition conditionObject;
    Controller controller;

    /**
     * Returns a new AgentJConditionObject that handles the ConditionObject that
     * syncs with agentj
     *
     * @return a new AgentJConditionObject
     */
    public static Condition newCondition(Lock targetObject) {
        logger.debug("Returning AgentJ Condition object interface");
        AgentJConditionObject conditionobj = new AgentJConditionObject(targetObject);
        return conditionobj;
    }


    public AgentJConditionObject(Lock targetObject) {
        logger.debug("Creating AgentJ Condition object interface");
        this.conditionObject = targetObject.newCondition();
        logger.debug("Condition Object is instamce of " + conditionObject.getClass().getName());
        controller = AgentJVirtualMachine.getCurrentNS2NodeController();
        logger.debug("on node " + controller.getLocalHost());
    }

    /**
     * Causes the current thread to wait until it is signalled or
     * {@link Thread#interrupt interrupted}.
     *
     *
     **/
    public void await() throws InterruptedException {
        Thread awaitThread = Thread.currentThread();
        String threadName = awaitThread.getClass().getName();
        // no need for locks here - no variables needing checking and the real await locks anyway
        try {
            logger.debug("Trying await on Thread =  " + threadName);
            Logging.conditionStatus(controller, Logging.ConditionStatusType.AWAIT, threadName);
            controller.getThreadMonitor().threadPaused(threadName); // to sync up with signal...
            ++waiting;        
            conditionObject.await();
        } catch (InterruptedException e) {
            // Wait for ns-2
            ProtolibTimer timer = new ProtolibTimer(controller, 0, 0);
            timer.startTimerAndBlockUntilTimeout();
            throw e;
        } catch (IllegalMonitorStateException illmon) {
            logger.error("Error Message: " + illmon.getMessage());
            logger.error("Thread now =  " + Thread.currentThread());
            illmon.printStackTrace();
            System.exit(0);
        }

        Logging.conditionStatus(controller, Logging.ConditionStatusType.AWAIT_RELEASED, threadName);

        // Wait for ns-2
        ProtolibTimer timer = new ProtolibTimer(controller, 0, 0); // and then immediately stopping
        timer.startTimerAndBlockUntilTimeout(); // and then starting again once timer has returned
        // carry on now since we have woken up ns-2
    }


    /**
     * Causes the current thread to wait until it is signalled.
     */
    public void awaitUninterruptibly() {
        Thread awaitThread = Thread.currentThread();
        String threadName = awaitThread.getClass().getName();
          // no need for locks here - no variables needing checking and the real await locks anyway
        Logging.conditionStatus(controller, Logging.ConditionStatusType.AWAIT, threadName);
        controller.getThreadMonitor().threadPaused(threadName); // we are stopping ...
        ++waiting;
        conditionObject.awaitUninterruptibly();
        Logging.conditionStatus(controller, Logging.ConditionStatusType.AWAIT_RELEASED, threadName);
        ProtolibTimer timer = new ProtolibTimer(controller, 0, 0);
        timer.startTimerAndBlockUntilTimeout(); // releases control
    }

    /**
     * Causes the current thread to wait until it is signalled or interrupted,
     * or the specified waiting time elapses.
     *
     * @return A value less than or equal to zero if the wait has
     * timed out; otherwise an estimate, that
     * is strictly less than the <tt>nanosTimeout</tt> argument,
     * of the time still remaining when this method returned.
     */
    public long awaitNanos(long nanosTimeout) throws InterruptedException {
        Thread awaitThread = Thread.currentThread();
        String threadName = awaitThread.getClass().getName();

        Logging.conditionStatus(controller, Logging.ConditionStatusType.AWAIT, threadName);
        controller.getThreadMonitor().threadPaused(threadName); // we are stopping ...

        // keep all logic in the created awaitObject, to keep thread safety here and
        // remove the need for more locks
        AwaitTimeout awaitObj = new AwaitTimeout(controller);

        ++waiting;
        awaitObj.awaitNanos(nanosTimeout); // returns when done ...

        Logging.conditionStatus(controller, Logging.ConditionStatusType.AWAIT_RELEASED, threadName);
        ProtolibTimer timer = new ProtolibTimer(controller, 0, 0);
        timer.startTimerAndBlockUntilTimeout(); // and stopping
        // and starting again when ns-2 has caught up...

        if (awaitObj.getWasInterrupted()) throw awaitObj.getInterruptedException();

        return awaitObj.getNanosRetValue();
    }

    /**
     * Causes the current thread to wait until it is signalled or interrupted,
     * or the specified waiting time elapses. This method is behaviorally
     * equivalent to <br>
     *
     * <p>The return value indicates whether the deadline has elapsed.
     */
    public boolean await(long time, TimeUnit unit) throws InterruptedException {
        Thread awaitThread = Thread.currentThread();
        String threadName = awaitThread.getClass().getName();

        Logging.conditionStatus(controller, Logging.ConditionStatusType.AWAIT, threadName);
        controller.getThreadMonitor().threadPaused(threadName); // we are stopping ...

        // keep all logic in the created awaitObject, to keep thread safety here and
        // remove the need for more locks
        AwaitTimeout awaitObj = new AwaitTimeout(controller);

        ++waiting;
        awaitObj.awaitSecs(unit.toSeconds(time)); // returns when done ...

        Logging.conditionStatus(controller, Logging.ConditionStatusType.AWAIT_RELEASED, threadName);
        ProtolibTimer timer = new ProtolibTimer(controller, 0, 0);
        timer.startTimerAndBlockUntilTimeout(); // and stopping
        // and starting again when ns-2 has caught up...

        if (awaitObj.getWasInterrupted()) throw awaitObj.getInterruptedException();

        return awaitObj.getWasTimedOut();
    }

    /**
     * Causes the current thread to wait until it is signalled or interrupted,
     * or the specified deadline elapses.
     *
     * <p>The return value indicates whether the deadline has elapsed,
     */
    public boolean awaitUntil(Date deadline) throws InterruptedException{
        return await(deadline.getTime(), TimeUnit.MILLISECONDS);
    }


    /**
     **/
    public void signal() {
        Thread awaitThread = Thread.currentThread();
        String threadName = awaitThread.getClass().getName();
        Logging.conditionStatus(controller, Logging.ConditionStatusType.SIGNAL, threadName);
        controller.getThreadMonitor().threadContinued(threadName); // to sync up with await...
        --waiting;
        conditionObject.signal();
    }

    /**
     **/
    public void signalAll() {
        Thread awaitThread = Thread.currentThread();
        String threadName = awaitThread.getClass().getName();
        Logging.conditionStatus(controller, Logging.ConditionStatusType.SIGNALALL, threadName);

        for (int i =0; i< waiting; ++i)
            controller.getThreadMonitor().threadContinued(threadName); // to sync up with await...
        waiting = 0;
        conditionObject.signalAll();
    }

}
