package agentj.examples.nam;

import agentj.AgentJAgent;
import agentj.NAMCommands;
import proto.logging.api.LogProcessor;
import proto.logging.api.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;


/**
 * A Demos to show how to color nodes and add labels during a simulation. We use
 * simple multicast demo to ilustrate this. Every time a message is sent it
 * is added as a label to the NAM animation and colors are changed.
 */
public class NamDemo extends AgentJAgent {
    public InetSocketAddress group;
    private MulticastSocket s;
    static int msgcount=0;
    NAMCommands nam;

    public NamDemo() {
        // Always leave this blank - just to initialise object ....
        // The scheduler is not running when objects are created so do NOT
        // try and create sockets and so forth here. Use init instead.
        // You can set debug level here though:

        this.setJavaDebugLevel(Logger.LogLevel.DEBUG);
        this.setNativeDebugLevel(AgentJDebugLevel.detail);
        nam = this.getNamCommands();
        nam.setAnimationRate(0.02);
    }

    public void init() {
        group = new InetSocketAddress("228.5.6.7", 555);
        try {
            System.out.println("NamDemo, running on node " + InetAddress.getLocalHost().getHostName());
            System.out.println("NamDemo, Group Address Allocated = " + group.getHostName());
            s = new MulticastSocket(555);
            s.joinGroup(group, null);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }


    public void send() {
        nam.setNodeColor(NAMCommands.NamColor.chocolate);
        try {
            String msg = "Message #" + msgcount;
            ++msgcount;
            DatagramPacket hi = new DatagramPacket(msg.getBytes(),msg.length(), group);
            System.out.println("SimpleMulticast sending ----- " + msg);
            nam.setNodeLabel("Sent " + msg, NAMCommands.LabelPosition.down, NAMCommands.NamColor.red);
            s.send(hi);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void receive() {
        nam.setNodeColor(NAMCommands.NamColor.gold);
        byte[] buf = new byte[1000];
        DatagramPacket recv = new DatagramPacket(buf, buf.length);
        try {
            System.out.println("SimpleMulticast Calling receive" );
            s.receive(recv);
            String msg = new String(recv.getData());
            System.out.println("SimpleMulticast received ----- " + msg);
            nam.setNodeLabel("Received" + msg, NAMCommands.LabelPosition.up, NAMCommands.NamColor.blue);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public boolean command(String command, String args[]) {
        if (command.equals("init-server")) {
            init();
            return true;
        }
        if (command.equals("init-client")) {
            init();
            return true;
        }
        else if (command.equals("send")) {
            nam.traceAnnotate("Command: Send packet " + msgcount);
            send();
            return true;
        }
        else if (command.equals("receive")) {
            receive();
            return true;
        }
        return false;
    }
}
