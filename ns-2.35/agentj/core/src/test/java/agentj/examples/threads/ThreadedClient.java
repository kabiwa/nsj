package agentj.examples.threads;

import agentj.AgentJAgent;

import java.io.IOException;
import java.net.*;

/**
 * Author: iandow
 * Date: Jan 20, 2006
 * Description:
 */
public class ThreadedClient extends AgentJAgent {
    int threads=5;
    int nodeid;
    boolean sockets_enabled;
    DatagramSocket s[];
    String sendToAddress;

    public ThreadedClient() {
    }

    public void init() {
        s = new DatagramSocket[threads];
         this.nodeid = getID();
        this.sockets_enabled = false;
    }

    public boolean command(String command, String args[]) {
        if (command.equals("init")) {
            init();
            return true;
        }
        else if (command.equals("spawnthreads")) {
            spawnthreads();
            return true;
        } else if(command.equals("spawnsockets")) {
            sendToAddress = args[0];

            for (int i=0; i<threads; ++i) {
                try {
                    s[i] = new DatagramSocket(4444+i);
                } catch (SocketException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
            sockets_enabled = true;
            spawnthreads();
            return true;
        }
        return false;
    }

    public void spawnthreads(){
        for (int i=0; i<threads; ++i) {
            (new ClientThread(this,i)).start();
        }
        System.out.println("Threads spawned on node " + nodeid + "\n");
    }


    public void send(String text, String address, int threadNo) {
        int port=5555 + threadNo;

        try {

            System.out.println("Sending to address " + address + ", port number " + port + ": \n" + text);
            byte b[] = (text.getBytes());
            DatagramPacket p =new DatagramPacket(b, b.length,
                    new InetSocketAddress(address, port));
            s[threadNo].send(p);
        } catch (IOException eh) {
            System.out.println("Error Sending Data");
        }
    }

}
