package agentj.examples.udp;

import agentj.AgentJAgent;
import proto.logging.api.Log;
import proto.logging.api.Logger;
import proto.logging.api.LogProcessor;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.InetAddress;

/**
 * @author Ian Taylor.
 *         A demo of a NS2 Java Object that
 */
public class RespondingServer extends AgentJAgent {
    static Logger logger = Log.getLogger(Server.class);

    DatagramSocket s;

    public RespondingServer() {
        this.setJavaDebugLevel(Logger.LogLevel.ERROR);
        this.setNativeDebugLevel(AgentJDebugLevel.max);
    }

    public void init() {
        logger.trace("entering");

        try {
            System.out.println("RespondingServer.init my address is " + InetAddress.getLocalHost().getHostAddress());

            s = new DatagramSocket(4444);

        } catch (SocketException e) {
            System.out.println("Error opening socket");
        }
        catch (IOException ep) {
            System.out.println("Error opening socket");
        }
        logger.trace("socket created");
        while(true) {
        try {
            byte b[] = new byte[1000];
            DatagramPacket p = new DatagramPacket(b, b.length);
            System.out.println("Server --------------------------------------  About to receive");

            s.receive(p);
            String result = new String(p.getData());
            System.out.println("Server Received ----------------------------" + result + "--");
            if (result.trim().equalsIgnoreCase("hello")) {
                String resp = new String(p.getData()) + " back";
                byte[] bs = resp.getBytes();
                DatagramPacket packet = new DatagramPacket(bs, bs.length,
                        p.getSocketAddress());
                s.send(packet);
            }
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

    public static void main(String[] args) {
        new RespondingServer().init();
        
    }
}