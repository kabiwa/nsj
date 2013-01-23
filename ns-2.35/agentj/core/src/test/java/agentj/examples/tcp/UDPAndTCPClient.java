package agentj.examples.tcp;

import agentj.AgentJAgent;
import proto.logging.api.LogProcessor;
import proto.logging.api.Logger;
import util.StringSplitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;


/**
 * A TCP client that gets the server address and port number using a UDP multicast
 * group.  This address is used by clients to connect to and make a TCP connection
 * for passing data from the client to the server. The server then responds with a
 * hello too
 *
 * @author Ian Taylor
 */
public class UDPAndTCPClient extends AgentJAgent {

    MulticastSocket s;
    InetSocketAddress group;
    Socket kkSocket = null;
    String server=null;
    int serverPort=-1;

    public UDPAndTCPClient () {
        this.setJavaDebugLevel(Logger.LogLevel.ERROR);
        this.setNativeDebugLevel(AgentJDebugLevel.error);                
    }

    public void initUDPSocket() {
        group = new InetSocketAddress("228.5.6.7", 555);
        try {
            System.out.println("UDPAndTCPClient, running on node " + InetAddress.getLocalHost().getHostName());
            System.out.println("UDPAndTCPClient, Group Address Allocated = " + group.getHostName());
            s = new MulticastSocket(555);
            s.joinGroup(group, null);

        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

     public void getMyID() {
        byte[] buf = new byte[1000];
        DatagramPacket recv = new DatagramPacket(buf, buf.length);
        try {
            System.out.println("UDPAndTCPClient Calling receive" );
            s.receive(recv);
            String msg = new String(recv.getData());
            System.out.println("UDPAndTCPClient received -------------------------------------- " + msg);
            s.leaveGroup(group,null);

            StringSplitter split = new StringSplitter(msg);

            server = split.at(0);
            serverPort = Integer.parseInt(split.at(1));

            System.out.println("UDPAndTCPClient, server = " + server + " and port = " + serverPort);

        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void openAndConnect() {

        try {
            InetAddress serveraddr= InetAddress.getByName(server);
            kkSocket = new Socket(serveraddr,serverPort);

        } catch (UnknownHostException e) {
            System.err.println("UDPAndTCPClient: Can't get local address");
            System.exit(1);
        } catch (SocketException e) {
            System.out.println("UDPAndTCPClient: Error opening socket");
        }
        catch (IOException ep) {
            System.out.println("Error opening socket");
        }
    }


    public void sendData() {
        System.out.println("SimpleTCPClient: Sending data ...");

        PrintWriter out = null;
        BufferedReader in = null;

        try {
            out = new PrintWriter(kkSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(kkSocket.getInputStream()));

            out.println("Hello server, are you working?");
            out.flush();
            String inputLine;

            inputLine = in.readLine();

            System.out.println("SimpleTCPClient, receing Message !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" );
            System.out.println("--> " + inputLine);

            out.close();
            in.close();
            kkSocket.close();
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to: taranis.");
            System.exit(1);
        }

    }

    public boolean command(String command, String args[]) {
        if (command.equals("initUDP")) {
             initUDPSocket();
            return true;
         }
        else if (command.equals("getUDPMessage")) {
            getMyID();
            return true;
        }
        else if (command.equals("open")) {
            openAndConnect();
            return true;
        }
        else if (command.equals("send")) {
            sendData();
            return true;
        }
        return false;
    }
}
