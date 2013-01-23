/*
 *  JAVMSocketImp.cpp
 *  AgentJNativeCode
 *
 *  Created by Ian Taylor on 29/12/2007.
 *  Copyright 2007 __MyCompanyName__. All rights reserved.
 *
 */

#include "JAVMSocketImp.h"

void socketCreate(JNIEnv *env, jobject javaobj, SocketWrapper::SocketType socketType) {

	AgentjVirtualMachine::getVM(env);
	
	PLOG(PL_DEBUG, "JAVMSocketImp:socketCreate entered...\n");

	jint socketid = getSocketID(env, javaobj);

    Agentj *agentjPtr = getAgentjPtr(env, javaobj); // gets the pointer to the C++ agent from 
											   // the Ns2SocketImpl class's _brokerPtr variable
											   // set at construction
	
    PLOG(PL_DETAIL, "Creating Socket Wrapper...\n");

    SocketWrapper *socketwrapper = new SocketWrapper(env, javaobj, socketType, socketid, agentjPtr);

	PLOG(PL_DETAIL, "Adding socket number with id %i to socket store\n", (int)socketid);

	setSocketPtr(env,javaobj, (jlong)socketwrapper);
	
	PLOG(PL_DEBUG, "JAVMSocketImp:socketCreate exiting...\n");		
  }

// Parameters: InetAddress address, int port
// throws IOException;

void socketBind(JNIEnv *env, jobject javaobj, jint port) {

	PLOG(PL_DEBUG, "JAVMSocketImp:socketBind entering \n");

	ProtoSocket *socket = (ProtoSocket *)getSocket(env, javaobj); // gets the socket...
		
 	PLOG(PL_DETAIL, "JAVMSocketImp:socketBind: port is %i\n", (int)port);
	
	socket->Bind(port,NULL);
	
	PLOG(PL_DEBUG, "JAVMSocketImp:socketBind: exiting...\n");
  }


// Parameters: InetAddress address, int port, int timeout
// throws IOException;

void socketConnect(JNIEnv *env, jobject javaobj, jint address, jint port, jint timeout) {

    // DEAL with timeout ??
	
	PLOG(PL_DEBUG, "JAVMSocketImp:socketConnect entering\n");

	ProtoSocket *socket = (ProtoSocket *)getSocket(env, javaobj); // gets the socket...
			
	PLOG(PL_DETAIL, "JAVMSocketImp:socketConnect: Address is %u, port is %i\n", address, (int)port);
	
	ProtoAddress server;	
	server.SimSetAddress(address);
	server.SetPort(port);

	socket->Connect(server); // active

	PLOG(PL_DEBUG, "JAVMSocketImp:socketConnect Exiting\n");
  }
  
  
// Parameters: int count
// count is the listen backlog length
// throws IOException;

void socketListen(JNIEnv *env, jobject javaobj) {

	PLOG(PL_DEBUG, "JAVMSocketImp:socketListen Entering\n");

	ProtoSocket *socket = (ProtoSocket *)getSocket(env, javaobj); // gets the socket...

	socket->Listen(socket->GetPort());
	PLOG(PL_DEBUG, "JAVMSocketImp:socketListen Exiting\n");
}


