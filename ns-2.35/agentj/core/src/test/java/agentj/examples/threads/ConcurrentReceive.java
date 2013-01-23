package agentj.examples.threads;

import agentj.AgentJAgent;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.InputStream;

import proto.logging.api.LogProcessor;
import proto.logging.api.Logger;

/**
 * The class extends the simple tcp message exchange by doing a concurrent
 * thread receive using java.util.concurrent Locks and Conditions. The
 * receiver here create a thread that awaits until data is ready. When data
 * arrives from the tcp socket, it releases the lock and the thread reads
 * the data.
 *
 * <p/>
 * Created by scmijt
 * Date: Sep 15, 2008
 * Time: 4:09:05 PM
 */
public class ConcurrentReceive extends AgentJAgent implements Runnable {
    ServerSocket serverSocket = null;
    Thread receiveThread;
    ConcurrentConsumerProducer receiveConsProdExample;
    PrintWriter out;
    Socket clientSocket = null;

    public ConcurrentReceive() {
        this.setJavaDebugLevel(Logger.LogLevel.TRACE);
        this.setNativeDebugLevel(AgentJDebugLevel.max);
        receiveConsProdExample = new ConcurrentConsumerProducer();
    }

    public void open() {
        System.out.println("Running Server ...");

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
        String message=null;

        try {
            message = (String) receiveConsProdExample.get(); // waits for data to arrive
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        System.out.println("ConcurrentReceive:: received following message from client, sending a Reply !!!!!!!");
        System.out.println("---"+ message +  "---");

        out.println("Ah, Hello client, nice to make your acquaintance...");
        out.flush();
        System.out.println("ConcurrentReceive, Finished");

        out.close();

        try {
            clientSocket.close();
            System.out.println("ConcurrentReceive, client socket closed");
            serverSocket.close();
            System.out.println("ConcurrentReceive, serversocket closed");
        } catch (IOException e) {
            System.err.println("Accept failed.");
            System.exit(1);
        }
        System.out.println("ConcurrentReceive, DETECT ME DETECT ME PLEASE !!!!!!!!!!!!!");
    }

    public void acceptConnection() {
        try {
            clientSocket = serverSocket.accept();

            System.out.println("ConcurrentReceive::acceptConnection - connection accepted !!!!!!!");

            out = new PrintWriter(
                    clientSocket.getOutputStream(), true);
            InputStream in =
                    clientSocket.getInputStream();

            System.out.println("ConcurrentReceive:: Awaiting data !!!!!!!");

            System.out.println("ConcurrentReceive, receing Message !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" );

            int read;
            String message="";

            while ((read=in.read()) != -1)
                message+=(char)read;

           receiveConsProdExample.put(message); // to release sync

        } catch (IOException e) {
            System.err.println("Accept failed.");
            System.exit(1);
        }
        System.out.println("ConcurrentReceive::finished ..");

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
