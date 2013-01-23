package javm.lang;

import agentj.AgentJVirtualMachine;
import agentj.thread.Controller;
import proto.logging.api.Log;
import proto.logging.api.Logger;
import util.Logging;

import java.util.Vector;
import java.lang.Thread;


/**
 *   Implementation of the wait/notify methods
 *
 */
public class StaticObject extends java.lang.Object {

    static Object lastNotifiedObject=null;

    static Vector<ObjectCounter> objectsWaiting= new Vector<ObjectCounter>();

    static Logger logger = Log.getLogger(StaticObject.class);

    public static final void wait(java.lang.Object callingObject) throws InterruptedException {
        logger.debug("Entering");
        Controller controller = AgentJVirtualMachine.getCurrentNS2NodeController();
        logger.trace("Wait wait() on Node " + controller.getLocalHost() );
        logger.trace("Calling Object = " + callingObject.getClass().getName());

        java.lang.Thread awaitThread = Thread.currentThread();

        String threadName = awaitThread.getClass().getName();

        // keep all logic in the created WaitInvocation to keep thread safety here and
        // remove the need for more locks
        WaitInvocation waitObj = new WaitInvocation(controller);

        ObjectCounter objctr = getObject(callingObject);
        if (objctr!=null) {
            objctr.increment();
        } else {
            logger.trace("Adding a new object to Vector for " + callingObject.getClass().getName());            
            ObjectCounter ob=new ObjectCounter(1,callingObject);
            objectsWaiting.add(ob);
        }

        Logging.monitorStatus(controller, Logging.MonitorStatusType.WAIT, threadName);
        controller.getThreadMonitor().threadPaused(threadName); // we are stopping ...

        waitObj.agentJWait(callingObject); // returns when done ...

        Logging.monitorStatus(controller, Logging.MonitorStatusType.RELEASED, threadName);
        controller.getThreadMonitor().threadContinued(threadName); // we are stopping ...
        synchronized (callingObject) {
        callingObject.notify(); // to release notify method ....
        }
//        ProtolibTimer timer = new ProtolibTimer(controller, 0, 0);
        //       timer.startTimerAndBlockUntilTimeout(); // and stopping
               // and starting again when ns-2 has caught up...

        if (waitObj.getWasInterrupted()) throw waitObj.getInterruptedException();
        logger.debug("Leaving");
    }

    public static final void wait(java.lang.Object callingObject, long millis) throws InterruptedException {
        logger.debug("Entering");
        Controller controller = AgentJVirtualMachine.getCurrentNS2NodeController();
        logger.trace("Wait wait("+millis+") on Node " + controller.getLocalHost() );
        logger.trace("Calling Object = " + callingObject.getClass().getName());

        java.lang.Thread awaitThread = Thread.currentThread();
        String threadName = awaitThread.getClass().getName();

        // keep all logic in the created WaitInvocation to keep thread safety here and
        // remove the need for more locks
        WaitInvocation waitObj = new WaitInvocation(controller);

        ObjectCounter objctr = getObject(callingObject);
        if (objctr!=null) {
            objctr.increment();
        } else {
            logger.trace("Adding a new object to Vector for " + callingObject.getClass().getName());
            ObjectCounter ob=new ObjectCounter(1,callingObject);
            ob.setWaitInvocationObject(waitObj);
            objectsWaiting.add(ob);
        }
        // kick off timer first...
        waitObj.kickOffWaitTimer(millis);

        Logging.monitorStatus(controller, Logging.MonitorStatusType.WAIT, threadName);
        controller.getThreadMonitor().threadPaused(threadName); // we are stopping ...
        waitObj.agentJWait(callingObject); // returns when done ...

        Logging.monitorStatus(controller, Logging.MonitorStatusType.RELEASED, threadName);
      //  if (!waitObj.getWasTimedOut())
        controller.getThreadMonitor().threadContinued(threadName); // timed out objects already have added one to threadcount
        synchronized(callingObject) {
        callingObject.notify(); // to release notify method ....
        }
 //       ProtolibTimer timer = new ProtolibTimer(controller, 0, 0);
 //       timer.startTimerAndBlockUntilTimeout(); // and stopping
        // and starting again when ns-2 has caught up...

        if (waitObj.getWasInterrupted()) throw waitObj.getInterruptedException();
        logger.debug("leaving");
    }

