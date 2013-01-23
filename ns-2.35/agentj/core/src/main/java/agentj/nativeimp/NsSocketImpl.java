package agentj.nativeimp;

import agentj.AgentJVirtualMachine;
import agentj.Ns2Node;
import agentj.AgentJAgent;
import agentj.dns.AgentJNameService;
import agentj.nativeimp.api.AgentJSocket;
import agentj.nativeimp.util.DataCallback;
import agentj.nativeimp.util.SocketAndPortManager;
import agentj.nativeimp.util.DataPacket;
import agentj.thread.*;
import proto.logging.api.Log;
import proto.logging.api.Logger;
import sun.net.ConnectionResetException;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;

// Changes
import java.net.*;

/**
 * Default Socket Implementation. This implementation does
 * not implement any security checks.
 * Note this class should <b>NOT</b> be public.
 *
 * @author  Steven B. Byrne
 * @author Ian Taylor
 * @version 1.65, 12/19/03
 */
public class NsSocketImpl extends SocketImpl implements DataCallback, Callback, AgentJSocket {

    public static int AUTO_ALLOCATE_PORT=0xFFFF; // maximum - doesn;t accept negative port numbers e.g. -1

//    Socket socket;
  //  ServerSocket serverSocket;

    // End Native Pointers
    public long _nsAgentPtr; // the pointer to our JNI C++ NS Agent that this socket lives on
    public long _csocketPtr; // the pointer to the native socket

    // Used to pass the port and address fields back for the connecting agent after an accept()
    public int _dstaddr; // used in remote scoket accept
    public int _dstport; // used in remote scoket accept
    public int _socketid;
    public boolean _isDataBufferEmpty = true; // boolean indicating whether there is

    // End Native Pointers

    NsSocketImpl localSocketForConnection=null;

    static Hashtable localConnectionIndicator = new Hashtable();
    boolean localConnection=false;

    /** IANs Modifiactions here .... */
    static Logger logger = Log.getLogger(NsSocketImpl.class);

    Controller controller; // get controller when created
    Ns2Node ns2Node;
    SocketAndPortManager socketAndPortManager;
    ReleaseSafeSync sync; // actually used for synchronising a accept call

    boolean closed=true;
    InetAddress myLocalAddress;

    /**
     * Reset the socket ...
     */
    public void reset() {
        // TODO
    }

    public int getSocketID() {
        return _socketid;
    }

    public void setSocketID(int socketID) {
        this._socketid = socketID;
    }

    public String getSocketLogInfo() {
        return controller.getControllerIDString() + ", SockID "  + getSocketID() + ", Addr " + address.getHostName();
    }

    public InetAddress getMyLocalAddress() {
        return myLocalAddress;
    }

    public int getMyLocalPort() {
        return localport;
    }

    public int getDstPort() {
        return port;
    }

    public InetAddress getDstAddress() {
        return address;
    }

    public void setMyLocalAddress(InetAddress myLocalAddress) {
        this.myLocalAddress = myLocalAddress;
    }

    public void setDstAddress(InetAddress dstAddress) {
        this.address = dstAddress;
    }

    public void setDstPort(int dstPort) {
        this.port = dstPort;
    }

    public void setMyLocalPort(int myLocalPort) {
        this.localport = myLocalPort;
    }

