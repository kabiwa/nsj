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
 * Time: 1:11:25 PM
 * Modified from http://www.editcorp.com/Personal/Lars_Appel/JavaDemo/
 * This program is just another basic client/server type example, except
 * it spawns a few GUI threads for human interaction.  It uses
 * thread.sleep to synchronize UDP sockets on the client and server.
 */
public class NetEchoUDP extends AgentJAgent {
    // waits for udp connections, reads a number from the client and returns
    // a response based on this request. use number <= 0 to stop the server.
    static Logger logger = Log.getLogger();

    static final int myPORT = 3031;
    static final int destPORT = 3032;

    // AgentJ method for NS-2

    public boolean command(String command, String args[]) {
        if (command.equals("init")) {
            String[] mainArgs = new String[args.length+1];
            mainArgs[0] = command;
            System.arraycopy(args,0, mainArgs, 1, args.length);
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

        String dst = (args.length >= 1) ? args[0] : "127.0.0.1";

        System.out.println("NetEchoUDP listener ready.");

        DatagramSocket rsocket = new DatagramSocket(myPORT);
        byte[] recvBuf = new byte[256];
        DatagramPacket recvpkt = new DatagramPacket(recvBuf, recvBuf.length);
        rsocket.receive(recvpkt);
        logger.trace("Received ----------------------------" + new String(recvpkt.getData()));

        java.net.InetAddress clientAddress = recvpkt.getAddress();
        String clientNodeid = clientAddress.getHostName();
        System.out.println("Server heard client on node: " + clientNodeid + "\n");

        String recvdata = new String(recvpkt.getData());
        recvdata = recvdata.trim();
        int n = Integer.parseInt(recvdata);
        InetSocketAddress respondToAddress = new InetSocketAddress(dst, destPORT);

        System.out.println("client from " + respondToAddress + " requested " + n + " lines");

        rsocket.close();


        System.out.println("Server sleeping 1/2 second...");
        Thread.sleep(500);

        DatagramSocket ssocket = new DatagramSocket(myPORT);
        DatagramPacket sendpkt;

        String response = "Sending " + n + " lines...";
        System.out.println(response);
        byte[] sendBuf = response.getBytes();
        sendpkt = new DatagramPacket(sendBuf, sendBuf.length, respondToAddress);
        ssocket.send(sendpkt);

        Object[] options = { "OK", "CANCEL" };
        //JOptionPane.showOptionDialog(null,"Client requested " + n + "lines.")
        int selectedValue = JOptionPane.showOptionDialog(null,
                "Client requested " + n + " lines.", "Server dialog",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);
        if (options[selectedValue] == options[1]) {
            System.out.println("Server bailing...\n");
            return;
        }

        //String input = JOptionPane.showInputDialog("How many messages to send?");
        //n = Integer.parseInt(input);

        for (int k = 1; k <= n; k++) {
            System.out.println("Server sleeping 1/2 second...");
            Thread.sleep(500);

            response = "This is test line number " + k;
            System.out.println(response);
            sendBuf = response.getBytes();
            sendpkt = new DatagramPacket(sendBuf, sendBuf.length, respondToAddress);
            ssocket.send(sendpkt);
        }
        System.out.println("Server sleeping 1/2 second...");
        Thread.sleep(500);

        response = "Thanks and bye";
        System.out.println(response);
        sendBuf = response.getBytes();
        sendpkt = new DatagramPacket(sendBuf, sendBuf.length, respondToAddress);
        ssocket.send(sendpkt);

        ssocket.close();

    }
}

