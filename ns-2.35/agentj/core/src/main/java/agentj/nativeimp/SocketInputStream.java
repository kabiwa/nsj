/*
 * @(#)SocketInputStream.java	1.34 03/12/19
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package agentj.nativeimp;

import agentj.thread.Controller;
import agentj.thread.ReleaseSafeSync;
import agentj.thread.Callback;
import agentj.AgentJVirtualMachine;
import agentj.Ns2Node;
import agentj.AgentJAgent;
import agentj.nativeimp.util.DataPacket;

import java.net.Socket;
import proto.logging.api.Log;
import proto.logging.api.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.nio.channels.FileChannel;

/**
 * This stream extends FileInputStream to implement a
 * SocketInputStream. Note that this class should <b>NOT</b> be
 * public.
 *
 * @version     1.34, 12/19/03
 * @author Jonathan Payne
 * @author Ian Taylor
 * @author Arthur van Hoff
 */
public class SocketInputStream extends InputStream implements Callback {
    // Pointers for native implementation

    // Pointers for native implementation
	public boolean _isDataBufferEmpty; // boolean indicating whether there is
    public long _nsAgentPtr; // the pointer to our JNI C++ NS Agent that this socket lives on
    public long _csocketPtr; // the pointer to the native socket
    // IAN - to synchronise with socket
    int _socketid;

    // End Native Pointers
    Ns2Node ns2Node;

    static Logger logger = Log.getLogger(SocketInputStream.class);

    byte[] localData=null;
    boolean isLocalDataConnection=false;

    Controller cont; // get controller when created
    ReleaseSafeSync sync;
    int offForRead;
    int lengthForRead;


    /**
     * Returns whether this is a local connection
     *
     * @return true if its a local connection
     */
    public boolean isLocalDataConnection() {
        return isLocalDataConnection;
    }

    public int getSocketID() {
        return _socketid;
    }

    public void setSocketID(int socketID) {
        this._socketid = socketID;
    }

    /**
     * Sets whether this connection is local
     */
    public void setLocalDataConnection(boolean localDataConnection) {
        isLocalDataConnection = localDataConnection;
    }

    Object waitForDataEvent() {
        // set up the controller to be the socket listener

        java.lang.Thread cur = java.lang.Thread.currentThread();
        java.lang.Object tobj = cur;
        String className = tobj.getClass().getName();
        logger.trace("SocketID = " + _socketid + ", Thread Paused from Socket.receive in thread " + className);

        logger.debug("SocketID = " + _socketid + " - Waiting for Data on socket " + impl.getMyLocalAddress() + ":" + impl.getMyLocalPort());

        Object data = sync.blockAndGet(); // wait until I get notified before continuing
        logger.trace("Lock being released now for receive on node " + impl.getMyLocalAddress() + "...");
        logger.trace("=====================================================...");
        return data;
    }

    public void localDataArrived(byte[] data) {

        logger.trace("SocketID = " + _socketid + ", data arrived from a local connection: " + new String(data));

        localData=data;

        dataArrived();
    }
    DataPacket sp;
    
