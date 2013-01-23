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

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.*;
import java.io.IOException;

import agentj.AgentJAgent;
import proto.logging.api.Log;
import proto.logging.api.LogProcessor;
import proto.logging.api.Logger;


/**
 * Class Description Here...
 *
 * @author Andrew Harrison
 * @version $Revision: 1.2 $
 * @created Jul 29, 2008: 3:07:34 PM
 * @date $Date: 2008-11-07 11:37:22 $ modified by $Author: harrison $
 * @todo Put your notes here...
 */

public class WaitTest extends AgentJAgent {

    private boolean wakeup = false;

    public WaitTest() {
        this.setJavaDebugLevel(Logger.LogLevel.TRACE);
        this.setNativeDebugLevel(AgentJDebugLevel.max);
    }

    /**
     * Enables commands invoked in NS2 scripts to be passed along
     * to your Java object. The 'command' is the name of the
     * command and the 'args' are the arguments for the command
     *
     * @param command
     * @param args
     * @return true if the command was successful, false otherwise
     */
    public boolean command(String command, String args[]) {
        if (command.equals("send")) {
            System.out.println("Client calling waiter...");
            try {
                System.out.println("WaitTest.command trying first send");
                send();
                if(!wakeup) {
                    System.out.println("WaitTest.command first send returned false. trying again...");
                    send();
                    System.out.println("WaitTest.command after second call wakeup is " + wakeup);

                } else {
                    System.out.println("WaitTest.command returned true after first call");
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }


    public synchronized void send() throws IOException {

        try {
            //Waker r = new Waker(this);

            //r.start();

            long[] timeouts = {250L, 500L, 750L, 1000L, 1250L, 1500L, 2000L, 3000L, 4000L};
            int timeoutIndex = 0;
            long totalWait = System.currentTimeMillis();
            int noReplies = 0;
            while (!wakeup && timeoutIndex < timeouts.length) {
                long waitTime = timeouts[timeoutIndex];
                try {
                    System.out.println("WaitTest.send before WAIT");
                    wait(waitTime);
                    System.out.println("ConvergentSender.send AFTER WAIT");
                } catch (InterruptedException e) {
                }

                if (wakeup) {
                    // Keep the same timeout
                    // Reset the no result counter, in case we had the pattern: [message,] no reply, message
                    noReplies = 0;
                    break;
                } else {
                    // Timeout expired or duplicate messages
                    ++timeoutIndex;
                    ++noReplies;
                    // Exit if there are no result for 2 successive timeouts (RFC 2614 section 2.1.5)
                    if (noReplies > 1) {
                        break;
                    }
                }
            }

            //totalWait = System.currentTimeMillis() - totalWait;
            //System.out.println(" total wait time = " + totalWait);
            System.out.println("WaitTest.send wakeup=" + wakeup);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public synchronized void wakeUp(boolean b) {
        this.wakeup = b;
        notifyAll();
    }

    public static void main(String[] args) {
        new WaitTest().command("send", new String[0]);
    }

}