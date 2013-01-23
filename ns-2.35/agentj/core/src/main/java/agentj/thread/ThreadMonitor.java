package agentj.thread;

import proto.logging.api.Log;
import proto.logging.api.Logger;
import util.Logging;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The ThreadMonitor class is responsible for keeping track of the threads
 * being created within AgentJ simulations.  It keep a track of threads
 * being created by: external applications by adding method calls to the start
 * and end of every run() method to invoke threadStarted and threadStopped
 * respectively in ClassLoader; and threads created by AgentJ (i.e. worker
 * threads for runnning Java code), which are created here by invoking the
 * run method.
 */
public class ThreadMonitor {
    static Logger logger = Log.getLogger(ThreadMonitor.class);

    static String threadClassName = new Thread().getClass().getName();

    private ThreadCounter threadCount;
    ConcurrentLinkedQueue threadqueue = new ConcurrentLinkedQueue();

    private final ReentrantLock threadCounterlock = new ReentrantLock();

    private Controller controller;

    private static int workersCreated = 0;
    private static int threadsCreated = 0;

    private ConcurrentHashMap<String, Integer> mapped =
            new ConcurrentHashMap<String, Integer>();

    Vector threadsRunning = new Vector();
    Vector workersRunning = new Vector();


    public ThreadMonitor(Controller controller) {
        this.controller = controller;
        threadCount = new ThreadCounter();
    }


    public static int getThreadsCreated() {
        return threadsCreated;
    }

    public void releaseSync(String className) {
        threadCounterlock.lock();  // block until condition holds
        try {
            logger.debug("Nodes have finished, threads = " + threadCount.getValue() + " on Node " + controller.getLocalHost());
            ReleaseSafeSync.releaseWorkerThread(className);
        } finally {
            threadCounterlock.unlock();
        }
    }

    public void threadStarted(String className, Object target) {
        threadCounterlock.lock();  // block until condition holds

        try {
            String host = controller.getLocalHost();

            threadCount.increment();
            Logging.threadStatus(controller, Logging.ThreadStatusType.STARTED, className);
            if (className.indexOf("Threadworker for WORKER") == -1) {
                Integer num = mapped.get(className);
                if (num == null) {
                    mapped.put(className, 1);
                } else {
                    mapped.put(className, num + 1);
                }
            }

            logger.debug("Thread " + className + " Started - " + threadCount.getValue() + " threads running on Node " + host);
        } finally {
            threadCounterlock.unlock();
        }
    }

    public void threadStarted(String className) {
        threadStarted(className,null);
    }

    public void threadStopped(String className) {
        threadStopped(className, null);
    }

    public void threadStopped(String className, Object target) {
        threadCounterlock.lock();  // block until condition holds

        try {
            String host = controller.getLocalHost();

            Thread cur = Thread.currentThread();

            logger.debug("Thread STOPPING, source " + className + " on node " + host);
            if (className.indexOf("Threadworker for WORKER") == -1) {
                Integer num = mapped.get(className);
                if (num == null) { // maybe a Runnable Object started - see if there are any "Threads" started and guess
                                    // that if there are then this is a corresponding Runnable object being stopped.
                    if (target!=null) {
                        Class[] interfaces = target.getClass().getInterfaces();
                        if (interfaces != null) { // check first if its a runnable object being stopped
                            for (int in = 0; in < interfaces.length; ++in) {
                                if (interfaces[in].getName().equals("java.lang.Runnable")) {
                                    num = mapped.get(threadClassName);
                                    break;
                                }
                            }
                        }
                    }
                }

                if (num == null || num == 0) {
                    logger.debug("a thread has stopped,but no threads of class " + className + " were started on this node");
                    return;
                } else {
                    num = num - 1;
                    if (num == 0) {
                        mapped.remove(className);
                    } else {
                        mapped.put(className, num);
                    }
                }
            }
            threadCount.decrement();
            Logging.threadStatus(controller, Logging.ThreadStatusType.STOPPED, className);

            logger.debug("Thread " + className + " STOPPED - " + threadCount.getValue() + " threads running on Node " + host);

            if (threadCount.getValue() <= 0) { // i.e. no more workers or thread
                releaseSync(className);
                threadCount.reset();
            }
            //  }

        } finally {
            threadCounterlock.unlock();
        }
    }


