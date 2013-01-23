/*
 *  DatagramSocket.cpp
 *  AgentJNativeCode
 *
 *  Created by Ian Taylor on 22/12/2007.
 *  Copyright 2007 __MyCompanyName__. All rights reserved.
 *
 */

#include "agentj_nativeimp_NsDatagramSocketImpl.h"
#include "agentj_nativeimp_util_DataPacket.h"

// The protolib socket implementations

#include "JAVMSocketImp.h"

JNIEXPORT void JNICALL Java_agentj_net_DatagramPacket_init
  (JNIEnv *, jclass) {}

/*
 * Class:     javm_nativeimp_NsDatagramSocketImpl
 * Method:    init
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_agentj_nativeimp_NsDatagramSocketImpl_init
  (JNIEnv *, jclass) {
  // nothing to init here
  }


/*
 * Class:     javm_nativeimp_NsDatagramSocketImpl
 * Method:    datagramSocketCreate
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_agentj_nativeimp_NsDatagramSocketImpl_datagramSocketCreate
  (JNIEnv *env, jobject javaobj) {
	AgentjVirtualMachine::getVM(env);
	
	PLOG(PL_DETAIL, "Java_agentj_nativeimp_NsDatagramSocketImpl_datagramSocketCreate: Entering\n");

	socketCreate(env,javaobj, SocketWrapper::UDP);
	
	PLOG(PL_DETAIL, "Java_agentj_nativeimp_NsDatagramSocketImpl_datagramSocketCreate: Exiting\n");
}


/*
 * Class:     agentj_nativeimp_NsDatagramSocketImpl
 * Method:    bind0
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_agentj_nativeimp_NsDatagramSocketImpl_bind0
  (JNIEnv *env, jobject javaobj, jint port, jint) {
  	PLOG(PL_DETAIL, "Java_agentj_nativeimp_NsDatagramSocketImpl_bind0: Entering\n");

  	socketBind(env,javaobj, port);
	PLOG(PL_DETAIL, "Java_agentj_nativeimp_NsDatagramSocketImpl_bind0: Exiting\n");
}


/*
 * Class:     javm_nativeimp_NsDatagramSocketImpl
 * Method:    connect0
 * Signature: (Ljavm/net/InetAddress;I)V
 */
JNIEXPORT void JNICALL Java_agentj_nativeimp_NsDatagramSocketImpl_connect0
  (JNIEnv *env, jobject javaobj, jint address, jlong port) {
  	socketConnect(env,javaobj, address, (unsigned int)port, (int)-1);
}

/*
 * Class:     javm_nativeimp_NsDatagramSocketImpl
 * Method:    send
 * Signature: (Lagentj/nativeimp/util/DataPacket;)V
 */
