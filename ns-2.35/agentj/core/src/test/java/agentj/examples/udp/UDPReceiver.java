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

import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.DatagramPacket;
import java.io.IOException;

/**
 * Class Description Here...
*
* @author Andrew Harrison
* @version $Revision: 1.4 $
* @created Oct 10, 2008: 11:15:51 AM
* @date $Date: 2008-12-09 17:23:53 $ modified by $Author: ian $
* @todo Put your notes here...
*/
public class UDPReceiver extends Thread {

    private DatagramSocket socket;
    private boolean stopped = false;
    private ConvergentSender sender;

    public UDPReceiver(ConvergentSender sender) {
        this.sender = sender;
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public DatagramSocket getSocket() {
        return socket;
    }

    public synchronized void run() {
        byte[] buf = new byte[1600];
        DatagramPacket dp = new DatagramPacket(buf, buf.length);
        try {
            while (!stopped) {
                socket.receive(dp);
                byte[] data = new byte[dp.getLength()];
                System.arraycopy(dp.getData(), dp.getOffset(), data, 0, data.length);
                System.out.println("UDPReceiver.run ------- received response from server:" + new String(data));
                sender.addReply(data);
            }

        } catch (SocketException e) {
            //e.printStackTrace();
            System.out.println("caught exception " + e.getMessage());
        } catch (IOException ioe) {
            System.out.println("caught exception " + ioe.getMessage());

            //ioe.printStackTrace();
        }
        System.out.println("Run Finishing");
    }

    public void close() {
        try {
            if (!stopped) {
                stopped = true;
                socket.close();
                //interrupt();

            }
        } catch (Exception e) {
            System.out.println("caught exception " + e.getMessage());
            //e.printStackTrace();
        }
    }
}
