package javm.lang;

import agentj.AgentJVirtualMachine;
import agentj.thread.Controller;
import agentj.nativeimp.ProtolibTimer;
import proto.logging.api.Log;
import proto.logging.api.Logger;

/**
 * Reimplementation of java.lan.Thread so that we wrap any threading
 * around our worker class, so we can register what is happeneing.
 * Most of the java.lang.Thread functionality is not supported here.
 * We need to figure out in future how much of this it makes sense to
 * support.
 *
 * User: scmijt
 * Date: Jan 11, 2006
 * Time: 4:05:13 PM
 */
public class Thread {
    static Logger logger = Log.getLogger(Thread.class);
    static int invocationCount=0;

 
    public static void yield() {
        logger.trace("Entered");

        try {
            javm.lang.Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public static void sleep(long millis) throws InterruptedException {
        logger.trace("Entering");


        Controller cont = AgentJVirtualMachine.getCurrentNS2NodeController();

        ProtolibTimer timer = new ProtolibTimer(cont, (double)millis /1000.0, 0);

        // set up the controller to be the socket listener

        timer.startTimerAndBlockUntilTimeout();
        logger.trace("Exiting");
    }


    public static void sleep(long millis, int nanos) throws InterruptedException {
        if (millis < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException(
                    "nanosecond timeout value out of range");
        }

        if (nanos >= 500000 || (nanos != 0 && millis == 0)) {
            millis++;
        }

        javm.lang.Thread.sleep(millis);
    }


    /**
     * The long awaited interrupt method - hurrah !
     */
    public static void interrupt(java.lang.Thread interruptedObject) {

        logger.debug("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% ----->>   Interrupt :)");
        logger.debug("Thread Interrupt is about to be called on object " + interruptedObject.getClass().getName());

        interruptedObject.interrupt();

        logger.debug("Thread Interrupt was called on object " + interruptedObject.getClass().getName());
    }

}
