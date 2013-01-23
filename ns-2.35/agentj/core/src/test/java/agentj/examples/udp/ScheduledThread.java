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


/**
 * Class Description Here...
 *
 * @author Andrew Harrison
 * @version $Revision: 1.1 $
 * @created Mar 25, 2008: 5:44:41 PM
 * @date $Date: 2008-10-22 19:47:50 $ modified by $Author: harrison $
 * @todo Put your notes here...
 */

public class ScheduledThread extends Thread {

    private long initialDelay;
    private long interval;
    private boolean stop = false;
    ThreadedMulticast tm;
    private long startTime;
    private int count = 0;

    public ScheduledThread(ThreadedMulticast tm, long initialDelay, long interval) {
        this.tm = tm;
        this.initialDelay = initialDelay;
        this.interval = interval;
        this.startTime = System.currentTimeMillis();
    }

    public void shutdown() {
        stop = true;
        this.interrupt();
    }

    public void execute() {
        start();
    }

    public void run() {
        try {
            System.out.println("ScheduledThread.run entering initial delay at " + System.currentTimeMillis());
            sleep(initialDelay);
            System.out.println("ScheduledThread.run exiting initial delay at " + System.currentTimeMillis());
        } catch (InterruptedException e) {
        }

        synchronized (this) {
            while (!stop) {
                System.out.println("ScheduledThread.run at " + System.currentTimeMillis());
                //tm.discoverDAs();
                try {
                    sleep(interval);
                } catch (InterruptedException e) {
                }
            }
        }
    }


}
