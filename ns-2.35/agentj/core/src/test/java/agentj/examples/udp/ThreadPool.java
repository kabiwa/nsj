/*
 * Copyright 2004 - 2007 University of Cardiff.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package agentj.examples.udp;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Executor;

/**
 * Class Description Here...
 *
 * @author Andrew Harrison
 * @version $Revision: 1.2 $
 * @created Mar 25, 2008: 8:19:06 AM
 * @date $Date: 2008-10-30 20:08:20 $ modified by $Author: ian $
 * @todo Put your notes here...
 */

public class ThreadPool implements Executor {

    ArrayList<ThreadWorker> threads;

    ThreadGroup threadGroup;

    boolean daemon;

    public ThreadPool(int numberOfThreads) {
        this(numberOfThreads, false);
    }

    public ThreadPool(int numberOfThreads, boolean asDaemon) {
        if (numberOfThreads < 1) {
            throw new IllegalArgumentException(
                    "number of threads must be greater than 0: " + numberOfThreads);
        }

        threadGroup = new ThreadGroup("ThreadPool group1");
        threads = new ArrayList<ThreadWorker>(numberOfThreads);
        daemon = asDaemon;

        for (int i = 0; i < numberOfThreads; i++) {
            System.out.println("Adding thread number " + i);
            addThreadWorker();
        }
    }

    public boolean isActive() {
        for (ThreadWorker tw : threads) {
            if (tw.isAlive()) {
                return true;
            }
        }
        return false;
    }

    public synchronized int getNumberOfThreads() {
        return threads.size();
    }

    public synchronized void addThreadWorker() {
        ThreadWorker tw = new ThreadWorker(threadGroup,
                "ThreadWorker" + threads.size());
        tw.setDaemon(daemon);
        tw.start();
        threads.add(tw);
    }

    public synchronized int idleThreads() {
        int count = 0;
        for (ThreadWorker tw : threads) {
            if (tw.getQueueSize() == 0) {
                count++;
            }
        }
        return count;
    }

    public synchronized void removeIdleThreads() {
        Iterator<ThreadWorker> i = threads.iterator();
        while (i.hasNext()) {
            ThreadWorker tw = i.next();
            if (tw.getQueueSize() == 0) {
                tw.endAfterLast();
                i.remove();
            }
        }
    }

    public synchronized boolean removeIdleThread() {
        if (threads.size() < 1) {
            return false;
        }
        Iterator<ThreadWorker> i = threads.iterator();
        while (i.hasNext()) {
            ThreadWorker tw = i.next();
            if (tw.getQueueSize() == 0) {
                tw.endAfterLast();
                i.remove();
                return true;
            }
        }

        return false;
    }

    public synchronized void finishAll(boolean now) {
        Iterator<ThreadWorker> i = threads.iterator();
        while (i.hasNext()) {
            if (now) {
                i.next().endAfterCurrent();
            } else {
                i.next().endAfterLast();
            }
            i.remove();
        }
    }

    public void shutdown() {
        finishAll(true);
    }

    public synchronized int getQueueSize() {
        int count = 0;
        for (ThreadWorker tw : threads) {
            count += tw.getQueueSize();
        }
        return count;
    }


    public void execute(Runnable task) {
        runTask(task);
    }

    public synchronized void runTask(Runnable task)
            throws IllegalStateException {
        if (threads.size() < 1) {
            throw new IllegalStateException("no threads in pool");
        }

        int min = Integer.MAX_VALUE;
        ThreadWorker t = threads.get(0);

        for (ThreadWorker tw : threads) {
            int n = tw.getQueueSize();
            if (n == 0) {
                tw.runTask(task);
                return;
            }
            if (min > n) {
                min = n;
                t = tw;
            }
        }

        t.runTask(task);
    }
}