JNIEXPORT void JNICALL Java_agentj_nativeimp_NsDatagramSocketImpl_send0
  (JNIEnv *env, jobject javaobj, jobject datapacket) {

	PLOG(PL_DEBUG, "Java_agentj_nativeimp_NsDatagramSocketImpl_send: Entering\n");

  	jclass jobcls = env->GetObjectClass(javaobj); // gets the class signature for the datapacket class  

    jmethodID getSocketID = env->GetMethodID(jobcls, "getSocketID", "()I"); 
	PLOG(PL_DETAIL, "Java_agentj_nativeimp_NsDatagramSocketImpl_send: Got socket ID = %i\n", getSocketID);
  
  	jclass cls = env->GetObjectClass(datapacket); // gets the class signature for the datapacket class  

	if (cls==0) {
    	PLOG(PL_FATAL, "Java_agentj_nativeimp_NsDatagramSocketImpl_send: Can't access DataPacket Class\n");
    	exit(0);
    	}

	PLOG(PL_DETAIL, "Java_agentj_nativeimp_NsDatagramSocketImpl_send: Got data packet class = %u\n", cls);
	
    jmethodID getDataID = env->GetMethodID(cls, "getData", "()[B"); 
	PLOG(PL_DETAIL, "Java_agentj_nativeimp_NsDatagramSocketImpl_send: Got address ID = %i\n", getDataID);
    jmethodID getLengthID = env->GetMethodID(cls, "getLength", "()I"); 
	PLOG(PL_DETAIL, "Java_agentj_nativeimp_NsDatagramSocketImpl_send: Got length ID = %i\n", getLengthID);
    jmethodID getOffsetID = env->GetMethodID(cls, "getOffset", "()I"); 
	PLOG(PL_DETAIL, "Java_agentj_nativeimp_NsDatagramSocketImpl_send: Got offset ID = %i\n", getOffsetID);
    jmethodID getPortID = env->GetMethodID(cls, "getPort", "()I"); 
	PLOG(PL_DETAIL, "Java_agentj_nativeimp_NsDatagramSocketImpl_send: Got port ID  = %i\n", getPortID);
    jmethodID getAddressID = env->GetMethodID(cls, "getAddress", "()J"); 
	PLOG(PL_DETAIL, "Java_agentj_nativeimp_NsDatagramSocketImpl_send: Got address ID = %i\n", getAddressID);

	if (getAddressID==0) {
    	PLOG(PL_FATAL, "Java_agentj_nativeimp_NsDatagramSocketImpl_send: Can't access DataPacket Class methods\n");
    	exit(0); 
    	}
		  		  
	PLOG(PL_DETAIL, "Java_agentj_nativeimp_NsDatagramSocketImpl_send: All IDs are ok\n");

	jlong addr = env->CallLongMethod(datapacket, getAddressID);

	PLOG(PL_DETAIL, "Java_agentj_nativeimp_NsDatagramSocketImpl_send: Got address %u\n", addr);
	
	jbyteArray data = (jbyteArray)env->CallObjectMethod(datapacket, getDataID);	
	PLOG(PL_DETAIL, "Java_agentj_nativeimp_NsDatagramSocketImpl_send: Called getData\n");
	jint len = env->CallIntMethod(datapacket, getLengthID);	
	PLOG(PL_DETAIL, "Java_agentj_nativeimp_NsDatagramSocketImpl_send: Called getLength = %i\n", len);
	jint off = env->CallIntMethod(datapacket, getOffsetID);	
	PLOG(PL_DETAIL, "Java_agentj_nativeimp_NsDatagramSocketImpl_send: Called getOffset = %i\n", off);
	jint port = env->CallIntMethod(datapacket, getPortID);	
		
	PLOG(PL_DETAIL, "Java_agentj_nativeimp_NsDatagramSocketImpl_send: Called getPort = %i\n", port);

	jthrowable exc;
	exc = env->ExceptionOccurred();
	if (exc) {
         env->ExceptionDescribe();
         env->ExceptionClear();
		 throwException(env, "helpermethods: getAddress() - Can't get HostName, unknown error, FATAL");
		 exit(0);
		 }

	PLOG(PL_DETAIL, "Java_agentj_nativeimp_NsDatagramSocketImpl_send: Invoking scoketWrite now\n");

	socketWrite(env, javaobj, data, off, len, (unsigned int)addr, port);

	PLOG(PL_DEBUG, "Java_agentj_nativeimp_NsDatagramSocketImpl_send: Exiting\n");
}

/*
 * Class:     javm_nativeimp_NsDatagramSocketImpl
 * Method:    receive0
 * Signature: (Lagentj/nativeimp/util/DataPacket;)V
 */
