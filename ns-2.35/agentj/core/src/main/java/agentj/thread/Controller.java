package agentj.thread;

import agentj.AgentJAgent;
import agentj.AgentJVirtualMachine;
import agentj.Ns2Node;
import proto.logging.api.Log;
import proto.logging.api.Logger;

import java.util.concurrent.locks.ReentrantLock;

/**
 * The Controller class keeps control of the threads kciked off during a
 * simulation.  A worker is created for each chunk of Java code run within a
 * simulation (i.e. for each Ns-2 simulation command) and thereafter
 * the controller synchronises with this thread by using the WorkerMonitorSync
 * class.  The monitor is released when all threads that the Java code
 * kicks off terminate or finish in a known state i.e. either they call
 * socket receive(), they kick off a timer, or they call one of the
 * java.lang.Wait() methods. In each of these cases, the threadCount is
 * decremented indicating that this thread has (temporarily) finished.
 *
 * There is one Controller for each Ns-2 node in the simulation to monitor
 * threads created by that node.
 *
 * The controller is also responsible for keeping the NS-2 nodes ID up to date
 * in the AgentJVirtualMachine, which is used by the getLocalHost and used to synchronise
 * which controller's the sockets and timers speak too, since they use this current
 * ID to get a reference to the controller they should set up listeners for. The
 * current synchronisation sheeme is through the controller which sets the current
 * NS2 node to the one that set this controller up every time their is an "execute
 * user code" instruction or a callback from a socket of a timer. The exception
 * is wait and notify, which are set on the object they are applied to.
 *
 */
public class Controller {
    private final ReentrantLock lock = new ReentrantLock();
    static Logger logger = Log.getLogger(Controller.class);

    private static int controllerIDCount=0;
    private int controllerID;

    boolean retVal=true; // only set to false if command fails - if we block then we are ok
    private long nsAgentPtr;

    private String localHost; // the Ns-2 node that this controller is running on.

    String javaClassName;

    private ThreadMonitor threadMonitor;
    AgentJAgent agentJAgent; // the owner NS2 AgentJ node of this controller

    ThreadWorkerPool threadPool;

    Ns2Node ns2node;

    AgentJAgent agentJ; // this is the current executing agentJ node
    // Need to think about this - do we allow more than one Agent Per controller? If
    // not then agentJ and agentJAgent are the same

    private boolean localReceive=false; // set by the socket if they detect a local receive

    public Controller() {
        threadMonitor = new ThreadMonitor(this);
        controllerID=controllerIDCount;
        ++controllerIDCount;
    }

    public void setNodePropperties(Ns2Node node) {
        this.ns2node=node;
        this.localHost = node.getHostName();
        threadPool=new ThreadWorkerPool(this);
    }

    public void setAgentProperties(long nsAgentPtr, AgentJAgent agentJAgent) {
        this.agentJAgent = agentJAgent;
        this.nsAgentPtr=nsAgentPtr;
    }


    public ThreadWorkerPool getThreadPool() {
        return threadPool;
    }

    /**
     *
     * @return the C++ pointer to the Ns2 node that this controller is attached to
     */
    public long getNsAgentPtr() {
        return nsAgentPtr;
    }

    /**
     * Get's the local host i.e. node address for the node that this controller is running on.
     * @return
     */
    public String getLocalHost() {
        return localHost;
    }

    /**
     * Get the java object that repesents the ns-2 node that this controller is on.
     * @return
     */
    public Ns2Node getNs2node() {
        return ns2node;
    }

    /**
     * Controller ID for this controller. There is one controller per AgentJ node so
     * this gives the ID for the AgentJ node. So, if several exist on one NS-2 node
     * then it is possible to distinguish between them.
     *
     * @return
     */
    public int getControllerID() {
        return controllerID;
    }

    public String getControllerIDString() {
        return "AgentJID " + controllerID;
    }


    public void setRetVal(boolean retVal) {
        this.retVal = retVal;
    }

    /**
     * Called from BaseVM to execute thwe command. Here, we create
     * a worker and pass the command to execute to it

     * Kicks off the work thread for an agentj command.  This creates a Worker
     * Object and starts the work off in a new thread.  Once this has been started
     * this controller synchronises with the thread and waits for it to finish (
     * or for it to stop and wait for data).
     *
     * @param agentJ
     * @param command
     * @param args
     * @return String result from starting worker
     */
    public boolean executeCommand(AgentJAgent agentJ, String command, String args) {
        // SET NS-2 Node before execution of user code
        AgentJVirtualMachine.setCurrentNodeforAgent(agentJAgent);

        logger.info("COMMAND STARTED - Executing --" + command + "-- on Node " + getLocalHost() );
        logger.debug("<<<<================================================>>>>" );

        this.agentJ=agentJ;

        lock.lock();  // block until condition holds

        try {

            logger.trace("Switched Node to " + this.getJavaClassName() + ", node " + getLocalHost());
            logger.trace("Worker Starting on node " + localHost);

            logger.info("ThreadPool = " + threadPool);
            
            // IANS MODS - why on earth were the threads not being started?????????
            ThreadWorker threadWorker = threadPool.getWorkerThread();

            logger.info("ThreadWorker = " + threadWorker);

            ReleaseSafeSync sync = threadWorker.getSyncToReleaseWorker(agentJ, command, args);
            sync.setWaitForWorkers(true);

            logger.info("PUT AND RELEASE NOW !!!!!");

            sync.putAndRelease(null);

            logger.info("COMMAND " + command + " FINISHED on host " + getLocalHost());
            logger.debug("<<<<================================================>>>>" );

            while (AgentJEventQueue.popEvents()!= 0) {
                logger.trace("XXX: Sending Events on host " + getLocalHost());
            }

            if (ReleaseSafeSync.getWorkersWaiting()==0) {
                logger.trace("HOST " + getLocalHost() + " RELEASED, CONTROL GOING BACK TO  NS-2 ");
                logger.debug("<<<<=====================================================>>>>" );
                threadMonitor.reset();
            } else {
                logger.fatal("HOST " + getLocalHost() + " RELEASED to NS-2 WITH WORKERS STILL WORKING !!!! ");
                logger.fatal("Threads Running = " + ReleaseSafeSync.getWorkersWaiting());
                logger.fatal("<<<<=====================================================================>>>>" );
                logger.fatal("Aborting Run ....  " );
                logger.fatal(threadMonitor.getRunningThreadsAsString());
                System.exit(0);
            }
        } finally {
            lock.unlock();
        }

        return retVal;
    }


