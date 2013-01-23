package agentj.nativeimp.util;

import agentj.nativeimp.api.AgentJSocket;

import java.net.InetAddress;

import java.util.ArrayList;

/**
 * SocketAndPortManager is a class that keeps track of the sockets ad ports created on a
 * particular ns2 node.
 *
 * <p/>
 * Created by scmijt
 * Date: Nov 3, 2008
 * Time: 3:25:12 PM
 */
public class SocketAndPortManager {
    // we start from an unlikely range to reduce the chance of clashes later form applications
    private static int ALLOCATE_PORTS_STARTING_FROM=9999; // start at 9999 and allocate from thereafter
    private int udpPortToTryNext =ALLOCATE_PORTS_STARTING_FROM;
    private int tcpPortToTryNext =ALLOCATE_PORTS_STARTING_FROM;

    ArrayList<AgentJSocket> sockets = new ArrayList<AgentJSocket>();

    public void addSocket(AgentJSocket socket) {
        sockets.add(socket);
        }

    public void removeSocket(AgentJSocket socket) {
        sockets.remove(socket);
        }

    /**
     * Gets next available port that a socket can use ...
     *
     * @return
     */
    public int getUnusedPortFor(AgentJSocket.SocketType socketType) {
        boolean freePort=false;

            if (socketType== AgentJSocket.SocketType.UDP) {
                while (!freePort) {
                if (isPortUsed(socketType, udpPortToTryNext))
                    --udpPortToTryNext;
                else
                    freePort=true;
                return udpPortToTryNext;
                }
            } else {
                while (!freePort) {
                 if (isPortUsed(socketType, tcpPortToTryNext))
                     --tcpPortToTryNext;
                 else
                     freePort=true;
                 return tcpPortToTryNext;
                }
            }

        return -1;

    }

    /**
     * Checks if this port is in use
     *
     * @param port
     * @return true if the port is used, false, otherwise.
     */
    public boolean isPortUsed(AgentJSocket.SocketType socketType, int port) {
        for (AgentJSocket socket: sockets) {
            if ((socket.getSocketType()==socketType) && (socket.getPortBinding()==port))
                return true;
        }
        return false;
    }

    /**
     * Lists all sockets on this node - useful for debugging.
     *
     * @return String representation of sockets
     */
    public String toString() {
        StringBuffer socketString=new StringBuffer();

        socketString.append("Current Sockets and Port Bindings for Debugging\n");
        socketString.append("===============================================\n");
        socketString.append("\n");
        
        for (AgentJSocket socket: sockets) {
            socketString.append("Host Address " + socket.getHostAddress());
            socketString.append(", Port Number = " + socket.getPortBinding());
            socketString.append(", Socket Type = " + ((socket.getSocketType()== AgentJSocket.SocketType.TCP)?"TCP":"UDP") + "\n");
        }

        return socketString.toString();
    }

    // convenience methods
    
    public static boolean isSocketBoundtoPort(AgentJSocket socket, int port) {
        return (socket.getPortBinding()==port);
    }

    public static boolean isSocketBoundtoAddress(AgentJSocket socket, InetAddress address) {
        return (socket.getAddressBinding()==address);
    }
}
