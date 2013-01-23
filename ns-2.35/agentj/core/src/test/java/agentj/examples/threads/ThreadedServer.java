package agentj.examples.threads;

import agentj.AgentJAgent;
import proto.logging.api.Log;
import proto.logging.api.Logger;

import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Author: iandow
 * Date: Jan 20, 2006
 * Description:
 */
public class ThreadedServer extends AgentJAgent {
    static Logger logger = Log.getLogger();

    int threads=5;
    int nodeid;
    boolean sockets_enabled;
    DatagramSocket s[];

    public ThreadedServer() {
    }

    public void init() {
        logger.trace("entering");
        s = new DatagramSocket[threads];
        this.nodeid = getID();
        this.sockets_enabled = false;
        logger.trace("Exiting");
    }

    public boolean command(String command, String args[]) {
        logger.trace("entering");
        if (command.equals("init")) {
            init();
            return true;
        }
        else if (command.equals("spawnthreads")) {
            spawnthreads();
            return true;
        } else if(command.equals("spawnsockets")) {
            sockets_enabled = true;
            for (int i=0; i<threads; ++i) {
                try {
                    s[i] = new DatagramSocket(5555+i);
                } catch (SocketException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
            spawnthreads();
            return true;
        }
        logger.trace("Exiting");
        return false;
    }

    public void spawnthreads(){
        logger.trace("entering");
        for (int i=0; i<threads; ++i) {
            (new ServerThread(this, i)).start();
        }
        System.out.println("Threads spawned on node " + nodeid + "\n");

        logger.trace("Exiting");
    }

}
