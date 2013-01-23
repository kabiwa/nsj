package javm.util.concurrent.locks;

import sun.misc.Unsafe;
import util.Logging;
import agentj.nativeimp.ProtolibTimer;
import javm.util.concurrent.locks.ParkTimeout;
import proto.logging.api.Logger;
import proto.logging.api.Log;
import agentj.thread.Controller;
import agentj.AgentJVirtualMachine;

/**
 * Experimental - not integrated yet ...
 * 
 * <p/>
 * Created by scmijt
 * Date: Sep 20, 2008
 * Time: 9:28:41 AM
 */
public class LockSupport {
    static Logger logger = Log.getLogger(LockSupport.class);

    //   private AgentJLockSupport() {} // Cannot be instantiated.

    // Hotspot implementation via intrinsics API
    private static Unsafe unsafe=null;

    public static void init() { // Used to force loading of this class at
        // bootstrapping time. Otherwise I get a class not found exception from
        // Javassist when rewriting the bytecode...
        logger.debug("Initialised...");
    }

    private static Unsafe getUnsafe() {
        if (unsafe==null)
            unsafe = Unsafe.getUnsafe();
        return unsafe;
    }
    /**
     * Make available the permit for the given thread, if it
     * was not already available.  If the thread was blocked on
     * <tt>park</tt> then it will unblock.  Otherwise, its next call
     * to <tt>park</tt> is guaranteed not to block. This operation
     * is not guaranteed to have any effect at all if the given
     * thread has not been started.
     * @param thread the thread to unpark, or <tt>null</tt>, in which case
     * this operation has no effect.
     */
    public static void unpark(Thread thread) {
        if (thread != null)
            getUnsafe().unpark(thread);
        Controller controller = AgentJVirtualMachine.getCurrentNS2NodeController();
        Thread threadparking=Thread.currentThread();
        String threadName=threadparking.getClass().getName();
        Logging.parkStatus(controller, Logging.ParkStatusType.UNPARK, threadName);
        controller.getThreadMonitor().threadContinued(threadName); // to sync up with await...
    }

    /**
     * Disables the current thread for thread scheduling purposes unless the
     * permit is available.
     * <p>If the permit is available then it is consumed and the call returns
     * immediately; otherwise
     * the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until one of three things happens:
     * <ul>
     * <li>Some other thread invokes <tt>unpark</tt> with the current thread
     * as the target; or
     * <li>Some other thread {@link Thread#interrupt interrupts} the current
     * thread; or
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread upon return.
     */
    public static void park() {
        Controller controller = AgentJVirtualMachine.getCurrentNS2NodeController();

        Thread threadparking=Thread.currentThread();
        String threadparkingName=threadparking.getClass().getName();

        logger.debug("Trying park on Thread =  " + threadparking);
        Logging.parkStatus(controller, Logging.ParkStatusType.PARK, threadparkingName);
        controller.getThreadMonitor().threadPaused(threadparkingName); // we are stopping ...
        getUnsafe().park(false, 0L);

        Logging.parkStatus(controller, Logging.ParkStatusType.PARKRELEASED, threadparkingName);

        // Wait for ns-2
        ProtolibTimer timer = new ProtolibTimer(controller, 0, 0); // and then immediately stopping
        timer.startTimerAndBlockUntilTimeout(); // and then starting again once timer has returned
        // timer does the release needed for the unpark handshake.

        AgentJVirtualMachine.setCurrentNode(controller.getNs2node());
        // carry on now since we have woken up ns-2
    }

    /**
     * Disables the current thread for thread scheduling purposes, for up to
     * the specified waiting time, unless the permit is available.
     * <p>If the permit is available then it is consumed and the call returns
     * immediately; otherwise
     * the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until one of four things happens:
     * <ul>
     * <li>Some other thread invokes <tt>unpark</tt> with the current thread
     * as the target; or
     * <li>Some other thread {@link Thread#interrupt interrupts} the current
     * thread; or
     * <li>The specified waiting time elapses; or
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread, or the elapsed time
     * upon return.
     *
     * @param nanos the maximum number of nanoseconds to wait
     */
    public static void parkNanos(long nanos) {
        if (nanos <= 0) return;

        Controller controller = AgentJVirtualMachine.getCurrentNS2NodeController();
        Thread threadparking=Thread.currentThread();
        String threadparkingName=threadparking.getClass().getName();

        logger.debug("Trying park on Thread =  " + threadparking);
        Logging.parkStatus(controller, Logging.ParkStatusType.PARK, threadparkingName);
        controller.getThreadMonitor().threadPaused(threadparkingName); // we are stopping ...

        // keep all logic in the created awaitObject, to keep thread safety here and
        // remove the need for more locks
        ParkTimeout parkObj = new ParkTimeout(controller, threadparking);

        parkObj.parkNanos(nanos); // returns when done ...

        Logging.parkStatus(controller, Logging.ParkStatusType.PARKRELEASED, threadparkingName);

        // Wait for ns-2
        ProtolibTimer timer = new ProtolibTimer(controller, 0, 0); // and then immediately stopping
        timer.startTimerAndBlockUntilTimeout(); // and then starting again once timer has returned
        // timer does the release needed for the unpark handshake.

        AgentJVirtualMachine.setCurrentNode(controller.getNs2node());
        // carry on now since we have woken up ns-2
    }

    /**
     * Disables the current thread for thread scheduling purposes, until
     * the specified deadline, unless the permit is available.
     * <p>If the permit is available then it is consumed and the call returns
     * immediately; otherwise
     * the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until one of four things happens:
     * <ul>
     * <li>Some other thread invokes <tt>unpark</tt> with the current thread
     * as the target; or
     * <li>Some other thread {@link Thread#interrupt interrupts} the current
     * thread; or
     * <li>The specified deadline passes; or
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread, or the current time
     * upon return.
     *
     * @param deadline the absolute time, in milliseconds from the Epoch, to
     * wait until
     */
    public static void parkUntil(long deadline) {
        Controller controller = AgentJVirtualMachine.getCurrentNS2NodeController();
        Thread threadparking=Thread.currentThread();
        String threadparkingName=threadparking.getClass().getName();

        logger.debug("Trying park on Thread =  " + threadparking);
        Logging.parkStatus(controller, Logging.ParkStatusType.PARK, threadparkingName);
        controller.getThreadMonitor().threadPaused(threadparkingName); // we are stopping ...

        // keep all logic in the created awaitObject, to keep thread safety here and
        // remove the need for more locks
        ParkTimeout parkObj = new ParkTimeout(controller, threadparking);

        parkObj.parkUntil(deadline); // returns when done ...

        Logging.parkStatus(controller, Logging.ParkStatusType.PARKRELEASED, threadparkingName);

        // Wait for ns-2
        ProtolibTimer timer = new ProtolibTimer(controller, 0, 0); // and then immediately stopping
        timer.startTimerAndBlockUntilTimeout(); // and then starting again once timer has returned
        // timer does the release needed for the unpark handshake.

        AgentJVirtualMachine.setCurrentNode(controller.getNs2node());
        // carry on now since we have woken up ns-2
    }
}
