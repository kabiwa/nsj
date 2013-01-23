package agentj.examples.udp;

import agentj.AgentJAgent;
import proto.logging.api.Log;
import proto.logging.api.Logger;
import proto.logging.api.LogProcessor;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * @author Ian Taylor.
 * A demo of a NS2 Java Object that
 */
public class Server extends AgentJAgent {
    static Logger logger = Log.getLogger(Server.class);

    public Server() {
    }

    DatagramSocket s;

    public void init() {
        logger.trace("entering");

        try {
            s = new DatagramSocket(4444);
        } catch (SocketException e) {
            System.out.println("Error opening socket");
        }
        catch (IOException ep) {
            System.out.println("Error opening socket");
        }

        logger.trace("socket created");
        int count = 1000;
        int i = 0;
        while(i++ < count) {
        try {
            byte b[] = new byte[1000];
            DatagramPacket p = new DatagramPacket(b,b.length);
            System.out.println("--------------------------------------  About to receive");

            s.receive(p);
            System.out.println("Received ----------------------------" + new String(p.getData()) + "--");
        } catch (IOException ep) {
            System.out.println("PAICommands: Error opening socket");
        }
        }
    }


    public boolean command(String command, String args[]) {
        if (command.equals("init")) {
            init();
            return true;
        }
        return false;
    }

    public void shutdown() {
        System.out.println("Server: Shutting down gracefully !");
    }
}
