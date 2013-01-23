package agentj.nativeimp;

import agentj.AgentJVirtualMachine;
import agentj.Ns2Node;
import agentj.AgentJAgent;
import agentj.dns.AgentJNameService;
import agentj.nativeimp.api.AgentJSocket;
import agentj.nativeimp.util.IDFactory;
import agentj.nativeimp.util.SocketAndPortManager;
import agentj.nativeimp.util.DataPacket;
import agentj.thread.Callback;
import agentj.thread.Controller;
import agentj.thread.ReleaseSafeSync;
import proto.logging.api.Log;
import proto.logging.api.Logger;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.*;

/**
 * Concrete datagram and multicast socket implementation base class.
 * Note: This is not a public class, so that applets cannot call
 * into the implementation directly and hence cannot bypass the
 * security checks present in the DatagramSocket and MulticastSocket
 * classes.
 *
 * @author Pavani Diwanji
 */

public class NsDatagramSocketImpl extends DatagramSocketImpl implements Callback, AgentJSocket {
    static Logger logger = Log.getLogger(NsSocketImpl.class);

    public static int AUTO_ALLOCATE_PORT=0xFFFF;

    boolean dataReadyToRead; // when data has already arrived at the socket before a recive.

    // Start Native Pointers

    // Fields accessed by native methods ...
    public boolean _isDataBufferEmpty = true; // boolean indicating whether there is

    // Pointers for native implementation
    //more data to read from remote buffer
    public long _nsAgentPtr; // the pointer to our JNI C++ NS Agent that this socket lives on
    public long _csocketPtr; // the pointer to the native socket
    public int _socketid;

    // End Native Pointers

    Controller controller; // get controller when created
    ReleaseSafeSync sync; // actually used for synchronising a accept call

    boolean localReceive = false;

    // The ns2Node that this socket is on.
    Ns2Node ns2Node;

    // instance of the socketAndPortManager for this ns-2 node ...

    SocketAndPortManager socketAndPortManager;

    boolean receiveInPlace = false;

    DatagramPacket dataBufReady=null;


    /**
     * Reset the socket ...
     */
    public void reset() {
        AgentJAgent.getLock().lock();  // block until condition holds

        try {
            close();
            if (sync.isWaiting()) {
                sync.setWaitForWorkers(false);
                sync.putAndRelease(null);
            }
        } finally {
            AgentJAgent.getLock().unlock();
        }
    }

    public void waitForPacketFromDatagramReceive(Object syncObj) {

        logger.trace("Setting up Socket Listener on Node " + controller.getLocalHost());

        String className = this.getClass().getName();
        logger.trace("XXX: Thread Paused from Datagram socket in thread " + className);

        receiveInPlace = true;

        syncObj = sync.blockAndGet();

        logger.trace("Datagram socket returing data has been received on node " + ns2Node.getHostName() + "...");
        logger.trace("===================================================================...");
        logger.debug("Exiting");
    }