    public int getPortBinding() {
        return localport;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public InetAddress getAddressBinding() {
        return myLocalAddress;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public java.net.InetAddress getHostAddress() {
        return ns2Node.getHostAddress();
    }


    public SocketType getSocketType() {
        return SocketType.TCP;
    }

    void waitForServerConnection() {
        // set up the controller to be the socket listener

        Thread cur = Thread.currentThread();
        Object tobj = cur;
        String className = tobj.getClass().getName();
        logger.trace("XXX: Thread Paused from Socket.receive in thread " + className);

        sync.blockAndGet(); // wait until I get notified before continuing
    }

    void waitForSuccessfulClientConnection() {
        // set up the controller to be the socket listener

        Thread cur = Thread.currentThread();
        Object tobj = cur;
        String className = tobj.getClass().getName();
        logger.trace("XXX: Thread Paused from Socket.receive in thread " + className);

        sync.blockAndGet(); // wait until I get notified before continuing
    }

    /**
     * Called on the server to indicate that there is a connection request from a client
     */
    public void connectRequestArrived() {
        logger.trace("Connection Request ON SERVER at node " + controller.getLocalHost() );

        controller.executeCallback(this);
    }

    public Object executeCallback() {

        String host=null;
        host= controller.getLocalHost();

        NsSocketImpl clientSocket = (NsSocketImpl)localConnectionIndicator.get(host);
        localConnectionIndicator.remove(host);

        if (clientSocket!= null) { // This is a local connection
            logger.trace("I have ID " + _socketid + " and the Client Socket has id " + clientSocket.getSocketID());
            try {
                localConnection=true;
                logger.trace("NSSocketImpl: Local Connection, wiring output stream...");
                // Connect the client to the server
                SocketOutputStream clientOutputStream = (SocketOutputStream)clientSocket.getOutputStream();
                SocketInputStream clientInputStream = (SocketInputStream)clientSocket.getInputStream();

                SocketInputStream serverInputStream = (SocketInputStream)localSocketForConnection.getInputStream();
                SocketOutputStream serverOutputStream = (SocketOutputStream)localSocketForConnection.getOutputStream();

                clientOutputStream.setLocalDataConnection(true);
                serverOutputStream.setLocalDataConnection(true);
                clientInputStream.setLocalDataConnection(true);
                serverInputStream.setLocalDataConnection(true);

                logger.trace("NSSocketImpl: Local Connection, wiring input stream...");

                clientOutputStream.setLocalInputStream(serverInputStream);
                serverOutputStream.setLocalInputStream(clientInputStream);

                logger.trace("NSSocketImpl: Local Connection, finished wiring streams...");

                // Reset the thread count just in case - need to fix this ...
                //    controller.resetThreadCount(this);
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        } else {
            localConnection=false;
        }

        return this; // return not used
    }

    /**
     * Returns sync for use by controller
     *
     * @return the sync for this object
     */
    public ReleaseSafeSync getReleaseSafeSync() {
        return sync;
    }

    /**
     * This is called on the client to indicate that the connection made to the CLIENT was successful
     */
    public void connectCompleted() {

        logger.trace("Connection acknoledgement ON CLIENT at node " + controller.getLocalHost() );

        controller.executeCallback(new NsSocketImpl.WaitForConnectComplete(this));

    }

    /**
     * A scoping environment for the connect completed calback
     */
    class WaitForConnectComplete implements Callback {
        NsSocketImpl sockimpl;

        public WaitForConnectComplete(NsSocketImpl sockimpl) {
            this.sockimpl=sockimpl;
        }

        public Object executeCallback() {
            if (sockimpl.localConnection==false) {
                return this;
                // connection to be accepted
            }
            else { // Make sure count is back down as it should be ...
                //    controller.resetThreadCount(this); // reset thread count if things get messed up for
                // local conenctions.
            }
            return null;
        }
        /**
         * Returns sync for use by controller
         *
         * @return the sync for this object
         */
        public ReleaseSafeSync getReleaseSafeSync() {
            return sockimpl.getReleaseSafeSync();
        }
    }

    /**
     * Callback from the native code when data is ready for reading.
     */
    public void dataArrived() {
        logger.trace("NSSocketImpl: Data arrived at the socket ...");
        // pass on to socket input stream respoinsible for the data
        socketInputStream.dataArrived();
    }


    // Original Sun code with minor modifications follows:

    /* instance variable for SO_TIMEOUT */
    int timeout;   // timeout in millisec
    // traffic class
    private int trafficClass;

    private boolean shut_rd = false;
    private boolean shut_wr = false;

    private SocketInputStream socketInputStream = null;
    // IAN Added socketOutputStream  to fix dodgy code
    private SocketOutputStream socketOutputStream =null;

    /* number of threads using the FileDescriptor */
    private int fdUseCount = 0;

    /* lock when increment/decrementing fdUseCount */
    private Object fdLock = new Object();

    /* indicates a close is pending on the file descriptor */
    private boolean closePending = false;

    /* indicates connection reset state */
    private int CONNECTION_NOT_RESET = 0;
    private int CONNECTION_RESET_PENDING = 1;
    private int CONNECTION_RESET = 2;
    private int resetState;
    private Object resetLock = new Object();

    /* second fd, used for ipv6 on windows only.
     * fd1 is used for listeners and for client sockets at initialization
     * until the socket is connected. Up to this point fd always refers
     * to the ipv4 socket and fd1 to the ipv6 socket. After the socket
     * becomes connected, fd always refers to the connected socket
     * (either v4 or v6) and fd1 is closed.
     *
     * For ServerSockets, fd always refers to the v4 listener and
     * fd1 the v6 listener.
     */
    private FileDescriptor fd1;
    /*
     * Needed for ipv6 on windows because we need to know
     * if the socket is bound to ::0 or 0.0.0.0, when a caller
     * asks for it. Otherwise we don't know which socket to ask.
     */
    private InetAddress anyLocalBoundAddr=null;

    /* to prevent starvation when listening on two sockets, this is
     * is used to hold the id of the last socket we accepted on.
     */
    private int lastfd = -1;

    /**
     * Load net library into runtime.
     */
    static {
        java.security.AccessController.doPrivileged(
                new sun.security.action.LoadLibraryAction("net"));
        AgentJAgent.getLock().lock();

        try {
             initProto();
        } finally {
            AgentJAgent.getLock().unlock();
        }
    }

    /**
     * Constructs an empty instance.
     */
    public NsSocketImpl() {
        logger.debug("Entering");
        // Set the NS2 agent  pointer that this node is created for
        controller = AgentJVirtualMachine.getCurrentNS2NodeController(); // get controller when created
        ns2Node = controller.getNs2node();
        socketAndPortManager = ns2Node.getSocketAndPortManager();
        logger.trace("Got current Controller");
        _nsAgentPtr = controller.getNsAgentPtr();
        logger.trace("Got pointer to agent");
        sync= new ReleaseSafeSync(controller, ReleaseSafeSync.SyncType.SOCKET); // actually used for synchronising a accept call
        logger.debug("Exiting");

    }

    /**
     * Creates a socket with a boolean that specifies whether this
     * is a stream socket (true) or an unconnected UDP socket (false).
     */
    protected void create(boolean stream) throws IOException {
        logger.debug("Creating Socket ...." );

        AgentJAgent.getLock().lock();

        try {
            socketCreate(true);
        } finally {
            AgentJAgent.getLock().unlock();
        }

        address = InetAddress.getLocalHost();

        closed=false;

        logger.debug("Created Socket ok with ID " + this._socketid + " and C++ pointer " +this._csocketPtr);
    }


        /**
     * Binds the socket to the specified address of the specified local port.
     * @param address the address
     */
    protected synchronized void bind(InetAddress address, int lport)
            throws IOException
    {

        logger.info("Binding: to address " + address + " on " + this.getSocketLogInfo()
                + ", Port " + lport);

        if (lport==0) lport=AUTO_ALLOCATE_PORT;  // don't allow port 0 because that is the
                                                    // default for a socket - rellocate these ...
        
        if (lport== AUTO_ALLOCATE_PORT)
            lport=socketAndPortManager.getUnusedPortFor(SocketType.TCP);
        else if (socketAndPortManager.isPortUsed(SocketType.TCP, lport)) {
            throw new IOException("Port " + lport + " is already in use on Ns2 Node "
                    + ns2Node.getHostName() + "\n" + socketAndPortManager.toString());
        }
        // Note that in NS-2 - the address field is ignored anyway.


        AgentJAgent.getLock().lock();

        try {
            socketBind(lport);
        } finally {
            AgentJAgent.getLock().unlock();
        }


        localport=lport;
        myLocalAddress=address;
        socketAndPortManager.addSocket(this);
    }

    /**
     * Listens, for a specified amount of time, for connections.
     * @param count the amount of time to listen for connections
     */
    protected synchronized void listen(int count) throws IOException {
        logger.debug("SocketListening on socket : " + getSocketLogInfo()
                + ", Port " + localport);

        AgentJAgent.getLock().lock();

        try {
            socketListen(count);
        } finally {
            AgentJAgent.getLock().unlock();
        }

    }


    protected void connect(String s, int port) throws IOException {
        connect(InetAddress.getByName(s), port);
    }

    protected void connect(InetAddress inetAddress, int port) throws IOException {
        connect(new InetSocketAddress(inetAddress, port), 0);
    }

    protected void connect(SocketAddress socketAddress, int timeout) throws IOException {
        InetSocketAddress inetSockAddress=null;
        if (socketAddress instanceof InetSocketAddress) {
            inetSockAddress = ((InetSocketAddress)socketAddress);
            address = inetSockAddress.getAddress();
            port=inetSockAddress.getPort();
        } else
            throw new IOException("Does not have the correct address type " + socketAddress.getClass().getName());

        setDstAddress(address);
        setDstPort(port);

        String myAddress = controller.getLocalHost();

        localConnection=false;

        if (address.getHostName().equals(myAddress))
            localConnection=true;

        logger.trace("Client = " + myAddress + ", connecting to " +
                address.getHostName());

        if (localConnection) {
            localConnectionIndicator.put(myAddress, this);
        } else{
            localConnection=false;
        }

        logger.debug("Connecting: " + this.getSocketLogInfo()
                + ", Port " + localport
                + ", DAddr " +  address.getHostName() + ", DPort " + port);


        // add a item to the list to tell the server that this is a local connection

        AgentJAgent.getLock().lock();

        try {
            socketConnect(AgentJNameService.getNSAddress(address.getHostName()),
                port, timeout);
        } finally {
            AgentJAgent.getLock().unlock();
        }


        if (!localConnection) // don;t need this as we ARE waiting. Its a blocking call.
            this.waitForSuccessfulClientConnection();
        else {
            logger.error("ERROR - local connection - debug this ...");
        }
    }


       /**
     * Accepts connections.
     * @param s the connection
     */
    protected synchronized void accept(SocketImpl s) throws IOException {
        try {

            ((NsSocketImpl)s).create(true);

            // Used to keep track fo the socket used for this connection that deals with data
            // rather than the server socket that accepts it;
            localSocketForConnection=(NsSocketImpl)s;

            localSocketForConnection.setMyLocalAddress(this.getMyLocalAddress());
            localSocketForConnection.setMyLocalPort(this.getMyLocalPort());

            waitForServerConnection(); // wait for notification of connection before accepting it
            logger.trace("SocketID = " + _socketid + ", Notification of Accepted Connection Received");

            // For local connections this still has to be called in order to finalise the TCP
            // acks for the connection, otherwise we get retries ...
            // But for local connections, we stay in the Java world rom now on.

            logger.debug("Accept on socket : " + localSocketForConnection.getSocketLogInfo() +
                    " for server at " + this.getMyLocalAddress() + ", Port " + this.getMyLocalPort());

            if (s==null) {
                System.out.println("Socket given to accept is null - how????");
                System.exit(0);
            }

        AgentJAgent.getLock().lock();

        try {
            socketAccept(s);
        } finally {
            AgentJAgent.getLock().unlock();
        }


            localSocketForConnection.setDstAddress(InetAddress.getByName(String.valueOf(localSocketForConnection._dstaddr)));
            localSocketForConnection.setDstPort(localSocketForConnection._dstport);

            logger.debug("Node requested connection was " + localSocketForConnection.getDstAddress().getHostName()
                    + ", Port " + localSocketForConnection.getDstPort());

        } finally {
        }
    }


    public void setOption(int opt, Object val) throws SocketException {
        if (isClosedOrPending()) {
            throw new SocketException("Socket Closed");
        }
        boolean on = true;
        switch (opt) {
            /* check type safety b4 going native.  These should never
            * fail, since only java.Socket* has access to
            * NsSocketImpl.setOption().
            */
            case SO_LINGER:
                if (val == null || (!(val instanceof Integer) && !(val instanceof Boolean)))
                    throw new SocketException("Bad parameter for option");
                if (val instanceof Boolean) {
                    /* true only if disabling - enabling should be Integer */
                    on = false;
                }
                break;
            case SO_TIMEOUT:
                if (val == null || (!(val instanceof Integer)))
                    throw new SocketException("Bad parameter for SO_TIMEOUT");
                int tmp = ((Integer) val).intValue();
                if (tmp < 0)
                    throw new IllegalArgumentException("timeout < 0");
                timeout = tmp;
                break;
            case IP_TOS:
                if (val == null || !(val instanceof Integer)) {
                    throw new SocketException("bad argument for IP_TOS");
                }
                trafficClass = ((Integer)val).intValue();
                break;
            case SO_BINDADDR:
                throw new SocketException("Cannot re-bind socket");
            case TCP_NODELAY:
                if (val == null || !(val instanceof Boolean))
                    throw new SocketException("bad parameter for TCP_NODELAY");
                on = ((Boolean)val).booleanValue();
                break;
            case SO_SNDBUF:
            case SO_RCVBUF:
                if (val == null || !(val instanceof Integer) ||
                        !(((Integer)val).intValue() > 0)) {
                    throw new SocketException("bad parameter for SO_SNDBUF " +
                            "or SO_RCVBUF");
                }
                break;
            case SO_KEEPALIVE:
                if (val == null || !(val instanceof Boolean))
                    throw new SocketException("bad parameter for SO_KEEPALIVE");
                on = ((Boolean)val).booleanValue();
                break;
            case SO_OOBINLINE:
                if (val == null || !(val instanceof Boolean))
                    throw new SocketException("bad parameter for SO_OOBINLINE");
                on = ((Boolean)val).booleanValue();
                break;
            case SO_REUSEADDR:
                if (val == null || !(val instanceof Boolean))
                    throw new SocketException("bad parameter for SO_REUSEADDR");
                on = ((Boolean)val).booleanValue();
                break;
            default:
                throw new SocketException("unrecognized TCP option: " + opt);
        }
        AgentJAgent.getLock().lock();

        try {
          socketSetOption(opt, on, val);
        } finally {
            AgentJAgent.getLock().unlock();
        }
    }
    public Object getOption(int opt) throws SocketException {
        if (isClosedOrPending()) {
            throw new SocketException("Socket Closed");
        }
        if (opt == SO_TIMEOUT) {
            return new Integer(timeout);
        }
        int ret = 0;
        /*
        * The native socketGetOptionJava() knows about 3 options.
        * The 32 bit value it returns will be interpreted according
        * to what we're asking.  A return of -1 means it understands
        * the option but its turned off.  It will raise a SocketException
        * if "opt" isn't one it understands.
        */

        switch (opt) {
            case TCP_NODELAY:
                ret = socketGetOptionJava(opt, null);
                return Boolean.valueOf(ret != -1);
            case SO_OOBINLINE:
                ret = socketGetOptionJava(opt, null);
                return Boolean.valueOf(ret != -1);
            case SO_LINGER:
                ret = socketGetOptionJava(opt, null);
                return (ret == -1) ? Boolean.FALSE: (Object)(new Integer(ret));
            case SO_REUSEADDR:
                ret = socketGetOptionJava(opt, null);
                return Boolean.valueOf(ret != -1);
            case SO_BINDADDR:
                if (fd != null && fd1 != null ) {
                    /* must be unbound or else bound to anyLocal */
                    return anyLocalBoundAddr;
                }
                //   InetAddressContainer in = new InetAddressContainer();
                DataPacket dp = new DataPacket();
                ret = socketGetOptionJava(opt,dp ); // need to fix
                return AgentJNameService.getIPAddress(dp.getAddress());
            case SO_SNDBUF:
            case SO_RCVBUF:
                ret = socketGetOptionJava(opt, null);
                return new Integer(ret);
            case IP_TOS:
                ret = socketGetOptionJava(opt, null);
                if (ret == -1) { // ipv6 tos
                    return new Integer(trafficClass);
                } else {
                    return new Integer(ret);
                }
            case SO_KEEPALIVE:
                ret = socketGetOptionJava(opt, null);
                return Boolean.valueOf(ret != -1);
            // should never get here
            default:
                return null;
        }
    }


    public FileDescriptor getFileDescriptor() {
        logger.fatal("Nio Called my getFileDescriptor in NsSocketImpl ...");
        return null;
    }

    /**
     * Gets an InputStream for this socket.
     */
    protected synchronized InputStream getInputStream() throws IOException {
        logger.trace("entered");

        if (isClosedOrPending()) {
            throw new IOException("Socket Closed");
        }
        if (shut_rd) {
            throw new IOException("Socket input is shutdown");
        }

        if (socketInputStream == null) {
            logger.trace("Creating a a new input stream");
            socketInputStream = new SocketInputStream(this);
            socketInputStream.setSocketID(_socketid);
        }

        return socketInputStream;
    }

    public void setInputStream(SocketInputStream in) {
        socketInputStream = in;
    }



    /**
     * Gets an OutputStream for this socket.
     */
    protected synchronized OutputStream getOutputStream() throws IOException {
        if (isClosedOrPending()) {
            throw new IOException("Socket Closed");
        }
        if (shut_wr) {
            throw new IOException("Socket output is shutdown");
        }

        if (socketOutputStream == null) {
            socketOutputStream  = new SocketOutputStream(this);
            socketOutputStream.setSocketID(_socketid);
        }

        logger.trace("Returning output stream + " + socketOutputStream);

        return socketOutputStream ;
    }

    /**
     * Returns the number of bytes that can be read without blocking.
     */
    protected synchronized int available() throws IOException {
        if (isClosedOrPending()) {
            throw new IOException("Stream closed.");
        }

        /*
        * If connection has been reset then return 0 to indicate
        * there are no buffered bytes.
        */
        if (isConnectionReset()) {
            return 0;
        }

        /*
        * If no bytes available and we were previously notified
        * of a connection reset then we move to the reset state.
        *
        * If are notified of a connection reset then check
        * again if there are bytes buffered on the socket.
        */
        AgentJAgent.getLock().lock();
        int n = 0;

        try {

        try {
            n = socketAvailable();
            if (n == 0 && isConnectionResetPending()) {
                setConnectionReset();
            }
        } catch (ConnectionResetException exc1) {
            setConnectionResetPending();
            try {
                n = socketAvailable();
                if (n == 0) {
                    setConnectionReset();
                }
            } catch (ConnectionResetException exc2) {
            }
        }
        } finally {
            AgentJAgent.getLock().unlock();
        }
        return n;
    }

    /**
     * Closes the socket.
     */
    protected void close() throws IOException {
                    try {
                        socketPreClose();
                    } finally {
                        socketClose();
                    }
    }


    /**
     * Shutdown read-half of the socket connection;
     */
    protected void shutdownInput() throws IOException {
        AgentJAgent.getLock().lock();

        try {
            if (fd != null) {
            socketShutdown(SHUT_RD);
            if (socketInputStream != null) {
                socketInputStream.setEOF(true);
            }
            shut_rd = true;
        }
        } finally {
            AgentJAgent.getLock().unlock();
        }

    }

    /**
     * Shutdown write-half of the socket connection;
     */
    protected void shutdownOutput() throws IOException {
        AgentJAgent.getLock().lock();

        try {
            if (fd != null) {
            socketShutdown(SHUT_WR);
            shut_wr = true;
        }
        } finally {
            AgentJAgent.getLock().unlock();
        }

    }

    protected boolean supportsUrgentData () {
        return true;
    }

    protected void sendUrgentData (int data) throws IOException {
        if (fd == null) {
            throw new IOException("Socket Closed");
        }

        AgentJAgent.getLock().lock();

        try {
            socketSendUrgentData (data);
        } finally {
            AgentJAgent.getLock().unlock();
        }
    }

    /**
     * Cleans up if the user forgets to close it.
     */
    protected void finalize() throws IOException {
        close();
    }




    public boolean isConnectionReset() {
        synchronized (resetLock) {
            return (resetState == CONNECTION_RESET);
        }
    }

    public boolean isConnectionResetPending() {
        synchronized (resetLock) {
            return (resetState == CONNECTION_RESET_PENDING);
        }
    }

    public void setConnectionReset() {
        synchronized (resetLock) {
            resetState = CONNECTION_RESET;
        }
    }

    public void setConnectionResetPending() {
        synchronized (resetLock) {
            if (resetState == CONNECTION_NOT_RESET) {
                resetState = CONNECTION_RESET_PENDING;
            }
        }

    }

    /*
     * Return true if already closed or close is pending
     */
    public boolean isClosedOrPending() {
        return closed;   
    }

    /*
     * Return the current value of SO_TIMEOUT
     */
    public int getTimeout() {
        return timeout;
    }

    /*
     * "Pre-close" a socket by dup'ing the file descriptor - this enables
     * the socket to be closed without releasing the file descriptor.
     */
    private void socketPreClose() throws IOException {
        logger.trace("Entering");
        AgentJAgent.getLock().lock();

        try {
            socketClose0(true);
        } finally {
            AgentJAgent.getLock().unlock();
        }
    }

    /*
     * Close the socket (and release the file descriptor).
     */
    private void socketClose() throws IOException {
        closed=true;
        // Don't  need close in NS-2 imp - the pre close method does this instead.
        // This is called after a socket write which doesn't work,
        logger.trace("Entering");
        AgentJAgent.getLock().lock();

        try {
            socketClose0(false);
        } finally {
            AgentJAgent.getLock().unlock();
        }
    }

    // this is confusing - isServer is strange - both serversocket and socket create using true
    // this is always true !! so I said that ...

    //private native void socketCreate(boolean isServer) throws IOException;

    private native void socketCreate(boolean alwaysTrue) throws IOException;


    private native void socketConnect(long address, int port, int timeout)
            throws IOException;
    private native void socketBind(int port)
            throws IOException;
    private native void socketListen(int count)
            throws IOException;
    private native void socketAccept(SocketImpl s)
            throws IOException;
    private native int socketAvailable()
            throws IOException;
    private native void socketClose0(boolean useDeferredClose)
            throws IOException;
    private native void socketShutdown(int howto)
            throws IOException;
    private static native void initProto();

    private native void socketSetOption(int cmd, boolean on, Object value)
            throws SocketException;

    // this (iaContainerObj) was an InternetContainer Object
    //    static class InetAddressContainer {
    //        InetAddress addr;
    // need to look into these more...
    //  }

    private int socketGetOptionJava(int opt, Object iaContainerObj) throws SocketException {
        AgentJAgent.getLock().lock();

        int ret=-1;
        
        try {
            ret = socketGetOption(opt, iaContainerObj);
       } finally {
            AgentJAgent.getLock().unlock();
        }
        return ret;
    }

    private native int socketGetOption(int opt, Object iaContainerObj) throws SocketException;

    private native void socketSendUrgentData(int data)
            throws IOException;

    public final static int SHUT_RD = 0;
    public final static int SHUT_WR = 1;

}
