package agentj.nativeimp.api;

import java.net.InetAddress;

/**
 * The AgentJSocket interface is an interface for agentj sockets (UDP and TCP) that
 * allows monitoring and managing of port allocation upon the binding and address
 * monitoring. Also, when packets arrive at the socket this interface is used to
 * find out if the socket should receive the given packet.
 *
 * Really this is a convenience interface so that UDP and TCP sockets "appear"
 * the same.
 * 
 * <p/>
 * Created by scmijt
 * Date: Nov 3, 2008
 * Time: 3:35:21 PM
 */
public interface AgentJSocket {

    static enum SocketType {UDP, TCP};


    /**
     * Gets the type of socket for this connection.
     *
     * @return the socket type, UDP or TCP
     */
    public SocketType getSocketType();

    /**
     *
     * @return the local port this socket is bound to
     */
    public int getPortBinding();

    /**
     *
     * @return the ocal address binding for this socket, or null if it doesn't 
     * bind to a local address
     */
    public InetAddress getAddressBinding();

    /**
     * Gets the address of this host.
     *
     * @return host address of the socket
     */
    public InetAddress getHostAddress();

    /**
     * Rests the socket for reuse.
     */
    public void reset();

}
