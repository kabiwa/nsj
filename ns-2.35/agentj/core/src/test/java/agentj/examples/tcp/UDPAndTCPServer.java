package agentj.examples.tcp;

import agentj.AgentJAgent;
import proto.logging.api.LogProcessor;
import proto.logging.api.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;


/**
 * A TCP Server that notifies its address using UDP to all nodes that join the UDP multicast
 * group.  This address is used by clients to connect to and make a TCP connection
 * for passing data from the client to the server. The server then responds with a hello too
 *
 * @author Ian Taylor
 */
public class UDPAndTCPServer extends AgentJAgent {
    ServerSocket serverSocket = null;
    MulticastSocket s;
    InetSocketAddress group;
    Socket kkSocket = null;
    int port = 427;

    public void init() {
        this.setJavaDebugLevel(Logger.LogLevel.ERROR);
        this.setNativeDebugLevel(AgentJDebugLevel.error);                
    }

    public void initUDPSocket() {
        init();
        group = new InetSocketAddress("228.5.6.7", 555);
        try {
            System.out.println("UDPAndTCPServer, running on node " + InetAddress.getLocalHost().getHostName());
            System.out.println("UDPAndTCPServer, Group Address Allocated = " + group.getHostName());
            s = new MulticastSocket(555);
            s.joinGroup(group, null);

        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void sendMyID() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();

            String msg = localHost.getHostName() + " " + port;
            DatagramPacket id = new DatagramPacket(msg.getBytes(),
                    msg.length(), group);

            System.out.println("UDPAndTCPServer sending ID = " + msg);
            s.send(id);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void open() {
        System.out.println("Running Server ...");

        try {

            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(port));

        } catch (IOException e) {
            System.err.println("Could not listen on port: " + port);
            System.exit(1);
        }
    }

    public void acceptConnection() {
        Socket clientSocket = null;
        try {
            clientSocket = serverSocket.accept();

            System.out.println("SimpleTCPServer::acceptConnection - connection accepted !!!!!!!");

            PrintWriter out = new PrintWriter(
                    clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            clientSocket.getInputStream()));
            String inputLine;

            inputLine = in.readLine();

            System.out.println("SimpleTCPServer, receing Message !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" );
            System.out.println("--> " + inputLine);

            out.println("Ah, Hello client, nice to make your acquaintance ...");
            out.flush();

            clientSocket.close();
            serverSocket.close();

        } catch (IOException e) {
            System.err.println("Accept failed.");
            System.exit(1);
        }
        System.out.println("SimpleTCPServer::finished ..");

    }

    public boolean command(String command, String args[]) {
        if (command.equals("initUDP")) {
             initUDPSocket();
            return true;
         }
        else if (command.equals("sendUDPMessage")) {
            sendMyID();
            return true;
        }
        else if (command.equals("open")) {
            open();
            return true;
        }
        else if (command.equals("accept")) {
            acceptConnection();
            return true;
        }
        return false;
    }
}
