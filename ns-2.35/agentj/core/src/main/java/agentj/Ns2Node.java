package agentj;

import agentj.thread.Controller;
import agentj.nativeimp.util.SocketAndPortManager;

import java.net.InetAddress;

import java.net.UnknownHostException;

/**
 * The Ns2Node represents a node in Ns-2
 *
 * Created by scmijt
 * Date: Sep 19, 2008
 * Time: 1:47:41 PM
 */
public class Ns2Node {

    protected String hostName;
    protected InetAddress myAddress;
    protected AgentJAgent agent;

    protected Controller controller;

    protected SocketAndPortManager socketAndPortManager;

    public Ns2Node(String hostName, Controller controller) {
        this.hostName = hostName;
        this.controller = controller;
        try {
            this.myAddress =  InetAddress.getByName(hostName);
        } catch (UnknownHostException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        socketAndPortManager = new SocketAndPortManager();    // create one per node ...
    }

    public void setAgent(AgentJAgent agent) {
        this.agent = agent;
    }

    public String getHostName() {
         return hostName;
     }

    public InetAddress getHostAddress() {
         return myAddress;
     }

    public AgentJAgent getAgent() {
        return agent;
    }

    public Controller getController() {
        return controller;
    }

    /**
     * Gets the SocketAndPortManager for this node which is used by the sockets
     * in order to find free ports to bind to...
     *
     * @return the SocketAndPortManager for this ns2 node
     */
    public SocketAndPortManager getSocketAndPortManager() {
        return socketAndPortManager;
    }
}
