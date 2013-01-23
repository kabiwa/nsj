/*
 *  java_net_sock_imp.cpp
 *  JavaNetImplementation
 *
 *  Created by Ian Taylor on 28/03/2007.

CALLBACKS :

    method called dataArrived in implementation

IDs:

    each class has an id upon creation
 *
 */

// This class implements everything to do with native TCP sockets, including the input/output streams
#include "agentj_nativeimp_NsSocketImpl.h"
#include "agentj_nativeimp_SocketInputStream.h"
#include "agentj_nativeimp_SocketOutputStream.h"
#include "java_net_SocketOptions.h"

#include "SocketWrapper.h"
#include "protoDebug.h"
#include "nsTCPProtoSocketAgent.h"

// The protolib socket implementations

#include "JAVMSocketImp.h"
 

// Parameters: none

JNIEXPORT void JNICALL Java_agentj_nativeimp_NsSocketImpl_initProto
  (JNIEnv *env, jclass) {
	PLOG(PL_INFO, "Java_agentj_nativeimp_NsSocketImpl_initProto: initialising\n");
  }

// ***************************************************
// JNI implementations for the NSSocketImpl Java class
// ***************************************************

// boolean alwaysTrue
// throws IOException;

JNIEXPORT void JNICALL Java_agentj_nativeimp_NsSocketImpl_socketCreate
  (JNIEnv *env, jobject javaobj, jboolean) {

  // ignore the boolean as its always true ...
	
	// create a TCP socket 
	
	socketCreate(env,javaobj, SocketWrapper::TCP);
  }

// Parameters: InetAddress address, int port
// throws IOException;

JNIEXPORT void JNICALL Java_agentj_nativeimp_NsSocketImpl_socketBind
  (JNIEnv *env, jobject javaobj, jint port) {
    // we ignore address here - not sure how to bind to a different address
	socketBind(env,javaobj, port);
  }


// Parameters: InetAddress address, int port, int timeout
// throws IOException;

JNIEXPORT void JNICALL Java_agentj_nativeimp_NsSocketImpl_socketConnect
  (JNIEnv *env, jobject javaobj, jlong address, jint port, jint timeout) {

	socketConnect(env,javaobj, (unsigned int)address, port, timeout);
  }
  
  
// Parameters: int count
// count is the listen backlog length
// throws IOException;

JNIEXPORT void JNICALL Java_agentj_nativeimp_NsSocketImpl_socketListen
  (JNIEnv *env, jobject javaobj, jint backlog) {
  
     // backlog - should we implement this?
	 
  	socketListen(env,javaobj);
}


// Parameters:  SocketImpl s
// So, look up the id for the SocketImpl provided and use this to pass this socket
// along to the accpet method.
JNIEXPORT void JNICALL Java_agentj_nativeimp_NsSocketImpl_socketAccept
  (JNIEnv *env, jobject javaobj, jobject nssocketimpl) {
  	socketAccept(env,javaobj, nssocketimpl);
  }


// Parameters: none
// throws IOException;
// This returns the nnumber of bytes available to read from the socket.

JNIEXPORT jint JNICALL Java_agentj_nativeimp_NsSocketImpl_socketAvailable
  (JNIEnv *env, jobject javaobj) {
  
	ProtoSocket *socket = (ProtoSocket *)getSocket(env, javaobj); // gets the socket...

	// THIS GETS THE IMPLEMENTATION OF THE SIM SOCKET - the NsSocketProxy Object ...
	
	NsTCPProtoSocketAgent *simSocketImpl = (NsTCPProtoSocketAgent *) static_cast<ProtoSimAgent::SocketProxy*>(socket->GetHandle());

	int available = simSocketImpl->AvailableToRead();
	
	PLOG(PL_DEBUG, "Java_agentj_nativeimp_NsSocketImpl_socketAvailable: bytes avaiable = %i...\n ", available);

    return available;
	}

// Parameters: boolean useDeferredClose
// if useDeferredClose is true then the socket is closed by duplicating the file descriptor - this enables
// the socket to be closed without releasing the file descriptor.
// throws IOException;

JNIEXPORT void JNICALL Java_agentj_nativeimp_NsSocketImpl_socketClose0
  (JNIEnv *env, jobject javaobj, jboolean useDeferredClose) {

    if (useDeferredClose==true) return; // don't do pre-closes
	}

// Parameters: int howto
// howto can be 0 or 1 :
//   public final static int SHUT_RD = 0;
// Shut down the read half of the socket connectioon
//    public final static int SHUT_WR = 1;
// shut down the write half of the socket connection
// throws IOException;

JNIEXPORT void JNICALL Java_agentj_nativeimp_NsSocketImpl_socketShutdown
  (JNIEnv *env, jobject javaobj, jint howto) {
 
    if (howto==1) return; // waiting for read shutdown and then close socket

  	socketClose(env,javaobj);
	 
}

// Parameters: int cmd, boolean on, Object value
// throws SocketException;
// For integer:
// use ((Integer) val).intValue() on the object to get value.
// For the GetOptions, for boolean values Java uses the return value from getOptions:
// return Boolean.valueOf(ret != -1);
// --> so, -1 is false, anything else is true.