    public void dataArrived() {
        AgentJVirtualMachine.setCurrentNode(ns2Node);

        logger.debug("DatagramSocket: data received at: " + getSocketLogInfo()
                + ", Port " + getLocalPort());

        logger.trace("Returning to Release sync for Datagram receive() call ...");
        logger.trace("=====================================================...");

        logger.trace("About to Read remote data now ...");

        if (receiveInPlace) {
            dataReadyToRead=false;
            controller.executeCallback(this);
        } else { // no receive but read data
            DatagramPacket dataBufReady = new DatagramPacket(new byte[32000], 32000);
            try {
                readRemote(dataBufReady);
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            dataReadyToRead=true;
        }
    }


    /**
     * To implement the callback, when triggered
     */
    public Object executeCallback() {
        logger.trace("Returning Data now to release to application !!!!!!!!!");
        return new Object();  // this will release the sync for the DataGram
    }

    /**
     * Returns sync for use by controller
     *
     * @return the sync for this object
     */
    public ReleaseSafeSync getReleaseSafeSync() {
        return sync;
    }


    private void readRemote(DatagramPacket intoPacket) throws IOException {
        DataPacket data=makeDatapacket(intoPacket);

        AgentJAgent.getLock().lock();  // block until condition holds
        try {
            receive0(data);
        } finally {
            AgentJAgent.getLock().unlock();
        }
        refillDataPacket(data, intoPacket);
    }

    /**
     * Receive the datagram packet.
     */
    protected synchronized void receive(DatagramPacket receiveDataPacket)
            throws IOException {
        AgentJVirtualMachine.setCurrentNode(ns2Node);
        if (!socketOpen) throw new SocketException("Socket Is closed");
        try {
            if ((receiveDataPacket == null) || (receiveDataPacket.getData()==null)) {
                throw new IOException("Datagram Packet passed to receive is null");
            } else if (receiveDataPacket.getLength()>receiveDataPacket.getData().length) {
                throw new IllegalArgumentException();
            }

            logger.debug("DatagramSocket: receive: " + getSocketLogInfo()
                    + ", Port " + getLocalPort());

            if (dataReadyToRead) {
                receiveDataPacket = dataBufReady;
            } else if (!_isDataBufferEmpty) {
                logger.debug("Receiving Native Buffered Data --++ ");
                readRemote(receiveDataPacket);
            } else {
                logger.trace("Receive in place on node " + controller.getLocalHost());
                // IAN: enable asynchronous callback for data - only read when ready 8:)
                waitForPacketFromDatagramReceive(receiveDataPacket);

                logger.debug("SocketInputStream read: Got remote data, parsing now.... ");
                if (!socketOpen) throw new SocketException("Socket Is closed");

                if (receiveDataPacket == null) {
                    logger.error("Datagram packet received is null... returning.... ");
                    logger.error("++++++++++++++++++++++++++++++++++++++++++++++++++");
                }

                logger.debug("Checking Size = " + receiveDataPacket.getLength());

                try {
                    logger.debug("Data buffer is of size..." + receiveDataPacket.getLength());
                    readRemote(receiveDataPacket);
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }

                if (controller.getLocalHost().equals(receiveDataPacket.getAddress().getHostName()))
                    localReceive = true;
                else
                    localReceive = false;

                if (localReceive) {
                    logger.debug("LOCAL SEND DETECTED !!!!!!!!!");
                    logger.debug("Local Receive detected, sender is "
                            + receiveDataPacket.getAddress().getHostName() + " receiver is " + controller.getLocalHost());
//            controller.setLocalReceive(true, getClass().getName() + "on node " + controller.getLocalHost());

                    logger.debug("Dropping Packet ....");
                    return;
                } else {
//            logger.info("NoT LOCAL RECEIVE !!!!!!!!!");
                }

                receiveDataPacket.setPort(receiveDataPacket.getPort());
            }
        } finally {
        }
    }

    public String getSocketLogInfo() {
        return controller.getControllerIDString() + ", SockID " + getSocketID() + ", Addr " + controller.getLocalHost();
    }

    public int getSocketID() {
        return _socketid;
    }

    public void setSocketID(int socketID) {
        this._socketid = socketID;
    }

    public int getPortBinding() {
        return localPort;
    }

    public java.net.InetAddress getAddressBinding() {
        return anyLocalBoundAddr;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public java.net.InetAddress getHostAddress() {
        return ns2Node.getHostAddress();
    }

    public SocketType getSocketType() {
        return SocketType.UDP;
    }

    // JAVA IMPLEMENTATION

    /* timeout value for receive() */
    private int timeout = 0;
    private int trafficClass = 0;
    private boolean connected = false;
    private InetAddress connectedAddress = null;
    private int connectedPort = -1;

    /* cached socket options */
    private boolean loopbackMode = true;

    /*
    * Needed for ipv6 on windows because we need to know
    * if the socket was bound to ::0 or 0.0.0.0, when a caller
    * asks for it. In this case, both sockets are used, but we
    * don't know whether the caller requested ::0 or 0.0.0.0
    * and need to remember it here.
    */
    private java.net.InetAddress anyLocalBoundAddr = null;

    boolean socketOpen;

    /**
     * Load net library into runtime.
     */
    static {
        //   java.security.AccessController.doPrivileged(
        //       new sun.security.action.LoadLibraryAction("net"));
        //   NsDatagramSocketImpl.init();
    }

    /**
     * Creates a datagram socket
     */
    protected synchronized void create() throws SocketException {
        fd=new FileDescriptor();

        socketOpen = true;
        localPort=-1;

        _socketid = IDFactory.getNewID(); // get a new ID for this object upon creation

        logger.debug("Entering");
        // Set the NS2 agent  pointer that this node is created for
        controller = AgentJVirtualMachine.getCurrentNS2NodeController(); // get controller when created
        ns2Node = controller.getNs2node();
        socketAndPortManager = ns2Node.getSocketAndPortManager();

        logger.trace("Got current Controller");
        _nsAgentPtr = controller.getNsAgentPtr();
        logger.trace("Got pointer to agent");
        sync = new ReleaseSafeSync(controller, ReleaseSafeSync.SyncType.SOCKET);

        AgentJAgent.getLock().lock();  // block until condition holds

        try {
            datagramSocketCreate();
        } catch (SocketException e) {
            System.err.println("datagramSocketCreate: Error creating socket ...");
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally {
              AgentJAgent.getLock().unlock();
          }

        logger.debug("Exiting");
    }

    /**
     * Binds a datagram socket to a local port.
     */
    protected void bind(int lport, InetAddress laddr)
            throws SocketException {

        if (lport==0) lport=AUTO_ALLOCATE_PORT;  // don't allow port 0 because that is the
        // default for a socket - rellocate these ...

        if (!socketOpen) throw new SocketException("Socket Is closed");

        if (lport == AUTO_ALLOCATE_PORT)
            lport = socketAndPortManager.getUnusedPortFor(SocketType.UDP);
        else if (socketAndPortManager.isPortUsed(SocketType.UDP, lport))
            throw new SocketException("Port " + lport + " is already in use on Ns2 Node "
                    + ns2Node.getHostName() + "\n" + socketAndPortManager.toString());

        AgentJAgent.getLock().lock();
        try {
            bind0(lport, -1);
        } catch (Exception ee) {
            ee.printStackTrace();
        } finally {
            AgentJAgent.getLock().unlock();
        }

        localPort = lport; // I.T. Addition - I guess Sun do this natively but why ...

        socketAndPortManager.addSocket(this); // add this to the list

        if (laddr.isAnyLocalAddress()) {
            anyLocalBoundAddr = laddr;
        }
    }


    /**
     * Connects a datagram socket to a remote destination. This associates the remote
     * address with the local socket so that datagrams may only be sent to this destination
     * and received from this destination.
     *
     * @param address the remote InetAddress to connect to
     * @param port    the remote port number
     */
    protected void connect(InetAddress address, int port) throws SocketException {
        AgentJAgent.getLock().lock();  // block until condition holds

        try {
            try{
                connect0(AgentJNameService.getNSAddress(address.getHostName()), port);
            } catch (UnknownHostException e) {
                throw new SocketException("Failed to connect - connect to address invalid");
            }
            connectedAddress = address;
            connectedPort = port;
            connected = true;
        } finally {
            AgentJAgent.getLock().unlock();
        }
    }

    /**
     * Disconnects a previously connected socket. Does nothing if the socket was
     * not connected already.
     */
    protected void disconnect() {
        // IANS MODS March 2009
        // disconnect0(connectedAddress.getFamily());
        disconnect0(0);
        connected = false;
        connectedAddress = null;
        connectedPort = -1;
    }



    /**
     * Join the multicast group.
     */
    protected void join(InetAddress inetaddr) throws IOException {
        AgentJAgent.getLock().lock();
        try {
            join(AgentJNameService.getNSAddress(inetaddr.getHostName()));
         } finally {
              AgentJAgent.getLock().unlock();
          }
    }

    /**
     * Leave the multicast group.
     */
    protected void leave(InetAddress inetaddr) throws IOException {
        AgentJAgent.getLock().lock();
        try {
            leave(AgentJNameService.getNSAddress(inetaddr.getHostName()));
         } finally {
              AgentJAgent.getLock().unlock();
          }
    }

    /**
     * Join the multicast group.
     *
     * @param netIf specifies the local interface to receive multicast
     *              datagram packets
     * @throws IllegalArgumentException if mcastaddr is null or is a
     *                                  SocketAddress subclass not supported by this socket
     * @since 1.4
     */

    protected void joinGroup(SocketAddress mcastaddr, NetworkInterface netIf)
            throws IOException {
        if (!socketOpen) throw new SocketException("Socket Is closed");

        if (mcastaddr == null || !(mcastaddr instanceof InetSocketAddress))
            throw new IllegalArgumentException("Unsupported address type");

        AgentJAgent.getLock().lock();
        try {
            join(AgentJNameService.getNSAddress(((InetSocketAddress)mcastaddr).getHostName()));
         } finally {
              AgentJAgent.getLock().unlock();
          }
    }

    /**
     * Leave the multicast group.
     *
     * @param mcastaddr address to leave.
     * @param netIf     specified the local interface to leave the group at
     * @throws IllegalArgumentException if mcastaddr is null or is a
     *                                  SocketAddress subclass not supported by this socket
     * @since 1.4
     */
    protected void leaveGroup(SocketAddress mcastaddr, NetworkInterface netIf)
            throws IOException {
        if (mcastaddr == null || !(mcastaddr instanceof InetSocketAddress))
            throw new IllegalArgumentException("Unsupported address type");
         AgentJAgent.getLock().lock();
        try {
            leave(AgentJNameService.getNSAddress(((InetSocketAddress)mcastaddr).getHostName()));
         } finally {
              AgentJAgent.getLock().unlock();
          }
    }

    /**
     * Close the socket.
     */
    protected void close() {

        socketAndPortManager.removeSocket(this); // add this to the list

        socketOpen = false;
        fd = null;

    }

    protected void finalize() {
        logger.debug("Finalize Being called !!!!");
        AgentJAgent.getLock().lock();
        try {
            close();
            datagramSocketClose();
        } finally {
              AgentJAgent.getLock().unlock();
        }
    }

    /**
     * set a value - since we only support (setting) binary options
     * here, o must be a Boolean
     */

    public void setOption(int optID, Object o) throws SocketException {
        if (!socketOpen)
            throw new SocketException("Socket Closed");

        switch (optID) {
            /* check type safety b4 going native.  These should never
            * fail, since only java.Socket* has access to
            * NsSocketImpl.setOption().
            */
            case SO_TIMEOUT:
                if (o == null || !(o instanceof Integer)) {
                    throw new SocketException("bad argument for SO_TIMEOUT");
                }
                int tmp = ((Integer) o).intValue();
                if (tmp < 0)
                    throw new IllegalArgumentException("timeout < 0");
                timeout = tmp;
                return;
            case IP_TOS:
                if (o == null || !(o instanceof Integer)) {
                    throw new SocketException("bad argument for IP_TOS");
                }
                trafficClass = ((Integer) o).intValue();
                break;
            case SO_REUSEADDR:
                if (o == null || !(o instanceof Boolean)) {
                    throw new SocketException("bad argument for SO_REUSEADDR");
                }
                break;
            case SO_BROADCAST:
                if (o == null || !(o instanceof Boolean)) {
                    throw new SocketException("bad argument for SO_BROADCAST");
                }
                break;
            case SO_BINDADDR:
                throw new SocketException("Cannot re-bind Socket");
            case SO_RCVBUF:
            case SO_SNDBUF:
                if (o == null || !(o instanceof Integer) ||
                        ((Integer) o).intValue() < 0) {
                    throw new SocketException("bad argument for SO_SNDBUF or " +
                            "SO_RCVBUF");
                }
                break;
            case IP_MULTICAST_IF:
                if (o == null || !(o instanceof InetAddress))
                    throw new SocketException("bad argument for IP_MULTICAST_IF");
                break;
            case IP_MULTICAST_IF2:
                if (o == null || !(o instanceof NetworkInterface))
                    throw new SocketException("bad argument for IP_MULTICAST_IF2");
                break;
            case IP_MULTICAST_LOOP:
                if (o == null || !(o instanceof Boolean))
                    throw new SocketException("bad argument for IP_MULTICAST_LOOP");
                break;
            default:
                throw new SocketException("invalid option: " + optID);
        }
        socketSetOption(optID, o);
    }

    /*
     * get option's state - set or not
     */

    public Object getOption(int optID) throws SocketException {
        if (!socketOpen)
            throw new SocketException("Socket Closed");

        Object result;

        switch (optID) {
            case SO_TIMEOUT:
                result = new Integer(timeout);
                break;

            case IP_TOS:
                result = socketGetOption(optID);
                if (((Integer) result).intValue() == -1) {
                    result = new Integer(trafficClass);
                }
                break;

            case SO_BINDADDR:
                if (socketOpen)
                    return this.getHostAddress();
                /* fall through */
            case IP_MULTICAST_IF:
            case IP_MULTICAST_IF2:
            case SO_RCVBUF:
            case SO_SNDBUF:
            case IP_MULTICAST_LOOP:
            case SO_REUSEADDR:
            case SO_BROADCAST:
                result = socketGetOption(optID);
                break;

            default:
                throw new SocketException("invalid option: " + optID);
        }

        return result;
    }


    protected void send(DatagramPacket p) throws IOException {
        logger.debug("Send: About to lock for send now");

        AgentJAgent.getLock().lock();  // block until condition holds

        try {
            DataPacket data=makeDatapacket(p);
            logger.debug("Native Java UPD IMplementation: Sending data now");
            send0(data);
            refillDataPacket(data, p);
        } finally {
            AgentJAgent.getLock().unlock();
        }
    }

    // to refill datagram packet.
    private void refillDataPacket(DataPacket p, DatagramPacket data) throws IOException {
        data.setData(p.getData());
        data.setLength(p.getLength());
        data.setAddress(InetAddress.getByName(AgentJNameService.getIPAddress(p.getAddress())));
        data.setPort(p.getPort());
    }

    // to go in.

    private DataPacket makeDatapacket(DatagramPacket p) throws IOException {
        DataPacket data = new DataPacket(p.getData(),p.getOffset(),p.getLength());
        if (p.getAddress()!=null) {
            data.setAddress(AgentJNameService.getNSAddress(p.getAddress().getHostName()));
            data.setPort(p.getPort());
        }
        return data;
    }

    protected int peek(InetAddress address) throws IOException{
        AgentJAgent.getLock().lock();  // block until condition holds

        int bytes=0;

        try {
            bytes=peek(AgentJNameService.getNSAddress(address.getHostName()));
        } finally {
            AgentJAgent.getLock().unlock();
        }

        return bytes;
    }


    protected int peekData(DatagramPacket p) throws IOException {

        AgentJAgent.getLock().lock();  // block until condition holds

        int bytes=0;

        try {
            DataPacket data=makeDatapacket(p);
            peekData(data);
            refillDataPacket(data, p);
            bytes=data.getData()[0];
        } finally {
            AgentJAgent.getLock().unlock();
        }

        return bytes;

    }

    // Native implementations....

    /**
     * Peek at the packet to see who it is from.
     */
    private native int peek(Long i) throws IOException;

    private native int peekData(DataPacket p) throws IOException;


    private native void join(long inetaddr)
            throws IOException;

    private native void leave(long inetaddr)
            throws IOException;


    private native void datagramSocketClose();

    private native void socketSetOption(int opt, Object val)
            throws SocketException;

    private native Object socketGetOption(int opt) throws SocketException;

    private native void disconnect0(int family);

    /**
     * Perform class load-time initializations.
     */
    private native static void init();

    /**
     * Sends a datagram packet. The packet contains the data and the
     * destination address to send the packet to.
     */
    private native void send0(DataPacket p) throws IOException;

    private native void receive0(DataPacket p)
            throws IOException;

    private native void datagramSocketCreate() throws SocketException;

    private native void bind0(int lport, int laddr)
            throws SocketException;

    private native void connect0(long address, int port) throws SocketException;

   /**
     * Set the TTL (time-to-live) option.
     *
     * @param ttl to be set.
     */
    protected native void setTimeToLive(int ttl) throws IOException;

    /**
     * Get the TTL (time-to-live) option.
     */
    protected native int getTimeToLive() throws IOException;

    /**
     * Set the TTL (time-to-live) option.
     *
     * @param ttl to be set.
     */
    protected native void setTTL(byte ttl) throws IOException;

    /**
     * Get the TTL (time-to-live) option.
     */
    protected native byte getTTL() throws IOException;

}
