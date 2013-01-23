package agentj.examples.udp;

import java.net.MulticastSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.io.IOException;

/**
 * The ... class ...
 * <p/>
 * Created by scmijt
 * Date: Oct 31, 2008
 * Time: 8:00:10 AM
 */
public class MulticastReceive extends Thread {

    MulticastSocket socket;
    InetAddress group;

    int port;

    public MulticastReceive(InetAddress group, int port) {
        this.group = group;
        this.port=port;

        try {
            socket = new MulticastSocket(port);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        joinGroup();
    }

    public MulticastReceive(InetAddress group) {
         this.group = group;
         this.port=0;

         try {
             socket = new MulticastSocket();
         } catch (IOException e) {
             e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
         }
        joinGroup();
     }

    public void joinGroup() {
        try {
            socket.joinGroup(new InetSocketAddress(group,8888), null);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void run() {
        receive();
    }


    public void receive() {
        byte[] buf = new byte[1000];
        DatagramPacket recv = new DatagramPacket(buf, buf.length);
        try {
            System.out.println("Multicast socket for port " + port + " Calling receive" );
            socket.receive(recv);
            System.out.println("Multicast socket for port " + port +
                    " received ------> " + new String(recv.getData()));
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void shutdown() {
        System.out.println("SimpleMulticast: Shutting down gracefully !");
        try {
            socket.leaveGroup(group);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}