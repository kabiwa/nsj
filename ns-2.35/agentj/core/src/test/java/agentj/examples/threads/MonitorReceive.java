package agentj.examples.threads;

import agentj.AgentJAgent;

import java.net.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.InputStream;

import proto.logging.api.LogProcessor;
import proto.logging.api.Logger;

/**
 * Simple receive class that spawns a thread that waits on a monitor for data. When
 * data is received, the monitor is released so the receive thread can print out the message
 * and send a reply.
 */
public class MonitorReceive extends AgentJAgent implements Runnable {
    ServerSocket serverSocket = null;
    Thread receiveThread;
    MonitorExample receiveMonitor;
    PrintWriter out;
    Socket clientSocket = null;

    public MonitorReceive() {
        this.setJavaDebugLevel(Logger.LogLevel.TRACE);
        this.setNativeDebugLevel(AgentJDebugLevel.max);
        receiveMonitor = new MonitorExample(this);
    }

    public void open() {
        System.out.println("MonitorReceive: Running Server ...");

        receiveThread = new Thread(this);
        receiveThread.start();

        try {

            serverSocket = new ServerSocket(1111);

        } catch (IOException e) {
            System.err.println("Could not listen on port: 0");
            System.exit(1);
        }
    }

    public void run() {
        String message = (String)receiveMonitor.receive(); // waits for data to arrive

        System.out.println("MonitorReceive:: received following message from client, sending a Reply !!!!!!!");
        System.out.println("---"+ message +  "---");

        out.println("Ah, Hello client, nice to make your acquaintance...");
        out.flush();
        System.out.println("MonitorReceive, Finishing");

        out.close();

        try {
            clientSocket.close();
            System.out.println("SimpleTCPServer, client socket closed");
            serverSocket.close();
            System.out.println("SimpleTCPServer, serversocket closed");
        } catch (IOException e) {
            System.err.println("Accept failed.");
            System.exit(1);
        }
        System.out.println("MonitorReceive, DETECT ME DETECT ME PLEASE !!!!!!!!!!!!!");
    }

    public void acceptConnection() {
        try {
            clientSocket = serverSocket.accept();

            System.out.println("MonitorReceive::acceptConnection - connection accepted !!!!!!!");

            out = new PrintWriter(
                    clientSocket.getOutputStream(), true);
            InputStream in =
                    clientSocket.getInputStream();

            System.out.println("MonitorReceive:: Awaiting data !!!!!!!");

            System.out.println("MonitorReceive, receing Message !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" );

            int read;
            String message="";

            while ((read=in.read()) != -1)
                message+=(char)read;

            System.out.println("MonitorReceive, calling send");
            receiveMonitor.send(message); // to release sync

        } catch (IOException e) {
            System.err.println("Accept failed.");
            System.exit(1);
        }
        System.out.println("MonitorReceive::finished ..");

    }

    public boolean command(String command, String args[]) {
        if (command.equals("open")) {
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