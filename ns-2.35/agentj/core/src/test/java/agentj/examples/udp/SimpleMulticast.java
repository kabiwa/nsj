package agentj.examples.udp;

import agentj.AgentJAgent;
import proto.logging.api.LogProcessor;
import proto.logging.api.Logger;

import java.io.IOException;
import java.net.*;

/**
 * @author Ian Taylor
 *
 * Simple multicast class that works in AgentJ and NS2 - see the
 * SimpleMulticast.tcl script in the examples directory
 */

public class SimpleMulticast extends AgentJAgent {
    static int groupPort1 =5000;
    static int groupPort2 =5005;
    static InetAddress group1;

    static int unicastport=10;
    InetAddress returnAddress=null;

    private DatagramSocket unicast;

    public SimpleMulticast () {
        this.setJavaDebugLevel(Logger.LogLevel.ALL);
        this.setNativeDebugLevel(AgentJDebugLevel.max);
    }

    public void init() {
        try {
                group1 = InetAddress.getByName("228.5.6.7");
            } catch (UnknownHostException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
    }


    public void receive() {
        (new MulticastReceive(group1, groupPort1)).start();
        (new MulticastReceive(group1, groupPort2)).start();
        (new MulticastReceive(group1)).start();

    }

    public void send() {
        try {
            String msg = "Hello";
            DatagramPacket hi = new DatagramPacket(msg.getBytes(),
                    msg.length(), group1, groupPort1);

            System.out.println("SimpleMulticast, node " + " sending = " + msg);
            DatagramSocket sock = new DatagramSocket(15);
            sock.send(hi);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }
    

    public void sendUnicast() {
        if (returnAddress==null) {
            System.out.println("SimpleMulticast: No return address to send data to" );            
            return;
        }
        try {
            String msg = "Nice to make your acquaintance......";
            DatagramPacket hi = new DatagramPacket(msg.getBytes(),
                    msg.length(), returnAddress, unicastport);

            System.out.println("SimpleMulticast, node " + InetAddress.getLocalHost().getHostName() +
                    " sending = " + msg);
            new DatagramSocket(12).send(hi);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }


    public void receiveUnicast() {
        try {
            unicast=new DatagramSocket(unicastport);
        } catch (SocketException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        byte[] buf = new byte[1000];
        DatagramPacket recv = new DatagramPacket(buf, buf.length);
        try {
            System.out.println("SimpleMulticast Calling Unicast receive" );
            unicast.receive(recv);
            System.out.println("SimpleMulticast running on node " + InetAddress.getLocalHost().getHostName() +  " " +
                    "received UNICAST Packet ------> " + new String(recv.getData()));
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public boolean command(String command, String args[]) {
        if (command.equals("init")) {
            init();
            return true;
        }
        else if (command.equals("send")) {
            send();
            return true;
        }
        else if (command.equals("receive")) {
            receive();
            return true;
        }
        else if (command.equals("receive-unicast")) {
            receiveUnicast();
            return true;
        }
        else if (command.equals("send-unicast")) {
            try {
                if (args.length ==0) {
                    System.out.println("SimpleMulticast: ERROR USAGE = send-unicast <address>" );
                    System.exit(0);
                }
                returnAddress=InetAddress.getByName(args[0]);
            } catch (UnknownHostException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            sendUnicast();
            return true;
        }
        return false;
    }

    public void shutdown() {
        System.out.println("SimpleMulticast: Shutting down gracefully !");
    }

    public static void main(String[] args) {
        SimpleMulticast sim = new SimpleMulticast();
        try {
            sim.group1 = InetAddress.getByName("228.5.6.7");
        } catch (UnknownHostException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        sim.send();
        sim.receive();
    }

}
