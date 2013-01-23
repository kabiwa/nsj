package agentj.thread;

import proto.logging.api.Log;
import proto.logging.api.Logger;

/**
 * Must have one thread for each worker than runs code.  Threads are reused
 * through the use of a ThreaPool (ThreadWorkerPool).
 *
 */
public class ThreadWorker extends java.lang.Thread {
    static Logger logger = Log.getLogger(ThreadWorker.class);

    ThreadMonitor monitor;
    ReleaseSafeSync workerSync;
    String myID;
    boolean isWorking=true;
    Worker placeOfWork=null;
    String[] variables;
    Controller controller;
    private static int workerCounter=0;

    public ThreadWorker(Controller controller) {
        this.controller=controller;
        this.monitor = controller.getThreadMonitor();
        ++workerCounter;
        this.myID= "AgentJ Thread #" + workerCounter;
        workerSync = new ReleaseSafeSync(controller, ReleaseSafeSync.SyncType.THREADWORKER);
    }


    /**
     * Releases ThreadWorkerSync so that this thread starts work
     */
    public ReleaseSafeSync getSyncToReleaseWorker(Worker placeOfWork, String... variables) {
        monitor.registerThreadWorkerStart("WORKER STARTING: " + placeOfWork.getClass().getName(), this); // let AgentJ know we have started
        this.placeOfWork=placeOfWork;
        this.variables=variables;
        return workerSync;
    }
    /**
     * Runs a worker to perform the agentJ command operations invoked from the
     * Java code on a NS-2 node.
     */
    public void run() {
        do {
            workerSync.blockWithoutNotification();    // wait for work
            logger.trace("Worker starting to execute Java Code in Thread");
            placeOfWork.doWork(variables);
            // When a worker has finished add it back to the pool for reuse
            logger.trace("Worker Finished Executing Java Code in Thread");
            monitor.registerThreadWorkerStop("WORKER STOPPING: " + placeOfWork.getClass().getName(), this);
            controller.getThreadPool().addWorkerThreadToPool(this);
        } while (isWorking);
    }

    /**
     * @return the ID assigned to this worker thread
     */
    public String getMyID() {
        return myID;
    }

    public void stopThread() {
        isWorking = false;
    }
}
