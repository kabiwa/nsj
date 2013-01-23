package agentj.examples.udp;

import agentj.AgentJAgent;

import java.io.IOException;
import java.net.*;

import proto.logging.api.LogProcessor;
import proto.logging.api.Logger;


/**
 * @author Ian Taylor.
 *         A demo of a NS2 Java Object that
 */
public class Client extends AgentJAgent {
    DatagramSocket s;

    public Client() {
    }

    public void init() {
/*
        try {
            s = new DatagramSocket();
        } catch (SocketException e) {
            System.out.println("Error opening socket");
        }
        catch (IOException ep) {
            System.out.println("Error opening socket");
        }*/
    }


    public void send(final String text, final String address) {
        new Thread() {
            public void run() {
                try {
                    DatagramSocket s = new DatagramSocket();

                    byte b[] = (text.getBytes());
                    DatagramPacket p = new DatagramPacket(b, b.length,
                            new InetSocketAddress(address, 4444));
                    s.send(p);
                    s.close();
                    System.gc();
                } catch (IOException eh) {
                    System.out.println("Error Sending Data");
                }
            }
        }.start();
    }


    public boolean command(String command, String args[]) {
        if (command.equals("init")) {
            init();
            return true;
        } else if (command.equals("send")) {
            System.out.println("Client Sending " + args[0] + " to " + args[1]);
            send(args[0], args[1]);
            return true;
        }
        return false;
    }

    public void shutdown() {
        System.out.println("Client: Shutting down gracefully !");
    }
}