JNIEXPORT void JNICALL Java_agentj_nativeimp_NsDatagramSocketImpl_receive0
  (JNIEnv *env, jobject javaobj, jobject datapacket) {
  
	PLOG(PL_DEBUG, "Java_agentj_nativeimp_NsDatagramSocketImpl_receive0: Entering\n");
	
  	jclass jobcls = env->GetObjectClass(javaobj); // gets the class signature for the datapacket class  

    jmethodID getSocketID = env->GetMethodID(jobcls, "getSocketID", "()I"); 
	PLOG(PL_DETAIL, "Java_agentj_nativeimp_NsDatagramSocketImpl_receive0: Got socket ID = %i\n", getSocketID);
	jint sockid = env->CallIntMethod(javaobj, getSocketID);	
	PLOG(PL_DETAIL, "Java_agentj_nativeimp_NsDatagramSocketImpl_receive0: Got socket ID = %i\n", sockid);
  
	PLOG(PL_DETAIL, "Java_agentj_nativeimp_NsDatagramSocketImpl_receive0: datagram packet is %l\n", datapacket);
    
  	jclass cls = env->GetObjectClass(datapacket); // gets the class signature for the datapacket class  

	PLOG(PL_DETAIL, "Java_agentj_nativeimp_NsDatagramSocketImpl_receive0: Got datagram packet\n");
	
    jmethodID getDataID = env->GetMethodID(cls, "getData", "()[B"); 
    jmethodID getLengthID = env->GetMethodID(cls, "getLength", "()I"); 
		  		  
    PLOG(PL_DETAIL, "Java_agentj_nativeimp_NsDatagramSocketImpl_receive0: Got Method IDs Data %i Length %i \n", 
				getDataID, getLengthID);

	if ((getDataID==0) || (getLengthID==0)) {
    	PLOG(PL_FATAL, "Java_agentj_nativeimp_NsDatagramSocketImpl_receive0: Can't access datapacket Class methods\n");
		exit(0);
    	}
	
    PLOG(PL_DETAIL, "Java_agentj_nativeimp_NsDatagramSocketImpl_receive0: Getting Length field now\n");
	jint len = env->CallIntMethod(datapacket, getLengthID);	
    PLOG(PL_DETAIL, "Java_agentj_nativeimp_NsDatagramSocketImpl_receive0: Getting Data field now\n");
	jbyteArray data = (jbyteArray)env->CallObjectMethod(datapacket, getDataID);	
	
	char sendersAddress[40];
	int sendersPort;
		
    PLOG(PL_DETAIL, "Java_agentj_nativeimp_NsDatagramSocketImpl_receive0: Calling socket read method to read data\n");
    PLOG(PL_DETAIL, "Java_agentj_nativeimp_NsDatagramSocketImpl_receive0: Data length %i ArraySize %i\n", len, env->GetArrayLength(data));
    jint bytesread = socketRead(env, javaobj, data, 0, len, 0, sendersAddress, sendersPort);
    PLOG(PL_DETAIL, "Java_agentj_nativeimp_NsDatagramSocketImpl_receive0: Called socketRead\n");

	char *returnData = new char[bytesread];
	
    PLOG(PL_DETAIL, "Java_agentj_nativeimp_NsDatagramSocketImpl_receive0: Getting Data of size %i\n", bytesread);

	env->GetByteArrayRegion(data, 0, bytesread, (jbyte *)returnData);  // Get actual bytes read

    PLOG(PL_DETAIL, "Java_agentj_nativeimp_NsDatagramSocketImpl_receive0: Got Byte Array from routine %i\n", returnData);

	jbyteArray result = env->NewByteArray(bytesread);
	
	if (result!=0) {	
		env->SetByteArrayRegion(result, 0, bytesread, (jbyte *)returnData); // copy data to size

		PLOG(PL_DETAIL, "Java_agentj_nativeimp_NsDatagramSocketImpl_receive0: Data read, bulding data packet object now\n");

		jmethodID setDataID = env->GetMethodID(cls, "setData", "([B)V"); 
		env->CallVoidMethod(datapacket, setDataID, result);
		env->ReleaseByteArrayElements(result, NULL, JNI_ABORT);
		delete []returnData;
		}
	else {
		PLOG(PL_ERROR, "Out of Memory in Java_agentj_nativeimp_NsDatagramSocketImpl_receive0 routine - can't create result array\n");
		jthrowable exc;
		exc = env->ExceptionOccurred();
		if (exc) {
			env->ExceptionDescribe();
			env->ExceptionClear();
			throwException(env, "helpermethods: getAddress() - Can't get HostName, unknown error, FATAL");
		}
		exit(0);
		}
	
	jmethodID setAddressID = env->GetMethodID(cls, "setAddress", "(I)V"); 
	if (setAddressID!=0) env->CallVoidMethod(datapacket, setAddressID, atoi(sendersAddress));	
			
    jmethodID setLengthID = env->GetMethodID(cls, "setLength", "(I)V"); 
	if (setLengthID!=0) env->CallVoidMethod(datapacket, setLengthID, bytesread);	

    jmethodID setPortID = env->GetMethodID(cls, "setPort", "(I)V"); 
	if (setPortID!=0) env->CallVoidMethod(datapacket, setPortID, sendersPort);	

	PLOG(PL_DEBUG, "Java_agentj_nativeimp_NsDatagramSocketImpl_receive0: Exiting\n");
}


