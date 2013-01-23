package agentj.examples.gui;

import agentj.AgentJAgent;
import proto.logging.api.Log;
import proto.logging.api.Logger;

import javax.swing.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

/**
 * Created by IntelliJ IDEA.
 * User: iandow
 * Date: Jul 13, 2006
 * Time: 1:12:58 PM
 * Modified from http://www.editcorp.com/Personal/Lars_Appel/JavaDemo/
 * This program is just another basic client/server type example, except
 * it spawns a few GUI threads for human interaction. The first
 * argument to main (or init, from ns-2) specifies how many responses will
 * be requested from the NetEcho server.
 *
 * This application can be run in both ns-2 and in the real-world.  If
 * running in the real world, you should start the server (NetEchoUDP)
 * first.
 *
 * Running in the real world goes something like this:
 *
 * cd ~agentj/classes/
 * java agentj/exmaples/NetClientUDP
 *
 *
 * Running in ns2 goes something like this:
 *
 * ../ns NetEcho_wireless.tcl
 *
 */
public class NetClientUDP extends AgentJAgent {
    // opens udp connection to NetEchoUDP server, requests a number of lines and
    // displays the response received. Optional args are N and target host.
    static Logger logger = Log.getLogger();

    static final int myPORT = 3030;
    static final int destPORT = 3031;

    // AgentJ method for NS-2
    public boolean command(String command, String args[]) {
        if (command.equals("init")) {
            logger.trace("=======args length is: " + args.length);
            String[] mainArgs = new String[args.length+1];
            mainArgs[0] = command;
            System.arraycopy(args, 0, mainArgs, 1, args.length);
            try {
                main(args);
            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            return true;
        }
       return false;
    }

    public static void main(String args[])
            throws Exception {
        // set default number of lines to request echoed
        String req = (args.length >= 1) ? args[0] : "100";
        //default server's address (for real world use)
        String dst = (args.length >= 2) ? args[1] : "127.0.0.1";

        req = JOptionPane.showInputDialog(null, "How many messages to send?",
                "Client dialog", JOptionPane.QUESTION_MESSAGE);

        // System.out.println("client sending to " + dst);

        ProgressBarFrame frame = new ProgressBarFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.progressBar.setMaximum(Integer.parseInt(req));
        frame.setVisible(true);

        DatagramSocket ssocket = new DatagramSocket(myPORT);

        byte[] sendBuf = req.getBytes();

        InetSocketAddress address = new InetSocketAddress(dst, destPORT);
        DatagramPacket sendpkt = new DatagramPacket(sendBuf, sendBuf.length, address);

        String mytext = new String(sendpkt.getData());
        System.out.println("Sending " + sendBuf.length + " bytes of text: " + mytext);

        ssocket.send(sendpkt);
        ssocket.close();

        DatagramSocket rsocket = new DatagramSocket(3032);
        byte[] recvBuf = new byte[256];
        DatagramPacket recvpkt = new DatagramPacket(recvBuf, recvBuf.length);

        rsocket.receive(recvpkt);
        logger.trace("Received ----------------------------" + new String(recvpkt.getData()));

        java.net.InetAddress serverAddress = recvpkt.getAddress();
        String serverNodeid = serverAddress.getHostName();
        System.out.println("Client heard server on node : " + serverNodeid + "\n");

        int i=0;
        String recvdata = new String(recvpkt.getData());
        recvdata = recvdata.trim();
        while (recvdata.compareTo("Thanks and bye") != 0 ) {
            System.out.println("Server said: " + recvdata);

            recvBuf = new byte[256];
            recvpkt = new DatagramPacket(recvBuf, recvBuf.length);
            rsocket.receive(recvpkt);
            logger.trace("Received ----------------------------" + new String(recvpkt.getData()));
            recvdata = new String(recvpkt.getData());
            recvdata = recvdata.trim();

            // Update GUI progress bar and text area
            i++;
            frame.setProgress(i);
            frame.textArea.append("Server said, \"" + recvdata + "\"\n");

        }
        System.out.println("Server said: " + recvdata);

        rsocket.close();

    }
}

