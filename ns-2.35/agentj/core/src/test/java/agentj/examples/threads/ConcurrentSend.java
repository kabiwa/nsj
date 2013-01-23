package agentj.examples.threads;

import proto.logging.api.LogProcessor;
import proto.logging.api.Logger;

import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.net.SocketException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import agentj.AgentJAgent;
import util.Logging;

/**
 * The ... class ...
 * <p/>
 * Created by scmijt
 * Date: Sep 15, 2008
 * Time: 4:08:47 PM
 */
public class ConcurrentSend extends AgentJAgent {
    Socket kkSocket = null;

    public ConcurrentSend() {
        this.setJavaDebugLevel(Logger.LogLevel.TRACE);
        this.setNativeDebugLevel(AgentJAgent.AgentJDebugLevel.max);
    }

    public void openAndConnect() {


        try {
            InetSocketAddress serveraddr= new InetSocketAddress("0", 1111);

            InetSocketAddress saddr = new InetSocketAddress(55);

            kkSocket = new Socket();

            kkSocket.bind(saddr);

            System.out.println("ConcurrentSend: Trying to connect to " + saddr.getHostName() + ", port " + saddr.getPort());

            kkSocket.connect(serveraddr);

            System.out.println("ConcurrentSend: connected ok");

        } catch (UnknownHostException e) {
            System.err.println("ConcurrentSend: Can't get local address");
            System.exit(1);
        } catch (SocketException e) {
            System.err.println("ConcurrentSend: Error opening socket");
            System.exit(1);
        }
        catch (IOException ep) {
            System.err.println("ConcurrentSend: Error opening socket");
            System.exit(1);
        }

        System.out.println("ConcurrentSend: Socket created and connected to host");
    }


    public void sendData() {
        System.out.println("ConcurrentSend: Sending data ...");

        PrintWriter out = null;
        BufferedReader in = null;

        try {
            out = new PrintWriter(kkSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(kkSocket.getInputStream()));

            out.println("Hello Concurrent server, are you working?");
            out.flush();
            String inputLine;

            System.out.println("ConcurrentSend: Data sent, now awaiting reply ...");

            inputLine = in.readLine();

            System.out.println("ConcurrentSend, receing Message !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" );
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