/*
 * Class:     javm_nativeimp_NsDatagramSocketImpl
 * Method:    peek
 * Signature: (Ljavm/net/InetAddress;)I
 */
JNIEXPORT jint JNICALL Java_agentj_nativeimp_NsDatagramSocketImpl_peek
  (JNIEnv *env, jobject javaobj, jobject longAddress) {
	PLOG(PL_DEBUG, "Java_agentj_nativeimp_NsDatagramSocketImpl_peek: Entering\n");
  
	ProtoSocket *socket = (ProtoSocket *)getSocket(env, javaobj); // gets the socket...
	int len=1;
	char *bdata = new char[len];
	unsigned int actualRead=len; 
	
	socket->Recv(bdata, actualRead);
	delete []bdata;

	char sendersAddress[5];
	int sendersPort;
	
	ProtoAddress sendersProtoAddress = socket->GetDestination();
	sendersPort = sendersProtoAddress.GetPort();
	sendersProtoAddress.GetHostString(sendersAddress, strlen(sendersAddress));

	jclass longClass = env->FindClass("java/lang/Long");
	jmethodID setAddressID = env->GetMethodID(longClass, "<init>", "(J)V"); 
	 
	longAddress = env->NewObject(longClass, setAddressID, (jlong)sendersProtoAddress.SimGetAddress());
	  
	return 0;
  }

/*
 * Class:     javm_nativeimp_NsDatagramSocketImpl
 * Method:    peekData
 * Signature: (Ljavm/net/datapacket;)I
 
 * The datagram implementation just wants to know where the data is coming from.
 */
JNIEXPORT jint JNICALL Java_agentj_nativeimp_NsDatagramSocketImpl_peekData
  (JNIEnv *env, jobject javaobj, jobject datapacket) {

// This is incorrect - should return the address and the data BUT NOT CONSUME THE DATA.
 
	PLOG(PL_DEBUG, "Java_agentj_nativeimp_NsDatagramSocketImpl_peekData: Entering\n");

    Java_agentj_nativeimp_NsDatagramSocketImpl_receive0(env,javaobj, datapacket);

  	jclass cls = env->GetObjectClass(datapacket); // gets the class signature for the datapacket class  
	jmethodID getAddressID = env->GetMethodID(cls, "getAddress", "()I"); 
	PLOG(PL_DEBUG, "Java_agentj_nativeimp_NsDatagramSocketImpl_peekData: Exiting\n");
	return env->CallIntMethod(datapacket, getAddressID);	
}

/*
 * Class:     javm_nativeimp_NsDatagramSocketImpl
 * Method:    setTimeToLive
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_agentj_nativeimp_NsDatagramSocketImpl_setTimeToLive
  (JNIEnv *, jobject, jint) {}

/*
 * Class:     javm_nativeimp_NsDatagramSocketImpl
 * Method:    getTimeToLive
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_agentj_nativeimp_NsDatagramSocketImpl_getTimeToLive
  (JNIEnv *, jobject) {
    return 0;
}

/*
 * Class:     javm_nativeimp_NsDatagramSocketImpl
 * Method:    setTTL
 * Signature: (B)V
 */