    public void threadContinued(String className) {
        threadCounterlock.lock();  // block until condition holds
        String host = controller.getLocalHost();

        try {
            threadCount.increment();

            Logging.threadStatus(controller, Logging.ThreadStatusType.RESUMED, className);

            logger.debug("Thread CONTINUED: source " + className + " - " + threadCount.getValue() + " threads running on Node " + host);
        } finally {
            threadCounterlock.unlock();
        }
    }

    public void threadPaused(String className) {
        threadCounterlock.lock();  // block until condition holds
        String host = controller.getLocalHost();

        try {
            threadCount.decrement();

            logger.debug("Thread PAUSED: source " + className + " - " + threadCount.getValue() + " threads running on Node " + host);

            Logging.threadStatus(controller, Logging.ThreadStatusType.PAUSED, className);

            if (threadCount.getValue() <= 0) { // i.e. no more workers or thread
                releaseSync(className);
                threadCount.reset();
            }
        } finally {
            threadCounterlock.unlock();
        }
    }

    /**
     * Implements synchronised starting and registration of a thread (when start has
     * been called)
     *
     * @param target that is being started
     */
    public void registerThreadStart(Object target) {
        ++threadsCreated;
        logger.trace("AGENTJ STATS: Total number of threads created " + threadsCreated);

        //       if (target instanceof Thread) { // a runnable object ...
        //           Thread thread = (Thread)target;
        //           thread.setName("AgentJ Detected Thread #" + threadsCreated);
        //     }

        threadsRunning.add(target);

        logger.trace("Thread Detected by AgentJ - incrementing threadcount now " + target.getClass().getName());

        threadStarted(target.getClass().getName(), target);
    }

    /**
     * Notifies that a thread has been stopped. This is invoked at the end of a run()
     * method that is contained within a Thread or Runnable object.  Also, we check
     * here whether that thread has been previously started before we stop it by
     * checking the threadqueue to see if it has been started.  We do this because
     * it is conceivable that run is invoked directly without creating a thread...
     *
     * @param target
     */
    public void registerThreadStop(Object target) {
        threadsRunning.remove(target);
        threadStopped(target.getClass().getName(), target);
    }


    /**
     * Implements synchronised starting and registration of a worker
     *
     * @param worker that is being started
     */
    public void registerThreadWorkerStart(String placeOfWork, Thread worker) {
        ++workersCreated;
        logger.trace("AGENTJ STATS: Total number of worker threads created " + workersCreated);

        workersRunning.add(worker);

        threadStarted("Threadworker for WORKER started for object " + placeOfWork);
    }

    /**
     * Notifies that a worker thread has been stopped.
     *
     * @param worker
     */
    public void registerThreadWorkerStop(String placeOfWork, Thread worker) {
        workersRunning.remove(worker);
        threadStopped("Threadworker for WORKER stopped for object " + placeOfWork);
    }


    public String getRunningThreadsAsString() {
        StringBuffer threads = new StringBuffer();

        threads.append("Current Threads Running or waiting in AgentJ\n");
        threads.append("============================================\n");
        threads.append("");

        int i = 0;

        for (Object target : threadsRunning) {
            threads.append("Application Thread " + i + " : " + target.getClass().getName() + "\n");
            ++i;
        }

        for (Object target : workersRunning) {
            threads.append("Worker Thread " + i + " : " + target.getClass().getName() + "\n");
            ++i;
        }

        return threads.toString();
    }

    /**
     * Resets state of thread count for this node to 0.
     */
    public void reset() {
        threadCount.reset();

    }

    public int getThreadCount() {
        return threadCount.getValue();
    }

    public Controller getController() {
        return controller;
    }
}
