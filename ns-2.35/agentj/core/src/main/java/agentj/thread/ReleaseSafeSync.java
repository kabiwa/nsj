package agentj.thread;

import proto.logging.api.Logger;
import proto.logging.api.Log;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;

import util.Logging;
import agentj.AgentJVirtualMachine;

/**
 * The synchronisation between releasing internal agentj threads and the awaiting
 * threads continuation has been a problem for Agentj.  This class is an
 * implementation of a syn that ateempts to solve this issue, which is: when
 * a thread releases a waiting thread, the release method returns before
 * the waiting one, which cause a gap in which the thread counter decrements
 * and releases a node before the waiting thread has processed.  Previous
 * attempts at solving this including timing delays and incrementing
 * thread counter just before a release and then decrementing again once
 * the waiting thread continues.  Both schemes are hard to debug.
 *
 * Here, we synchronise internally so that the waiting thread always returns
 * before the releasing one. This class also implements lists for multiple
 * sends before releasing and synchronisation so that
 * "puts" before "waits" work too.
 *
 *
 * <p/>
 * Created by scmijt
 * Date: Oct 21, 2008
 * Time: 7:16:26 PM
 */
public class ReleaseSafeSync {
    static Logger logger = Log.getLogger(ReleaseSafeSync.class);

    Controller controller;
    ThreadMonitor threadMonitor;

    public static enum SyncType {TIMER, SOCKET, DATAGRAMSOCKET, THREADMONITOR, THREADWORKER}

    SyncType syncType;
    ArrayList dataStore = new ArrayList();
    private Lock lock = new ReentrantLock();
    private Condition blockCondition = lock.newCondition(); //Get the condition variable.

    // create a static worker lock. Used per node, one node at a time so one lock needed
    
    private static Lock workerlock = new ReentrantLock();
    static private Condition workerReleaseCondition = workerlock.newCondition(); //Get the condition variable.

    static int workersWaiting=0;

    boolean waitForWorkers=true;
    boolean waiting;
    boolean releaseWait;

    private ReleaseSafeSync() {}

    public ReleaseSafeSync(Controller controller, SyncType syncType) {
        this.controller = controller;
        this.syncType = syncType;
        this.threadMonitor=controller.getThreadMonitor();
    }


    /**
     * Blocks until this object is (or has been released)
     *
     * @return data held by the object releasing the lock
     */
    public Object blockAndGet() {
        Object ret = null;
        logger.debug("Blocking");
        log("Entering ");
 
        lock.lock();

        if(dataStore.size()==0) {
            log("blockAndGet: Awaiting");
            waiting=true;
            threadReleaseControl();
            blockCondition.awaitUninterruptibly();
            log("Unblocked await");
            threadGetControl();
        } else {
            log("blockAndGet: No need to await, datastore has " + dataStore.size() + " objects waiting");
        }
        waiting=false;

        if (dataStore.size()>0) {
            ret = dataStore.get(0);
            dataStore.remove(ret);
        }

        lock.unlock();

        log("Exiting ");

        return ret;
    }


    /**
     * Blocks until this object is (or has been released)
     *
     * @return data held by the object releasing the lock
     */
    public Object blockWithoutNotification() {
        log("Entering ");
        logger.debug("Blocking");
        Object ret = null;

        lock.lock();

        if(dataStore.size()==0) {
            waiting=true;
            blockCondition.awaitUninterruptibly();
            log("Unblocked await");
        } else {
            log("blockAndGet: No need to await, datastore has " + dataStore.size() + " objects waiting");
        }
        waiting=false;

        if (dataStore.size()>0) {
            ret = dataStore.get(0);
            dataStore.remove(ret);
        }

        lock.unlock();

        log("Exiting");

        return ret;
    }

    public boolean isWaiting() {
        return waiting;
    }

    /**
     * Releases the condition and passes the data to the waiting object.
     *
     * @param data
     */
    public void putAndRelease(Object data) {
        log("Entering ");
        lock.lock();

        logger.debug("Put and Release: waitForWorkers="+waitForWorkers + " waiting = " + waiting);

        if (data!=null) dataStore.add(data);

        log("About to signal, data objects waiting are " + dataStore.size());

        if ((waitForWorkers) && (waiting)){
            workerlock.lock();
            logger.debug("ABOUT TO WAIT FOR WORKERS NOW !!!!!!!!!!!");
            logger.debug("+++++++++++++++++++++++++++++++++++++++++");
        }

        if (waiting) {
            logger.debug("RELEASE WAIT... ");
            blockCondition.signal();
            releaseWait=false;
            lock.unlock();

            if (waitForWorkers) {
              //  System.out.println("WAITING FOR WORKERS: About to block now ... on node " + controller.getLocalHost());
                ++workersWaiting;
                int tries=0;
                while ((workersWaiting>0) || (threadMonitor.getThreadCount()>0)) {
                    try {
                        workerReleaseCondition.await(6, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        logger.debug("TIMED OUT WAITING FOR WORKERS - Attempt " + tries);
                        logger.debug(threadMonitor.getRunningThreadsAsString());
                    }
                    ++tries;
                    if (tries>10) {
                        logger.info("ReleaseSafeSync: WARNING, HAD TO RELEASE Node " +
                                controller.getLocalHost() + " threads still blocked after on minute....");
                        workersWaiting=0;
                        threadMonitor.reset();
                    }
                }
                logger.trace("WAITING FOR WORKERS: Released on node ... on node " + controller.getLocalHost());
                Logging.workStatus(this,controller,Logging.WorkStatusType.RELEASED, null);
                workerlock.unlock();
            }
        } else {
            lock.unlock();            
        }
        
        log("Exiting ");
    }


    public static void releaseWorkerThread(String className) {
        logger.debug("Static Release Entering");
        workerlock.lock();
        logger.debug("Attempting to Release Lock on Workers, checking worker count now ...");
        if (workersWaiting>0) {
            workerReleaseCondition.signal();
            --workersWaiting;
            logger.debug("Worker Count = " + workersWaiting);
        } else {
            System.err.println("++++++++++++++++++++ LOOK +++++++++++++++++++++++");
            System.err.println("Received a worker release with no workers waiting ");
            System.err.println("Worker Count = " + workersWaiting);
            System.err.println("Thread Count = " +
                    AgentJVirtualMachine.getCurrentNS2NodeController().getThreadMonitor().getThreadCount());
        }
        workerlock.unlock();
        logger.debug("Static Release Exiting");
    }

    public static int getWorkersWaiting() {
        return workersWaiting;
    }

    public boolean isWaitForWorkers() {
        return waitForWorkers;
    }

    public void setWaitForWorkers(boolean waitForWorkers) {
        this.waitForWorkers = waitForWorkers;
    }

    private void log(String message) {
        String addtolog=getSyncTypeAsString();

        logger.debug(addtolog + ((controller!=null)? " on node " + controller.getLocalHost():"") + ":" + message);
    }


    private String getSyncTypeAsString() {
        switch (syncType) {
            case TIMER:
                return "Timer Sync";
            case SOCKET:
                return "Socket Sync";
            case DATAGRAMSOCKET:
                return "Datagram Socket Sync";
            case THREADMONITOR:
                return  "ThreadMonitor Sync";
            case THREADWORKER:
                return  "ThreadWorker Sync";                
            default:
                return "SynType Not Defined";
        }
    }

    private void threadReleaseControl() {
        controller.getThreadMonitor().threadPaused(getSyncTypeAsString());
    }

    private void threadGetControl() {
        controller.getThreadMonitor().threadContinued(getSyncTypeAsString());
    }
}
