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
 * @version $Revision: 1.5 $
 * @created Jul 29, 2008: 3:07:34 PM
 * @date $Date: 2008-12-09 17:23:53 $ modified by $Author: ian $
 * @todo Put your notes here...
 */

public class ConvergentSender extends AgentJAgent {

    private DatagramPacket packet;
    List<byte[]> replys = new Vector<byte[]>();

    public ConvergentSender() {
        this.setJavaDebugLevel(Logger.LogLevel.TRACE);
        //this.setNativeDebugLevel(AgentJDebugLevel.max);
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
            System.out.println("Client Sending  to " + args[0]);
            try {
                //List<String> ret = send("hi", args[0]);
                //if(ret.size() == 0) {
                //    System.out.println("ConvergentSender.command no responses from request \"hi\"");
                //    System.out.println("ConvergentSender.command now trying to send \"hello\"");
                    List<String> ret = send("hello", args[0]);
                System.out.println("got list:" + ret);
                //    System.out.println("ConvergentSender.command got response from request \"hello\":" + ret);
                //} else {
                //    System.out.println("ConvergentSender.command got response from request \"hi\":" + ret);
                //}

            } catch (IOException e) {
                System.out.println("caught exception " + e.getMessage());
                //e.printStackTrace();
            }
            return true;
        }
        return false;
    }
    

    public synchronized List<String> send(String msg, String address) throws IOException {
        byte[] b = msg.getBytes();
        packet = new DatagramPacket(b, b.length,
                new InetSocketAddress(address, 4444));
        List<String> results = new ArrayList<String>();
        try {
            UDPReceiver r = new UDPReceiver(this);
            DatagramSocket socket = r.getSocket();
            r.start();
            
            System.out.println("ConvergentSender.send about to send message:: " + msg);
            socket.send(packet);
            System.out.println("ConvergentSender.sent message:: " + msg);
            long[] timeouts = {500L, 1000L, 2000L, 4000L};
            int timeoutIndex = 0;
            long totalWait = System.currentTimeMillis();
            long maxWait = 0;
            int noReplies = 0;
            int maxnum = 2;
            while (timeoutIndex < timeouts.length && results.size() < maxnum) {
                long waitTime = timeouts[timeoutIndex];
                maxWait += waitTime;
                if (maxWait > 1500L) {
                    System.out.println("ConvergentSender.send exceeded maximum wait timeout. Breaking out of waits.");
                    break;
                }
                int currCount = results.size();
                System.out.println("ConvergentSender.send current reply count is " + currCount);
                if (currCount < maxnum) {
                    System.out.println("ConvergentSender.send  I'm going to wait because I'm hoping for " + maxnum + " results...");
                }
                try {
                    System.out.println("ConvergentSender.send enter wait...");
                    wait(waitTime);
                } catch (InterruptedException e) {
                    System.out.println("ConvergentSender.send caught interrupted exception");
                }
                System.out.println("ConvergentSender.send after wait current reply count is " + results.size());
                boolean newMessages = results.size() > currCount;
                if (newMessages) {
                    System.out.println("ConvergentSender.send  got some new messages...");
                    // Keep the same timeout
                    // Reset the no result counter, in case we had the pattern: message, no reply, message
                    noReplies = 0;
                } else {
                    System.out.println("ConvergentSender.send  no new messages...");
                    // Timeout expired or duplicate messages
                    ++timeoutIndex;
                    ++noReplies;
                    // Exit if there are no result for 2 successive timeouts (RFC 2614 section 2.1.5)
                    // TODO: make this a configuration option
                    if (noReplies > 1) {
                        System.out.println("ConvergentSender.send  two successive timeouts with no results. Breaking out of waits.");
                        // drop out after two successive failures
                        break;
                    }
                }
            }

            totalWait = System.currentTimeMillis() - totalWait;
            System.out.println("ConvergentSender.send  total wait time = " + totalWait);
            System.out.println("ConvergentSender.send results=" + results);
            r.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    public synchronized void addReply(byte[] context) {
        replys.add(context);
        notifyAll();
    }

    public synchronized byte[] pop() {
        return replys.remove(0);
    }

    public synchronized boolean isEmpty() {
        return replys.isEmpty();
    }


    public static void main(String[] args) {
        new ConvergentSender().command("send", new String[]{"192.168.1.4"});
    }

}