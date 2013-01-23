package agentj.examples.threads;

import agentj.AgentJAgent;
import proto.logging.api.LogProcessor;
import proto.logging.api.Logger;
import util.Logging;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * A simple TCP send to feed the monitor receive example...
 * <p/>
 * Created by scmijt
 * Date: Sep 10, 2008
 * Time: 1:37:04 PM
 */
public class MonitorSend extends AgentJAgent {

    Socket kkSocket = null;

    public MonitorSend() {
        this.setJavaDebugLevel(Logger.LogLevel.TRACE);
        this.setNativeDebugLevel(AgentJAgent.AgentJDebugLevel.max);
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

        } catch (UnknownHostException er) {
            System.err.println("SimpleTCPClient: Can't get local address");
            System.exit(1);
        } catch (SocketException e) {
            System.err.println("SimpleTCPClient: Error opening socket");
            e.printStackTrace();
            System.exit(1);
        }
        catch (IOException ep) {
            System.err.println("SimpleTCPClient: Error opening socket");
            ep.printStackTrace();
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

            Logging.closeLog(); // close log so we can view
        } catch (IOException e) {
            e.printStackTrace();
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
