#ifndef JAVMSocketImp_h
#define JAVMSocketImp_h

#include "SocketWrapper.h"
#include "nsTCPProtoSocketAgent.h"

/*
 *  JAVMSocketImp.h
 *  
 *  AgentJ NativeCode: This is the interface to protolib socket for both TCP and datagram sockets.
 * The methods contain common implementations and are accessed by the JNI hooks implemented in
 * JAVMTcpSocket.cpp and JAVMDatagramSocket.cpp.  The UDP and TCP specific stuff is implemented in 
 * those classes
 *
 *  Created by Ian Taylor on 29/12/2007.
 *
 */

//
//  The socketType is either UDP or TCP depending on which socket you want to create.	
void socketCreate(JNIEnv *env, jobject javaobj, SocketWrapper::SocketType socketType);

// Parameters: InetAddress address, int port
// throws IOException;

void socketBind(JNIEnv *env, jobject javaobj, jint port);

// Parameters: InetAddress address, int port, int timeout
// throws IOException;

void socketConnect(JNIEnv *env, jobject javaobj, jint address, jint port, jint timeout);  
  
// Parameters: int count
// count is the listen backlog length
// throws IOException;

void socketListen(JNIEnv *env, jobject javaobj);

// Parameters:  SocketImpl s
// So, look up the id for the SocketImpl provided and use this to pass this socket
// along to the accpet method.
void socketAccept(JNIEnv *env, jobject javaobj, jobject nssocketimpl);

void socketClose(JNIEnv *env, jobject javaobj);
    /**
	 * Parameters: int data
     * Send one byte of urgent data on the socket. The byte to be sent is the lowest eight
     * bits of the data parameter. The urgent byte is
     * sent after any preceding writes to the socket OutputStream
     * and before any future writes to the OutputStream.
     * @param data The byte of data to send
	 * throws IOException;
     */
void socketSendUrgentData(JNIEnv *env, jobject javaobj, jint data);  

   /**
     * Writes to the socket.
     * @param bytes the data to be written
     * @param off the start offset in the data
     * @param len the number of bytes that are written
     * @param sendToAddress is the address of where to send this data to
     * @param sendToPort is the port of where to send this data to
     * @exception IOException If an I/O error has occurred.
     */
void socketWrite(JNIEnv *env, jobject javaobj, jbyteArray bytes, jint off, jint len, int sendToAddress, int sendToPort);

   /**
     * Reads into an array of bytes at the specified offset using
     * the received socket primitive. 
     * @param bytes the buffer into which the data is read
     * @param off the start offset of the data
     * @param len the maximum number of bytes read
     * @param timeout the read timeout in ms
     * @param sendersAddress is the address of the sender of this data
     * @param sendersPort is the port of the sender of this data
     * @return the actual number of bytes read, -1 is
     *          returned when the end of the stream is reached. 
     * @exception IOException If an I/O error has occurred.
     */
jint socketRead(JNIEnv *env, jobject javaobj, jbyteArray bytes, jint off, jint len, jint timeout, char *sendersAddress, int &sendersPort);


#endif