    public void dataArrived() {
        AgentJVirtualMachine.setCurrentNode(ns2Node);
        logger.debug("Got TCP Callback from Native Code on " + impl.getSocketLogInfo()
                + ", Port " + impl.getMyLocalPort());

       logger.debug("About to Receive data at : " + impl.getSocketLogInfo()
                + ", Port " + impl.getMyLocalPort());

        int n=lengthForRead;

        byte inputBuffer[] = new byte[n]; // umm - what should I set this to ?

        // set in readline method below
        int off=offForRead;

        try { // do remote read straight away and send that to the monitor
//            n= socketRead0(fd, inputBuffer, off, lengthForRead, impl.getTimeout());
            // don;t use fd so:
            logger.debug("READING REMOTE DATA: Calling Native method from " + impl.getSocketLogInfo());
            n= socketRead0(inputBuffer, off, n, impl.getTimeout());
            logger.debug("Finished Calling Native method from " + impl.getSocketLogInfo());
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally {
            // don't need
           // impl.releaseFD();
        }

        sp = new DataPacket(inputBuffer, off, n);
        cont.executeCallback(this);
    }

    public Object executeCallback() {
        logger.info("Entering");

        logger.trace("Returning to Release sync for Socket receive() call ...");
        logger.trace("=====================================================...");

        return sp;  // returns the data for the controller to execute sync
    }

    /**
     * Returns sync for use by controller
     *
     * @return the sync for this object
     */
    public ReleaseSafeSync getReleaseSafeSync() {
        return sync;
    }
    
    // Sun Code mostly:

    static {
        AgentJAgent.getLock().lock();

         try {
             init();
          } finally {
             AgentJAgent.getLock().unlock();
         }
    }

    private boolean eof;
    private NsSocketImpl impl = null;
    private byte temp[];
    private Socket socket = null;

    /**
     * Creates a new SocketInputStream. Can only be called
     * by a Socket. This method needs to hang on to the owner Socket so
     * that the fd will not be closed.
     * @param impl the implemented socket input stream
     */
    SocketInputStream(NsSocketImpl impl) throws IOException {
        super();
        logger.trace("Creating the input stream");

        // pointer initialisation
         _csocketPtr=impl._csocketPtr;
         _nsAgentPtr =impl._nsAgentPtr; // the pointer to our JNI C++ NS Agent that this socket lives on

        this.impl = impl;
        ns2Node=impl.ns2Node;

        _isDataBufferEmpty=true;
        
        // IAN - to synchronise with socket
        _socketid =impl._socketid;
        cont=impl.controller;

        sync = new ReleaseSafeSync(cont, ReleaseSafeSync.SyncType.SOCKET);

        logger.trace("Done creating the input stream");
    }

    /**
     * Returns the unique {@link java.nio.channels.FileChannel FileChannel}
     * object associated with this file input stream.</p>
     *
     * The <code>getChannel</code> method of <code>SocketInputStream</code>
     * returns <code>null</code> since it is a socket based stream.</p>
     *
     * @return  the file channel associated with this file input stream
     *
     * @since 1.4
     */
    public final FileChannel getChannel() {
        return null;
    }

    /**
     * Reads into an array of bytes at the specified offset using
     * the received socket primitive. 
     * @param b the buffer into which the data is read
     * @param off the start offset of the data
     * @param len the maximum number of bytes read
     * @param timeout the read timeout in ms
     * @return the actual number lof bytes read, -1 is
     *          returned when the end of the stream is reached. 
     * @exception IOException If an I/O error has occurred.
     */
    private native int socketRead0(byte b[], int off, int len,
                                   int timeout)
            throws IOException;

    /**
     * Reads into a byte array data from the socket. 
     * @param b the buffer into which the data is read
     * @return the actual number of bytes read, -1 is
     *          returned when the end of the stream is reached. 
     * @exception IOException If an I/O error has occurred. 
     */
    public int read(byte b[]) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * Reads into a byte array <i>b</i> at offset <i>off</i>, 
     * <i>length</i> bytes of data.
     * @param b the buffer into which the data is read
     * @param off the start offset of the data
     * @param length the maximum number of bytes read
     * @return the actual number of bytes read, -1 is
     *          returned when the end of the stream is reached. 
     * @exception IOException If an I/O error has occurred.
     */
    public int read(byte b[], int off, int length) throws IOException {
        int n;
        logger.trace("SocketInputStream read: Entering - Socket ID = " + _socketid);

        // EOF already encountered
        if (eof) {
            return -1;
        }

        // connection reset
        if (impl.isConnectionReset()) {
            throw new SocketException("Connection reset");
        }

        // bounds check
        if (length <= 0 || off < 0 || off + length > b.length) {
            if (length == 0) {
                return 0;
            }
            throw new ArrayIndexOutOfBoundsException();
        }

        boolean gotReset = false;


        if (!_isDataBufferEmpty) {
            logger.trace("Setting up TCP Read into Buffer from native socket");
            AgentJAgent.getLock().lock();

             try {
                 n= socketRead0(b, off, length, impl.getTimeout());
              } finally {
                 AgentJAgent.getLock().unlock();
             }
            // don't synchronise nor put in a thread - app will
        	// generally just be reading the data multiple times if remote buffer is not empty
        } else {
            logger.trace("No Data left, need to wait for data ...");
        	offForRead=off;
        	lengthForRead=length;

        	DataPacket sp;
        
        	// IAN: enable asynchronous callback for data - only read when ready 8:)
        	sp = (DataPacket)waitForDataEvent();
        
        	logger.debug("Socket ID = " + _socketid + ", Sync unlocked, reading data now ... ");

        	if (isLocalDataConnection()) {
        		logger.debug("SocketInputStream read: retrieving local data now... Socket ID = " + _socketid);

        		System.arraycopy(localData,off,b,0,localData.length); // clean up data

        		return localData.length;
        	}
        	logger.debug("SocketInputStream read: Got remote data, parsing now.... ");

        	n = sp.getLength();

        	byte buf[] = new byte[b.length];
        	// acquire file descriptor and do the read

            logger.debug("SocketInputStream read: copying to read buffer.... ");

        	System.arraycopy(sp.getData(),0,b,off,sp.getLength()); // copy data

            logger.debug("SocketInputStream read: copied ok.... ");

            /*
             * We receive a "connection reset" but there may be bytes still
             * buffered on the socket
             */
             if (gotReset) {
                 impl.setConnectionResetPending();
                 System.arraycopy(sp.getData(),0,b,off,sp.getLength()); // copy data
                 if (n > 0) {
                     return n;
                 }
             }
       }
        
        logger.trace("SocketInputStream, testing for EOF");

        if (n > 0) {
            logger.trace("SocketInputStream read: returning ...");
            return n;
        }


        /*
        * If we get here we are at EOF, the socket has been closed,
        * or the connection has been reset.
        */
        if (impl.isClosedOrPending()) {
            throw new SocketException("Socket closed");
        }
        if (impl.isConnectionResetPending()) {
            impl.setConnectionReset();
        }
        if (impl.isConnectionReset()) {
            throw new SocketException("Connection reset");
        }
        eof = true;
        logger.trace("SocketInputStream EOF, returning");
        return -1;
    }

    /**
     * Reads a single byte from the socket. 
     */
    public int read() throws IOException {
        if (eof) {
            return -1;
        }
        temp = new byte[1];
        int n = read(temp, 0, 1);
        if (n <= 0) {
            return -1;
        }
        return temp[0] & 0xff;
    }

    /**
     * Skips n bytes of input.
     * @param numbytes the number of bytes to skip
     * @return	the actual number of bytes skipped.
     * @exception IOException If an I/O error has occurred.
     */
    public long skip(long numbytes) throws IOException {
        if (numbytes <= 0) {
            return 0;
        }
        long n = numbytes;
        int buflen = (int) Math.min(1024, n);
        byte data[] = new byte[buflen];
        while (n > 0) {
            int r = read(data, 0, (int) Math.min((long) buflen, n));
            if (r < 0) {
                break;
            }
            n -= r;
        }
        return numbytes - n;
    }

    /**
     * Returns the number of bytes that can be read without blocking.
     * @return the number of immediately available bytes
     */
    public int available() throws IOException {
        return impl.available();
    }

    /**
     * Closes the stream.
     */
    private boolean closing = false;
    public void close() throws IOException {
        // Prevent recursion. See BugId 4484411
        if (closing)
            return;
        closing = true;
        impl.close();
        closing = false;
    }

    void setEOF(boolean eof) {
        this.eof = eof;
    }

    /**
     * Overrides finalize.
     */
    protected void finalize() {}

    /**
     * Perform class load-time initializations.
     */
    private native static void init();
}