// Parameters:  SocketImpl s
// So, look up the id for the SocketImpl provided and use this to pass this socket
// along to the accpet method.
void socketAccept(JNIEnv *env, jobject javaobj, jobject nssocketimpl) {
    PLOG(PL_DEBUG, "JAVMSocketImp:socketAccept Entering\n");

	ProtoSocket *socket = (ProtoSocket *)getSocket(env, javaobj); // gets the socket...

    PLOG(PL_DEBUG, "JAVMSocketImp:socketAccept getting socket to accept connection \n");
	
	ProtoSocket *socketToAcceptConnection = (ProtoSocket *)getSocket(env, nssocketimpl); // gets the socket that will accept this connection...

    PLOG(PL_DEBUG, "JAVMSocketImp:socketAccept Calling Ns-2 Accept \n");

	socket->Accept(socketToAcceptConnection);
	
	// need to set the destination port and address (socket that initiated the connection) of the local socket
	// used to deal with the connection.
	
	NsTCPProtoSocketAgent *theProtoSocket = (NsTCPProtoSocketAgent *) static_cast<ProtoSimAgent::SocketProxy*>(socketToAcceptConnection->GetHandle());

    int address = theProtoSocket->getTCPSocketAgent()->getDestinationAddress();	
    int port = theProtoSocket->getTCPSocketAgent()->getDestinationPort();	

    PLOG(PL_DETAIL, "JAVMSocketImp:socketAccept Request from Address %i, Port %i\n", address, port);
	 
	jclass cls = env->GetObjectClass(nssocketimpl);
	jfieldID fid;

	fid = env->GetFieldID( cls, "_dstaddr", "I");
	if (fid==0) {
    	throwException(env, "JAVMSocketImp:socketAccept: FATAL: Can't find field ID for dstaddr_\n");
    	exit(0);
    	}
	env->SetIntField(nssocketimpl, fid, address);

	fid = env->GetFieldID( cls, "_dstport", "I");
	if (fid==0) {
    	throwException(env, "JAVMSocketImp:socketAccept: FATAL: Can't find field ID for dstport_\n");
    	exit(0);
    	}
	env->SetIntField(nssocketimpl, fid, port); 
    PLOG(PL_DEBUG, "JAVMSocketImp:socketAccept Exiting\n");
  }

