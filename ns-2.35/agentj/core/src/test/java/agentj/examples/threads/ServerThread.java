package agentj.examples.threads;

import java.io.IOException;
import java.net.DatagramPacket;

/**
 * Created by IntelliJ IDEA.
 * User: scmijt
 * Date: Jul 12, 2006
 * Time: 7:50:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServerThread extends java.lang.Thread {
    ThreadedServer threadServer;
    int id;

    public ServerThread(ThreadedServer threadServer, int id) {
        this.threadServer = threadServer;
        this.id=id;
    }

    public void run() {
        System.out.println("Server, Running thread number ---------->  " + id);
        threadServer.logger.trace("entering");
        if (threadServer.sockets_enabled){
            threadServer.logger.trace("Run Sockets");
            try {
                byte b[] = new byte[1000];
                DatagramPacket p = new DatagramPacket(b,b.length);
                System.out.println("Receiving on thread " + id + ", port = " + threadServer.s[id].getLocalPort());
                
                threadServer.s[id].receive(p);
                System.out.println("Receiving in thread " + id + " ----------------------------" + new String(p.getData()));
            } catch (IOException ep) {
                System.out.println("Error opening socket");
            }
        } else {
            threadServer.logger.trace("Running Threads");
            System.out.println("Thread returning from node " + threadServer.nodeid + "\n");
        }
        threadServer.logger.trace("Exiting");
    }

}
