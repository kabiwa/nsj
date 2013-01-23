package agentj.nativeimp.util;

import proto.logging.api.Log;
import proto.logging.api.Logger;

import java.util.Vector;

import agentj.thread.Controller;
import agentj.thread.ThreadWorker;

/**
 * A Thread pool for running threads within agentj
 * <p/>
 * Created by scmijt
 * Date: Jul 25, 2007
 * Time: 5:20:44 PM
 */
public class AgentJSocketPool {
    static Logger logger = Log.getLogger(AgentJSocketPool.class);
    static int MINIMUM_THREADS=20;

    Controller controller;

    Vector threadWorkers;

    public AgentJSocketPool(Controller controller) {
        this.controller = controller;
        ThreadWorker worker;
        threadWorkers = new Vector();

        for (int i=0; i< MINIMUM_THREADS; ++i) {
            worker = new ThreadWorker(controller);
            worker.start();
            threadWorkers.add(worker);
        }
    }

    /**
     *
     * Returns a WorkerThread from the pool or a new object if all are being used
     *
     * @return a workerThread
     */
    public ThreadWorker getWorkerThread() {
        ThreadWorker worker;
        if (threadWorkers.size()>0) {
            worker = (ThreadWorker)threadWorkers.remove(0);
            return worker;
        } else {
            worker = new ThreadWorker(controller);
            worker.start();
            return worker;
        }
    }

    /**
     * When a worker is released (i.e. when it has finished work) then is is added back to the pool
     *
     * @param threadWorker
     */
    public void addWorkerThreadToPool(ThreadWorker threadWorker) {
        threadWorkers.add(threadWorker);
        AgentJSocketPool.logger.trace("Pool Size is now " + threadWorkers.size());
    }
}