JNIEXPORT void JNICALL Java_agentj_nativeimp_NsDatagramSocketImpl_setTTL
  (JNIEnv *, jobject, jbyte) {}

/*
 * Class:     javm_nativeimp_NsDatagramSocketImpl
 * Method:    getTTL
 * Signature: ()B
 */
JNIEXPORT jbyte JNICALL Java_agentj_nativeimp_NsDatagramSocketImpl_getTTL
  (JNIEnv *, jobject) {
    return 0;
}

/*
 * Class:     javm_nativeimp_NsDatagramSocketImpl
 * Method:    join
 * Signature: (Ljavm/net/InetAddress;Ljavm/net/NetworkInterface;)V
 */
JNIEXPORT void JNICALL Java_agentj_nativeimp_NsDatagramSocketImpl_join
  (JNIEnv *env, jobject javaobj, jlong socketAddress) {
  
	PLOG(PL_DEBUG, "Java_agentj_nativeimp_NsDatagramSocketImpl_join entering \n");

	ProtoSocket *socket = (ProtoSocket *)getSocket(env, javaobj); // gets the socket...
					
	ProtoAddress multicastAddress;

	multicastAddress.SimSetAddress((unsigned int)socketAddress); 
		
	PLOG(PL_DETAIL, "Java_agentj_nativeimp_NsDatagramSocketImpl_join: JOIN Group Address is %u\n", socketAddress);

	socket->JoinGroup(multicastAddress, NULL); 
		
	PLOG(PL_DEBUG, "Java_agentj_nativeimp_NsDatagramSocketImpl_join: exiting...\n");
	}

/*
 * Class:     javm_nativeimp_NsDatagramSocketImpl
 * Method:    leave
 * Signature: (Ljavm/net/InetAddress;Ljavm/net/NetworkInterface;)V
 */
JNIEXPORT void JNICALL Java_agentj_nativeimp_NsDatagramSocketImpl_leave
  (JNIEnv *env, jobject javaobj, jlong socketAddress) {
    
  	PLOG(PL_DEBUG, "Java_agentj_nativeimp_NsDatagramSocketImpl_leave entering \n");

	ProtoSocket *socket = (ProtoSocket *)getSocket(env, javaobj); // gets the socket...
		
	ProtoAddress multicastAddress;

	multicastAddress.SimSetAddress((unsigned int)socketAddress);
	
	socket->LeaveGroup(multicastAddress, NULL); 
		
	PLOG(PL_DEBUG, "Java_agentj_nativeimp_NsDatagramSocketImpl_leave: exiting...\n");
	
}

/*
 * Class:     javm_nativeimp_NsDatagramSocketImpl
 * Method:    datagramSocketClose
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_agentj_nativeimp_NsDatagramSocketImpl_datagramSocketClose
  (JNIEnv *env, jobject javaobj) {
	PLOG(PL_DEBUG, "NsDatagramSocketImpl_datagramSocketClose: entering...\n");	
  	socketClose(env,javaobj);
	
	PLOG(PL_DEBUG, "NsDatagramSocketImpl_datagramSocketClose: exiting...\n");	
  }

/*
 * Class:     javm_nativeimp_NsDatagramSocketImpl
 * Method:    socketSetOption
 * Signature: (ILjava/lang/Object;)V
 * int optID and Object o, which is always a boolean i.e. sett option on or off
 */
JNIEXPORT void JNICALL Java_agentj_nativeimp_NsDatagramSocketImpl_socketSetOption
  (JNIEnv *env, jobject javaobj, jint optID, jobject onoff) {}

/*
 * Class:     javm_nativeimp_NsDatagramSocketImpl
 * Method:    socketGetOption
 * Signature: (I)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_agentj_nativeimp_NsDatagramSocketImpl_socketGetOption
  (JNIEnv *, jobject, jint optID) {
    return NULL;
}

/*
 * Class:     javm_nativeimp_NsDatagramSocketImpl
 * Method:    disconnect0
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_agentj_nativeimp_NsDatagramSocketImpl_disconnect0
  (JNIEnv *, jobject, jint) {}

