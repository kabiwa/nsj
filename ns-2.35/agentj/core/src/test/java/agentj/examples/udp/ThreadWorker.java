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
import java.util.concurrent.Executor;

/**
 * Class Description Here...
 *
 * @author Andrew Harrison
 * @version $Revision: 1.1 $
 * @created Mar 25, 2008: 9:42:58 AM
 * @date $Date: 2008-10-22 19:47:50 $ modified by $Author: harrison $
 * @todo Put your notes here...
 */

public class ThreadWorker extends Thread implements Executor {

    ArrayList<Runnable> tasks = new ArrayList<Runnable>();

    boolean enabled = true;

    boolean endNow = false;

    int count = 0;

    boolean once = false;

    /**
     * Creates a new ThreadWorker object.
     */
    public ThreadWorker() {
        super();
    }

    /**
     * Creates a new ThreadWorker object.
     *
     */
    public ThreadWorker(String name) {
        super(name);
    }

    /**
     * Creates a new ThreadWorker object.
     *
     */
    public ThreadWorker(ThreadGroup group, String name) {
        super(group, name);
    }


    /**
     * returns the Runnable object currently executed by this
     * ThreadWorker
     *
     */
    public synchronized Runnable getCurrentTask() {
        return (tasks.isEmpty()) ? null : (Runnable) tasks.get(0);
    }

    /**
     * returns the number of Runnable objects waiting to be executed
     *
     */
    public synchronized int getQueueSize() {
        return tasks.size();
    }

    public synchronized boolean isIdle() {
        return tasks.isEmpty();
    }

    public synchronized Runnable[] getTasks() {
        return tasks.toArray(new Runnable[tasks.size()]);
    }

    public synchronized boolean removeTask(Runnable task)
            throws IllegalStateException {
        Runnable r = getCurrentTask();

        if (r == null) {
            return false;
        }

        if (r.equals(task)) {
            throw new IllegalStateException("given task is currently active");
        }

        return tasks.remove(task);
    }

    /**
     * returns the number of tasks that have been executed (including
     * the current one)
     *
     */
    public int getTaskCount() {
        return count;
    }

    public void execute(Runnable task) {
        runTask(task);
    }

    /**
     * runs the given task immediately after all previous tasks have
     * finished - once the ThreadWorker has been started.
     *
     * @param task 
     * @return the number of tasks currently in the queue (including the given
     *         one)
     * @throws IllegalStateException if the task has no chance anymore of being
     *                               run
     */
    public synchronized int runTask(Runnable task) throws IllegalStateException {
        if (endNow) {
            throw new IllegalStateException(
                    "ThreadWorker has been ended after current");
        }

        if (once && !isAlive()) {
            throw new IllegalStateException("ThreadWorker already finished");
        }

        if (tasks.isEmpty()) {
            notifyAll();
        }

        tasks.add(task);

        return tasks.size();
    }

    /**
     * should be called only once through <code>start()</code>
     *
     * @throws IllegalThreadStateException if the method is called through
     *                                     anything but <code>start()</code> once
     */
    public void run() throws IllegalThreadStateException {
        synchronized (this) {
            if (!isAlive()) {
                throw new IllegalThreadStateException();
            }

            if (once) {
                throw new IllegalThreadStateException();
            }

            once = true;
        }

        loop:
        while (true) {
            if (endNow) {
                break;
            }

            Runnable currentTask = null;

            synchronized (this) {

                if (tasks.isEmpty()) {
                    if (!enabled) {
                        break loop;
                    }

                    try {
                        System.out.println("ThreadWorker should be waiting ....");
                        wait();
                        System.out.println("ThreadWorker releasing ....");
                    } catch (InterruptedException ex) {
                    }
                } else {
                    currentTask = tasks.get(0);
                }
            }

            if (currentTask == null) {
                continue;
            }

            Throwable throwable = null;
            count++;

            try {
                currentTask.run();
            } catch (Throwable th) {
                throwable = th;
            } finally {
                synchronized (this) {
                    tasks.remove(0);
                }
            }
        }
    }

    /**
     * ends the thread after the last queued task ended; the effect is
     * irreversible
     */
    public synchronized void endAfterLast() {
        enabled = false;

        if (isIdle()) {
            notifyAll();
        }
    }

    /**
     * ends the thread after the current task ended; the effect is
     * irreversible
     */
    public synchronized void endAfterCurrent() {
        endNow = true;

        if (isIdle()) {
            notifyAll();
        }
    }

    public boolean willEndAfterCurrent() {
        return endNow;
    }

    public boolean willEndAfterLast() {
        return !enabled;
    }

}
