/*
 * @(#)SocketOutputStream.java	1.30 03/12/19
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package agentj.nativeimp;

import agentj.thread.AgentJEventObjectInterface;
import agentj.thread.AgentJEventQueue;
import agentj.nativeimp.util.DataPacket;
import agentj.AgentJAgent;

import java.net.Socket;
import proto.logging.api.Log;
import proto.logging.api.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.nio.channels.FileChannel;

/**
 * This stream extends FileOutputStream to implement a
 * SocketOutputStream. Note that this class should <b>NOT</b> be
 * public.
 *
 * @version     1.30, 12/19/03
 * @author 	Jonathan Payne
 * @author	Arthur van Hoff
 * @author Ian Taylor
 */
public class SocketOutputStream extends OutputStream implements AgentJEventObjectInterface {

    // Pointers for native implementation

    public long _nsAgentPtr; // the pointer to our JNI C++ NS Agent that this socket lives on
    public long _csocketPtr; // the pointer to the native socket
    int _socketid =-1;

    // End Native Pointers
    
    static Logger logger = Log.getLogger(SocketOutputStream.class);
    
    /**
     * By default, a socket output stream does not send the data.  It adds it to the event queue
     * for sending later - after this Ns-2 node has had its time slice.
     */
    boolean sendEnabled =false;

    boolean isLocalDataConnection=false;
    SocketInputStream localInputStream=null;

    /**
     * Returns whether this is a local connection
     *
     * @return true if its a local connection
     */
    public boolean isLocalDataConnection() {
        return isLocalDataConnection;
    }

    /**
     * Sets whether this connection is local
     */
    public void setLocalDataConnection(boolean localDataConnection) {
        isLocalDataConnection = localDataConnection;
    }

    public int getSocketID() {
        return _socketid;
    }

    public void setSocketID(int socketID) {
        this._socketid = socketID;
    }

    /**
     * Sets the local input stream if the connection is local
     *
     * @param localInputStream
     */
    public void setLocalInputStream(SocketInputStream localInputStream) {
        this.localInputStream = localInputStream;
    }

    static {
        AgentJAgent.getLock().lock();

         try {
             init();
          } finally {
             AgentJAgent.getLock().unlock();
         }
    }

    private NsSocketImpl impl = null;
    private byte temp[] = new byte[1];
    private Socket socket = null;

    /**
     * Creates a new SocketOutputStream. Can only be called
     * by a Socket. This method needs to hang on to the owner Socket so
     * that the fd will not be closed.
     * @param impl the socket output stream inplemented
     */
    SocketOutputStream(NsSocketImpl impl) throws IOException {
        // pointer initialisation

        _csocketPtr=impl._csocketPtr;
        _nsAgentPtr=impl._nsAgentPtr;

        this.impl = impl;

        // IAN - to synchronise with socket
        _socketid=impl._socketid;
    }

    /**
     * Returns the unique {@link java.nio.channels.FileChannel FileChannel}
     * object associated with this file output stream. </p>
     *
     * The <code>getChannel</code> method of <code>SocketOutputStream</code>
     * returns <code>null</code> since it is a socket based stream.</p>
     *
     * @return  the file channel associated with this file output stream
     *
     * @since 1.4
     */
    public final FileChannel getChannel() {
        return null;
    }

    /**
     * Writes to the socket.
     * @param b the data to be written
     * @param off the start offset in the data
     * @param len the number of bytes that are written
     * @exception IOException If an I/O error has occurred.
     */
    private native void socketWrite0(byte[] b, int off, int len) throws IOException;


    /**
     * Interface from the Event Queue to send data
     *
     * @see agentj.thread.AgentJEventQueue
     * @param payload
     * @throws IOException
     */
    public void doSend(Object payload) throws IOException {
        DataPacket data = (DataPacket)payload;
        sendEnabled =true;
        socketWrite(data.getData(), data.getOffset(), data.getLength());
        sendEnabled =false;
    }

    /**
     * Writes to the socket with appropriate locking of the 
     * FileDescriptor.
     * @param b the data to be written
     * @param off the start offset in the data
     * @param len the number of bytes that are written
     * @exception IOException If an I/O error has occurred.
     */
    private void socketWrite(byte b[], int off, int len) throws IOException {

        logger.debug("Sending Data from : " + impl.getSocketLogInfo()
                        + ", Port " + impl.getMyLocalPort()
                + " to DAddr " +  impl.getDstAddress() + ", DPort " + impl.getDstPort());

        if ((!sendEnabled) && (isLocalDataConnection())) { // if not sending pop this send on the queue
            AgentJEventQueue.pushEvent(this, new DataPacket(b,off,len));
            return;
        }

        if (len <= 0 || off < 0 || off + len > b.length) {
            if (len == 0) {
                return;
            }
            throw new ArrayIndexOutOfBoundsException();
        }

        try {
            if (isLocalDataConnection()) {   // If its a local connection just pass it along
                logger.trace("SocketID: " + _socketid + " Sending to local connection " + len + " bytes");
                if ((off==0) && (len==b.length)) {
                    logger.trace("Calling LocalDataArrived method in inputstream");
                    localInputStream.localDataArrived(b);
                } else {
                    logger.trace("Reshuffling Data, off = " + off + ", len = " + len + " and b.length = " + b.length);
                    byte[] bnew = new byte[len];
                    for (int i=0; i<len; ++i)
                        bnew[i] = b[off+i];
                    logger.trace("Calling LocalDataArrived method , message = " + new String(bnew));
                    localInputStream.localDataArrived(bnew);

                }
                logger.trace("Called LocalDataArrived method");
            }
            else { // write as normal
                logger.trace("SocketID: " + _socketid + " Sending " + len + " bytes");
                AgentJAgent.getLock().lock();

                 try {
                     socketWrite0(b, off, len);
                 } finally {
                     AgentJAgent.getLock().unlock();
                 }
            }
        } catch (SocketException se) {
            if (se instanceof sun.net.ConnectionResetException) {
                impl.setConnectionResetPending();
                se = new SocketException("Connection reset");
            }
            if (impl.isClosedOrPending()) {
                throw new SocketException("Socket closed");
            } else {
                throw se;
            }
        } finally {
        }

        logger.trace("done");
    }

    /**
     * Writes a byte to the socket. 
     * @param b the data to be written
     * @exception IOException If an I/O error has occurred. 
     */
    public void write(int b) throws IOException {
        temp[0] = (byte)b;
        socketWrite(temp, 0, 1);
    }

    /**
     * Writes the contents of the buffer <i>b</i> to the socket.
     * @param b the data to be written
     * @exception SocketException If an I/O error has occurred. 
     */
    public void write(byte b[]) throws IOException {
        socketWrite(b, 0, b.length);
    }

    /**
     * Writes <i>length</i> bytes from buffer <i>b</i> starting at 
     * offset <i>len</i>.
     * @param b the data to be written
     * @param off the start offset in the data
     * @param len the number of bytes that are written
     * @exception SocketException If an I/O error has occurred.
     */
    public void write(byte b[], int off, int len) throws IOException {
        socketWrite(b, off, len);
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
        if (socket != null) {
            if (!socket.isClosed())
                socket.close();
        } else
            impl.close();
        closing = false;
    }

    /**
     * Overrides finalize, the fd is closed by the Socket.
     */
    protected void finalize() {}

    /**
     * Perform class load-time initializations.
     */
    private native static void init();

}
