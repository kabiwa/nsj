package agentj.examples.tcp;

import agentj.AgentJAgent;
import proto.logging.api.LogProcessor;
import proto.logging.api.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Simple TCOP client taken from the knock knock client in the Java tutorial
 *
 * @author Ian Taylor
 */
public class SimpleTCPClient extends AgentJAgent {

    Socket kkSocket = null;

    public SimpleTCPClient () {
        this.setJavaDebugLevel(Logger.LogLevel.TRACE);
        this.setNativeDebugLevel(AgentJDebugLevel.max);        
    }

    public void openAndConnect() {


        try {
            InetSocketAddress serveraddr= new InetSocketAddress("0", 1111);

            InetSocketAddress saddr = new InetSocketAddress(55);

            kkSocket = new Socket();

            kkSocket.bind(saddr);

            System.out.println("TCP Client: Trying to connect to " + saddr.getHostName() + ", port " + saddr.getPort());

            kkSocket.connect(serveraddr);

            System.out.println("TCP Client: connected ok");

        } catch (UnknownHostException e) {
            System.err.println("SimpleTCPClient: Can't get local address");
            System.exit(1);
        } catch (SocketException e) {
            System.err.println("SimpleTCPClient: Error opening socket");
            System.exit(1);
        }
        catch (IOException ep) {
            System.err.println("SimpleTCPClient: Error opening socket");
            System.exit(1);
        }

        System.out.println("SimpleTCPClient: Socket created and connected to host");
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

            System.out.println("SimpleTCPClient: Data sent, now awaiting reply ...");

            inputLine = in.readLine();

            System.out.println("SimpleTCPClient, receing Message !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" );
            System.out.println("--> " + inputLine);

            out.close();
            
            in.close();
            kkSocket.close();
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to: ");
            System.exit(1);
        }

    }

    public boolean command(String command, String args[]) {
        if (command.equals("open")) {
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