    /**
     * Typically callbacks kick off previous threads wating for data input.  This
     * is invoked just before the synchronisation is released in order to notify
     * the controller that work is about to start and should be monitored. The callback,
     * once the monitor has been released then calls waitForCallbackToFinish in
     * order to block until the thread has gracefully finished.
     *
     */
    public void executeCallback(Callback placeToPerformWork) {
        // SET NS-2 Node before execution of user code
        AgentJVirtualMachine.setCurrentNodeforAgent(agentJAgent);

        logger.info("CALLBACK STARTED for class " + placeToPerformWork.getClass().getName() + " -- on Node " + getLocalHost() );
        logger.debug("<<<<=======================================================================>>>>" );

        // threadMonitor.threadContinued(placeToPerformWork);

        //       threadMonitor.increaseThreadCountLevel(); // increase the level to count threads for this callback

        //     threadMonitor.startWorker(placeToPerformWork, getLocalHost());
        this.localReceive =false;

        Object data = placeToPerformWork.executeCallback();

        if (data !=null) {
            if (isLocalReceive())
                placeToPerformWork.getReleaseSafeSync().setWaitForWorkers(false);
            else
                placeToPerformWork.getReleaseSafeSync().setWaitForWorkers(true);

            placeToPerformWork.getReleaseSafeSync().putAndRelease(data);
        }

        logger.info("CALLBACK FINISHED on host " + getLocalHost());
        logger.debug("<<<<================================>>>>" );

        if (ReleaseSafeSync.getWorkersWaiting()==0) {
            logger.trace("HOST " + getLocalHost() + " RELEASED, CONTROL GOING BACK TO  NS-2 ");
            logger.debug("<<<<=====================================================>>>>" );
            threadMonitor.reset();
        } else {
            logger.fatal("HOST " + getLocalHost() + " RELEASED to NS-2 WITH WORKERS STILL WORKING !!!! ");
            logger.fatal("Threads Running = " + ReleaseSafeSync.getWorkersWaiting());
            logger.fatal("<<<<=====================================================================>>>>" );
            logger.fatal("Aborting Run ....  " );
            logger.fatal(threadMonitor.getRunningThreadsAsString());
            System.exit(0);
        }
    }


    public void executeCallbackInThread(Worker placeToPerformWork) {
        AgentJVirtualMachine.setCurrentNodeforAgent(agentJAgent);
        logger.debug("THREADED CALLBACK STARTED for class " + placeToPerformWork.getClass().getName() + " -- on Node " + getLocalHost() );
        logger.debug("<<<<=======================================================================>>>>" );

        ReleaseSafeSync sync = threadPool.getWorkerThread().getSyncToReleaseWorker(placeToPerformWork);
        sync.setWaitForWorkers(true);
        sync.putAndRelease(null);

        logger.info("THREADED CALLBACK FINISHED on host " + getLocalHost());
        logger.debug("<<<<================================================>>>>" );

        if (ReleaseSafeSync.getWorkersWaiting()==0) {
            logger.trace("THREADED CALLBACK HOST " + getLocalHost() + " RELEASED, CONTROL GOING BACK TO  NS-2 ");
            logger.debug("<<<<===============================================================================>>>>" );
            threadMonitor.reset();
        } else {
            logger.fatal("HOST " + getLocalHost() + " RELEASED to NS-2 WITH WORKERS STILL WORKING !!!! ");
            logger.fatal("Threads Running = " + ReleaseSafeSync.getWorkersWaiting());
            logger.fatal("<<<<=====================================================================>>>>" );
            logger.fatal("Aborting Run ....  " );
            logger.fatal(threadMonitor.getRunningThreadsAsString());
            System.exit(0);
        }
    }

    public void setLocalReceive(boolean localReceive, String source) {
        this.localReceive = localReceive;
        // if set to true, decrement thread count to get past block for callback, then increment once released
        //   if (localReceive)
        //     getThreadMonitor().threadPaused(source);
    }

    public boolean isLocalReceive() {
        return localReceive;
    }

    /**
     * Resets the thread count on this node.  Useful for recovery in the event of a
     * deadlock.  BUT SHOULD NOT BE USED EXCEPT FOR DEBUGGING....
     */
    public void resetThreadCount(Object caller) {
        while (threadMonitor.getThreadCount()>1) {
            threadMonitor.threadPaused(caller.getClass().getName());
        }
    }

    public ThreadMonitor getThreadMonitor() {
        return threadMonitor;
    }



    public String getJavaClassName() {
        return javaClassName;
    }

}