JNIEXPORT void JNICALL Java_agentj_nativeimp_NsSocketImpl_socketSetOption
  (JNIEnv *env, jobject javaobj, jint cmd, jboolean on, jobject value) {
  
	PLOG(PL_DEBUG, "Java_agentj_nativeimp_NsSocketImpl_socketSetOption: Entering ...\n ");

//	ProtoSocket *socket = (ProtoSocket *)getSocket(env, javaobj); // gets the socket...

	switch (cmd) {
		case java_net_SocketOptions_SO_LINGER: // Boolean
			break;
		case java_net_SocketOptions_SO_TIMEOUT: // Value = Integer
			break;
		case java_net_SocketOptions_IP_TOS: // Value = Integer
			break;
		case java_net_SocketOptions_SO_BINDADDR : // not allowed
			break;
		case java_net_SocketOptions_TCP_NODELAY : // boolean
			break;
		case java_net_SocketOptions_SO_SNDBUF: // Value = Integer
			break;
		case java_net_SocketOptions_SO_RCVBUF: // Value = Integer
			break;		
		case java_net_SocketOptions_SO_KEEPALIVE: // boolean
			break;
		case java_net_SocketOptions_SO_OOBINLINE: // boolean
			break;
		case java_net_SocketOptions_SO_REUSEADDR: // boolean
			break;
		default : PLOG(PL_WARN, "Java_agentj_nativeimp_NsSocketImpl_socketSetOption: Invalied Option !!! \n");
			exit(0);
		}	
	PLOG(PL_DEBUG, "Java_agentj_nativeimp_NsSocketImpl_socketSetOption: Exiting ...\n ");
	}

// Parameters: int opt, Object iaContainerObj
// see SOCKOPTIONS.txt for description of these ...
// 	 throws SocketException;

JNIEXPORT jint JNICALL Java_agentj_nativeimp_NsSocketImpl_socketGetOption
  (JNIEnv *env, jobject javaobj, jint cmd, jobject objcontainer) {

	PLOG(PL_DEBUG, "Java_agentj_nativeimp_NsSocketImpl_socketGetOption: Entering ... \n");

	// ProtoSocket *socket = (ProtoSocket *)getSocket(env, javaobj); // gets the socket...

	switch (cmd) {
		case java_net_SocketOptions_SO_LINGER: // Boolean
			break;
		case java_net_SocketOptions_SO_TIMEOUT: // Value = Integer
			break;
		case java_net_SocketOptions_IP_TOS: // Value = Integer
			break;
		case java_net_SocketOptions_SO_BINDADDR : // not allowed
			break;
		case java_net_SocketOptions_TCP_NODELAY : // boolean
			break;
		case java_net_SocketOptions_SO_SNDBUF: // Value = Integer
			break;
		case java_net_SocketOptions_SO_RCVBUF: // Value = Integer
			break;		
		case java_net_SocketOptions_SO_KEEPALIVE: // boolean
			break;
		case java_net_SocketOptions_SO_OOBINLINE: // boolean
			break;
		case java_net_SocketOptions_SO_REUSEADDR: // boolean
			break;
		default : PLOG(PL_WARN, "Java_agentj_nativeimp_NsSocketImpl_socketSetOption: Invalied Option !!! \n");
			exit(0);
		}

	PLOG(PL_DEBUG, "Java_agentj_nativeimp_NsSocketImpl_socketGetOption: Exiting ...\n ");
		
	return -1;
  }

// Parameters: int opt, Object iaContainerObj, FileDescriptor fd
// This is NOT USED :

JNIEXPORT jint JNICALL Java_agentj_nativeimp_NsSocketImpl_socketGetOption1
  (JNIEnv *env, jobject javaobj, jint, jobject, jobject) {
  // No need to implement - never called by the  java.net API ....
  return -1;
  }

    /**
	 * Parameters: int data
     * Send one byte of urgent data on the socket. The byte to be sent is the lowest eight
     * bits of the data parameter. The urgent byte is
     * sent after any preceding writes to the socket OutputStream
     * and before any future writes to the OutputStream.
     * @param data The byte of data to send
	 * throws IOException;
     */
JNIEXPORT void JNICALL Java_agentj_nativeimp_NsSocketImpl_socketSendUrgentData
  (JNIEnv *env, jobject javaobj, jint data) {
  	
  	socketSendUrgentData(env,javaobj, data);
  }
  

// Stream Implementation 


JNIEXPORT void JNICALL Java_agentj_nativeimp_SocketOutputStream_init
  (JNIEnv *, jclass) {
    // nothing to intialise
}

   /**
     * Writes to the socket.
     * @param bytes the data to be written
     * @param off the start offset in the data
     * @param len the number of bytes that are written
     * @exception IOException If an I/O error has occurred.
     */
JNIEXPORT void JNICALL Java_agentj_nativeimp_SocketOutputStream_socketWrite0
  (JNIEnv *env, jobject javaobj, jbyteArray bytes, jint off, jint len) {

	socketWrite(env,javaobj,bytes,off,len, NULL, -1);
  }

   /**
     * Reads into an array of bytes at the specified offset using
     * the received socket primitive. 
     * @param bytes the buffer into which the data is read
     * @param off the start offset of the data
     * @param len the maximum number of bytes read
     * @param timeout the read timeout in ms
     * @return the actual number of bytes read, -1 is
     *          returned when the end of the stream is reached. 
     * @exception IOException If an I/O error has occurred.
     */
JNIEXPORT jint JNICALL Java_agentj_nativeimp_SocketInputStream_socketRead0
  (JNIEnv *env, jobject javaobj, jbyteArray bytes, jint off, jint len, jint timeout) {

	PLOG(PL_DETAIL, "Java_agentj_nativeimp_SocketInputStream_socketRead0 Entered\n");

	int sendersPort;
	char sendersAddress[5];
	
	int actualread = socketRead(env,javaobj,bytes,off,len,timeout, sendersAddress, sendersPort);
	
	PLOG(PL_DETAIL, "Received %i bytes from Node %s, port %i\n", actualread, sendersAddress, sendersPort);
	
	return actualread;
  }


JNIEXPORT void JNICALL Java_agentj_nativeimp_SocketInputStream_init
  (JNIEnv *, jclass) {
  // nothing to intialise
  }