    public static final void wait(java.lang.Object callingObject, long timeout, int nanos) throws InterruptedException { 
        logger.debug("Entering");
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException(
                    "nanosecond timeout value out of range");
        }

        if (nanos >= 500000 || (nanos != 0 && timeout == 0)) {
            timeout++;
        }

        javm.lang.StaticObject.wait(callingObject, timeout);
        logger.debug("Leaving");
     }

    public final static void notify(java.lang.Object callingObject) {
        logger.debug("Entering");
         Controller controller = AgentJVirtualMachine.getCurrentNS2NodeController();
         Thread waitThread = Thread.currentThread();
         String threadName = waitThread.getClass().getName();

         ObjectCounter objctr = getObject(callingObject);
         if (objctr!=null) {
             logger.debug("Notify: check ok, object waiting");
             if (objctr.decrementandCheckZero()) objectsWaiting.remove(objctr);

             if (objctr.getWaitInvocationObject()!=null)
                 objctr.getWaitInvocationObject().cancelTimerIfSet();

             Logging.monitorStatus(controller, Logging.MonitorStatusType.NOTIFYING, threadName);
             synchronized (callingObject) {
                 callingObject.notify(); // call the REAL java.lang.notify to unrealse a thread
             }
             // to wait for the wait to return ...

             try {
                 synchronized (callingObject) {
                 callingObject.wait();
                 }
             } catch (InterruptedException e) {
                 e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
             }
          } else {
             logger.trace("Notify: No objects waiting for " + callingObject.getClass().getName() + " ok, proceeding...");
         }

        logger.debug("Exiting");
    }

    public static final void notifyAll(java.lang.Object callingObject) {
        logger.debug("Entering");
        Controller controller = AgentJVirtualMachine.getCurrentNS2NodeController();
        Thread waitThread = Thread.currentThread();
        String threadName = waitThread.getClass().getName();
        ObjectCounter objctr = getObject(callingObject);
        if ((objctr!=null) && (objctr.getObjectsWaiting()!=0)) {
            logger.debug("NotifyAll: check ok, object waiting");
            do {
                logger.debug("NotifyAll: notifying object!!!");
            } while (!objctr.decrementandCheckZero());

            objectsWaiting.remove(objctr);
            if (objctr.getWaitInvocationObject()!=null)
                 objctr.getWaitInvocationObject().cancelTimerIfSet();
            
            logger.debug("Calling NotifyAll now");
            Logging.monitorStatus(controller, Logging.MonitorStatusType.NOTIFYING, threadName);


            synchronized (callingObject) {
                callingObject.notifyAll(); // call the REAL java.lang.notify to unrealse a thread
            }

            // to wait for the wait to return ...
            try {
                synchronized (callingObject) {
                callingObject.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

        } else {
            logger.trace("NotifyAll: No objects waiting for " + callingObject.getClass().getName() + " ok, proceeding...");
        }

        logger.debug("Exiting");
    }

    static ObjectCounter getObject(Object targetObject) {
        for (int i=0; i< objectsWaiting.size(); ++i) {
            ObjectCounter objctr=objectsWaiting.get(i);
 //           logger.debug("And they equal " + (targetObject == objctr.getTargetObject()));
            if (targetObject == objctr.getTargetObject()) {
                 logger.debug("Matched " + targetObject + " with " + objctr.getTargetObject());
                 return objctr;
            }
        }
        logger.debug("Returning null :)");
        return null;
    }

}