// TODO - need to address this problem.  The close do not work right because they are called immediately
// before the sends have taken place i.e. we need to wait until the Ns-2 scheduler has processed its queue
// before closing the sockets, otherwise when we send, we get errors because the sockets are closed ...
// disable for now but need to look later.
void socketClose(JNIEnv *env, jobject javaobj) {
 
	PLOG(PL_DEBUG, "JAVMSocketImp:socketShutdown : Entering \n");
    
	ProtoSocket *socket = (ProtoSocket *)getSocket(env, javaobj); // gets the socket...

	PLOG(PL_DETAIL, "JAVMSocketImp:socketShutdown : Got Socket \n");
	
	socket->Close();

    PLOG(PL_DEBUG, "JAVMSocketImp:socketShutdown : Exiting \n");
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
void socketSendUrgentData(JNIEnv *env, jobject javaobj, jint data) {
  	
	PLOG(PL_DEBUG, "JAVMSocketImp:socketSendUrgentData: Entering ... \n");

	char *datastr = new char[10]; // only need 4 I guess but whatever ...
	
	sprintf(datastr, "%i", (int)data);
	unsigned int size=strlen(datastr);
	
	ProtoSocket *socket = (ProtoSocket *)getSocket(env, javaobj); // gets the socket...
	
	socket->Send((const char *)datastr,size);
	delete [] datastr;
	PLOG(PL_DEBUG, "JAVMSocketImp:socketSendUrgentData: Exiting ...\n");
  }
  

   /**
     * Writes to the socket.
     * @param bytes the data to be written
     * @param off the start offset in the data
     * @param len the number of bytes that are written
     * @exception IOException If an I/O error has occurred.
     */
void socketWrite(JNIEnv *env, jobject javaobj, jbyteArray bytes, jint off, jint len, int sendToAddress, int sendToPort) {

	PLOG(PL_DEBUG, "Native JAVMSocketImp: socketWrite: Entering\n");

//	printf("Sending data now to ns-2 ....\n");
	
	SocketWrapper *sw = getSocketWrapper(env, javaobj);

	if (sw==NULL)
		throwException(env,"helpermethods: getSocket() - cannot find SocketWrapper for Socket, FATAL\n");
		
	ProtoSocket *socket = (ProtoSocket *)sw->getSocket(); // gets the socket...

	if (socket==NULL)
		throwException(env,"helpermethods: getSocket() - Socket is NULL in SocketWrapper class, FATAL\n");

	char *bdata = new char[len];

	// copy data to local buffer for sending
	
	env->GetByteArrayRegion(bytes, off, len, (jbyte *)bdata);
	
	unsigned int length = (unsigned int)len;
	
	PLOG(PL_DETAIL, "Native JAVMSocketImp: socketWrite: sending: %i bytes of data \n",  len);
	PLOG(PL_DETAIL, "Native JAVMSocketImp: socketWrite: sending: %s \n", (char *)(bdata));
	
	if (sw->getType()==SocketWrapper::TCP)
		socket->Send((const char *)bdata, length);
	else {
		PLOG(PL_DETAIL, "Native JAVMSocketImp: socketWrite: sending data to address %ld port %i\n", sendToAddress, sendToPort);
		ProtoAddress server;	
		server.SimSetAddress(sendToAddress); 
		server.SetPort(sendToPort);
		socket->SendTo((const char *)bdata, length, server);
		}
		
	if (length!=(unsigned int)len) {
  		PLOG(PL_ERROR, "Native _JAVMSocketImp: socketWrite: ERROR \n");
		PLOG(PL_ERROR, "Number of bytes sent = %i but number of bytes requested to send was %i\n", length, (int)len);
		}

	delete []bdata;
	
	PLOG(PL_DEBUG, "Native JAVMSocketImp: socketWrite: Exiting\n");
  }

jint socketRead(JNIEnv *env, jobject javaobj, jbyteArray bytes, jint off, jint len, jint timeout, char *sendersAddress, int &sendersPort) {

	PLOG(PL_DEBUG, "Native JAVMSocketImp: socketRead: entering\n");

	PLOG(PL_DETAIL, "Native JAVMSocketImp: attempting to read %i bytes of data, offet %i \n", len, off);

	ProtoSocket *socket = (ProtoSocket *)getSocket(env, javaobj); // gets the socket...
    NsTCPProtoSocketAgent *theProtoSocket = (NsTCPProtoSocketAgent *) static_cast<ProtoSimAgent::SocketProxy*>(socket->GetHandle());

	char *bdata = new char[len];
	
	unsigned int actualRead=len; 

	bool readOK = socket->Recv(bdata, actualRead);
	
	PLOG(PL_DETAIL, "Native JAVMSocketImp: socketRead: Data Read from Proto Socket\n");

	if (readOK) {
        if (theProtoSocket->GetRecvDataOffset() !=0) // if there is more data to read
            setIsDataBufferEmptyID(env, javaobj, false);
    } else { // EOF
     	setIsDataBufferEmptyID(env, javaobj, true);
    }
 
	PLOG(PL_DETAIL, "Native JAVMSocketImp: socketRead: Setting buffer for return\n");

	if ((int)actualRead>0)
	    env->SetByteArrayRegion(bytes, off, actualRead, (jbyte *)bdata);  // note - byteArray here is still larger than data

	PLOG(PL_DETAIL, "Native JAVMSocketImp: socketRead: received %i bytes of data \n", actualRead);

	delete []bdata;
	
	ProtoAddress sendersProtoAddress = socket->GetDestination();

	sendersPort = sendersProtoAddress.GetPort();
	
	sprintf(sendersAddress, "%u", sendersProtoAddress.SimGetAddress());
	
	PLOG(PL_DEBUG, "Native JAVMSocketImp: Senders Address = %s, port %i\n", sendersAddress, sendersPort);

	jthrowable exc;
	exc = env->ExceptionOccurred();
	if (exc) {
	    env->ExceptionDescribe();
		env->ExceptionClear();
		throwException(env, "JAVMSocketImp: socketRead Error - see message above");
		}

	PLOG(PL_DEBUG, "Native JAVMSocketImp: socketRead: Exiting\n");
	
	return actualRead;
  }
