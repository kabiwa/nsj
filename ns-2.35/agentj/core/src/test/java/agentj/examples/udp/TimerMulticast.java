package agentj.examples.udp;

import agentj.AgentJAgent;
import proto.logging.api.LogProcessor;
import proto.logging.api.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;

import java.net.DatagramSocket;


/**
 * @author Ian Taylor
 *
 * Simple multicast class that works in AgentJ and NS2 - see the
 * SimpleMulticast.tcl script in the examples directory
 */

public class TimerMulticast extends AgentJAgent {
    static InetSocketAddress group = new InetSocketAddress("228.5.6.7", 555);
    private MulticastSocket receiver;
    private DatagramSocket sender;
    int repeats=5;

    public TimerMulticast () {
        this.setJavaDebugLevel(Logger.LogLevel.ALL);
        this.setNativeDebugLevel(AgentJDebugLevel.max);
    }

    public void init() {
        try {
            System.out.println("TimerMulticast, running on node " + InetAddress.getLocalHost().getHostName());
            System.out.println("TimerMulticast, Group Address Allocated = " + group.getHostName());
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }



    public void send() {
        try {
            sender=new DatagramSocket(11);
            for (int i=0; i<repeats; ++i ) {

                String msg = new String("Hello server, message number " + i);
              
                DatagramPacket hi = new DatagramPacket(msg.getBytes(),
                        msg.length(), group);

                System.out.println("TimerMulticast sending ----------------------- " + msg);
                sender.send(hi);
                Thread.sleep(1000);  // wait one second then continue
            }
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

    public void receive() { // just carry on receiving untill all are done
        try {
            receiver = new MulticastSocket(555);
            receiver.joinGroup(group, null);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        for (int i=0; i<repeats; ++i ) {

            byte[] buf = new byte[10];
            DatagramPacket recv = new DatagramPacket(buf, buf.length);
            try {
                System.out.println("TimerMulticast Calling receive" );
                receiver.receive(recv);
                System.out.println("TimerMulticast received -------------------------------------- " + new String(recv.getData()));
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        try {
            receiver.leaveGroup(group,null);
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
        return false;
    }

    public static void main(String[] args) {
        TimerMulticast sim = new TimerMulticast();
        sim.group = new InetSocketAddress("228.5.6.7", 555);
        sim.init();
        sim.send();
        sim.receive();
    }

}
