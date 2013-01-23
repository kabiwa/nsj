/*
 * SocketStore.cpp : A storage container for a socket and a capability to store
 * many sockets within a storage area. We implement a straight array for
 * this implementation, which can grow depending on socket uses. This
 * is on the one hand veryy efficient since the IDs passed from Java
 * begin at 0 and increment by 1 for each new socket.  However,
 * it is also wasteful in space as sockets get deleted.  Overall, speed
 * is more critical.  
 *
 * The buffer can change size and works in a similar way to java.util.Vector
 * in that when the space runs out, it doubles in size.
 *
 * Created by Ian Taylor on 05/03/2007.
 *
 */ 

#include "SocketWrapper.h"

/**
 * Gets the dataBufferEmpty field from the java object. 
 */
jboolean getIsDataBufferEmptyID(JNIEnv *env, jobject javaobj) {
	jclass cls = env->GetObjectClass(javaobj);
    jfieldID fid = env->GetFieldID(cls, "_isDataBufferEmpty", "Z");

	if (fid==0) {
    	throwException(env, "JNI SocketWrapper.cpp (isDataBufferEmpty) FATAL: Can't find method ID for socket in AgentJ - check your CLASSPATH\n");
//    	exit(0);
    	}

    return env->GetBooleanField(javaobj,fid);
 }

/**
 * Sets the dataBufferEmpty field in the java object. 
 */
void setIsDataBufferEmptyID(JNIEnv *env, jobject javaobj, jboolean val) {
	jclass cls = env->GetObjectClass(javaobj);
    jfieldID fid = env->GetFieldID(cls, "_isDataBufferEmpty", "Z");
 
	if (fid==0) {
    	throwException(env, "JNI SocketWrapper.cpp (isDataBufferEmpty) FATAL: Can't find method ID for socket in AgentJ - check your CLASSPATH\n");
//    	exit(0);
    	}

    return env->SetBooleanField(javaobj,fid, val);
 }

/**
 * Gets the socketid from the java object that made this invocation. SokcetIDs are used
 * to store and retrieve C++ sockets from the SocketStore.
 */
jint getSocketID(JNIEnv *env, jobject javaobj) {
	jclass cls = env->GetObjectClass(javaobj);
    jfieldID fid = env->GetFieldID(cls, "_socketid", "I");

	if (fid==0) {
    	throwException(env, "JNI SocketWrapper.cpp (getSocketID) FATAL: Can't find method ID for socket in AgentJ - check your CLASSPATH\n");
 //   	exit(0);
    	}

    return env->GetIntField(javaobj,fid);
 }

jfieldID getSocketPtrFieldID(JNIEnv *env, jobject obj) {
	  jclass cls = env->GetObjectClass(obj);
	  jfieldID fid;

	  PLOG(PL_DETAIL, "getSocketPtrFieldID Entering\n ");
	  
	  fid = env->GetFieldID(cls, "_csocketPtr", "J");
	  
	  if (fid==0) {
	      PLOG(PL_FATAL, "JNI SocketWrapper.cpp, getSocketMethodIDPtrFieldID() - ERROR, Can't find _csocketPtr variable .... \n");
		  exit(0);
		  }
	  PLOG(PL_DETAIL, "getSocketPtrFieldID Exiting\n ");
	  return fid;
	  }
	  
	  
void *getSocketPtr(JNIEnv *env, jobject obj) {
	jfieldID fid=getSocketPtrFieldID(env, obj);
	PLOG(PL_DETAIL, "getSocketPtr:Getting value\n ");
	jlong val = env->GetLongField(obj,fid);
	PLOG(PL_DETAIL, "getSocketPtr:Got value = %u\n ", val);
	if (val==NULL) {
		throwException(env,"SocketWrapper: getSocketPtr() - _csocketPtr is not set, FATAL");
		return NULL;
		}
		
	return (void *)val;
} 

void setSocketPtr(JNIEnv *env, jobject obj, jlong ptr) {
	jfieldID fid=getSocketPtrFieldID(env, obj);
	env->SetLongField(obj,fid, ptr);
} 

SocketWrapper *getSocketWrapper(JNIEnv *env, jobject javaobj) {
	void *ptr = getSocketPtr(env,javaobj);
	if (ptr!=NULL) return (SocketWrapper *)ptr;
	else return NULL;
}
 
void *getSocket(JNIEnv *env, jobject javaobj) {
	SocketWrapper *sw = getSocketWrapper(env, javaobj);

	if (sw==NULL)
		throwException(env,"SocketWrapper: getSocket() - cannot find SocketWrapper for Socket, FATAL");
		
	void *socket = sw->getSocket(); // gets the socket...

	PLOG(PL_DETAIL, "getSocket:got socket = %u\n ", socket);
 
	if (socket==NULL) 
		throwException(env,"SocketWrapper: getSocket() - Socket is NULL in SocketWrapper class, FATAL");

	return socket;
	} 
			
/**
 * Socket Wrappers create a wrapper for each socket that is creted by Java for its C++ counterpart.  The
 * information stored within SocketWrapper allows a Java object invoke the C++ counterparts for the methods
 * and also allow the C++ object to callback to the Java object when data arrives - see OnSocketEvent method
 * below
 */
SocketWrapper::SocketWrapper(JNIEnv *theEnv, jobject theJavaObj, SocketWrapper::SocketType socketType, jint socketid, Agentj *nsAgent) {
    PLOG(PL_DETAIL, "Creating SocketWrapper for socket with ID %i \n", socketid);
	env=theEnv;
	javaObject=theEnv->NewGlobalRef(theJavaObj);
	id=socketid;
	type=socketType;
	nsAgentPtr=nsAgent;
	
	switch (socketType) {
		case TCP : PLOG(PL_DETAIL, "SocketWrapper Creating a TCP Socket...\n");
			socket = new ProtoSocket(ProtoSocket::TCP);
			break;
		case UDP : PLOG(PL_DETAIL, "SocketWrapper Creating a TCP Socket...\n");
			socket = new ProtoSocket(ProtoSocket::UDP);
			break;
		default : PLOG(PL_FATAL, "SocketStore constructor: Protocol NOT defined - choices are UDP or TCP !!!\n");
			exit(0);
		}
	((ProtoSocket *)socket)->SetNotifier(nsAgentPtr);
	((ProtoSocket *)socket)->SetListener(this, &SocketWrapper::OnSocketEvent); 
    PLOG(PL_DETAIL, "End creating SocketWrapper for socket with ID %i \n", socketid);
	}

SocketWrapper::~SocketWrapper() {
    PLOG(PL_DEBUG, "SocketWrapper Destructor, entering \n");
	switch (type) {
		case TCP : delete ((ProtoSocket *)socket);
			break;
		case UDP : delete ((ProtoSocket *)socket);
			break;
		default : PLOG(PL_FATAL, "SocketStore destructor: Protocol NOT defined - choices are UDP or TCP !!!\n");
			exit(0);
		}
	PLOG(PL_DETAIL, "SocketWrapper::Deleting Global Reference...\n");
	JavaVM *jvm =AgentjVirtualMachine::getVM(NULL);
	JNIEnv *currentThreadEnv;
	int res = jvm->AttachCurrentThread((void**)&currentThreadEnv, NULL);
	if (res>=0) {
	    currentThreadEnv->DeleteGlobalRef(javaObject);
	    jvm->DetachCurrentThread();
	}

    PLOG(PL_DEBUG, "SocketWrapper Destructor, exiting \n");
    }

void SocketWrapper::generateCallback(const char *forMethod) {

    PLOG(PL_DEBUG, "SocketWrapper::generateCallback for %s \n", forMethod);

	jint res;
		
	PLOG(PL_DETAIL, "SocketWrapper::generateCallbackFunc, Getting JVM...\n");

	JavaVM *jvm;
	env->GetJavaVM(&jvm);

//	JavaVM *jvm =AgentjVirtualMachine::getVM(NULL);

	PLOG(PL_DETAIL, "SocketWrapper::generateCallbackFunc, Attaching Thread...\n");

	JNIEnv *currentThreadEnv;	
	res = jvm->AttachCurrentThreadAsDaemon((void**)&currentThreadEnv, NULL);

	if (res < 0) {
		PLOG(PL_FATAL, "SocketWrapper::generateCallbackFunc, Attach failed. Aborting\n");
		exit(0);
		}

	PLOG(PL_DETAIL, "SocketWrapper::generateCallbackFunc, Getting Class object...\n");
    
	jclass cls = currentThreadEnv->GetObjectClass(javaObject);

	if (cls==0) {
       throwException(currentThreadEnv, "SocketWrapper: FATAL: Can't get jclass in SocketWrapper\n");
       exit(0);
       }

	PLOG(PL_DETAIL, "SocketWrapper::generateCallback - get class object\n");

    jmethodID mid = currentThreadEnv->GetMethodID(cls, forMethod, "()V"); 
		  
	if (mid==0) {
    	throwException(currentThreadEnv, "SocketWrapper: FATAL: Can't find Callback method dataArrived - check your classpath\n");
    	exit(0);
    	}

	PLOG(PL_DETAIL, "SocketWrapper::generateCallback - got method id - calling method now\n");

	currentThreadEnv->CallVoidMethod(javaObject, mid);

	PLOG(PL_DETAIL, "SocketWrapper::generateCallback - Checking for Exception\n");

    jthrowable exc;
	exc = currentThreadEnv->ExceptionOccurred();
	if (exc) {
		 PLOG(PL_ERROR, "SocketWrapper::generateCallback - Exception in Invocation\n");
         currentThreadEnv->ExceptionDescribe();
         currentThreadEnv->ExceptionClear();
		 throwException(currentThreadEnv, "SocketWrapper: getAddress() - Can't invoke callback, unknown error, FATAL\n");
		 }		

	PLOG(PL_DEBUG, "SocketWrapper::generateCallback exiting\n");
}

void SocketWrapper::generateCallbackforData() {
	generateCallback("dataArrived");
	PLOG(PL_DEBUG, "SocketWrapper::generateCallbackforData() Leaving\n");
	}

void SocketWrapper::generateConnectionRequestCallback() {
	generateCallback("connectRequestArrived");
	}

void SocketWrapper::generateCallbackforConnectionOK() {
	generateCallback("connectCompleted");
}

void SocketWrapper::OnSocketEvent(ProtoSocket& theSocket, ProtoSocket::Event theEvent) {
 
    switch (theEvent) {
        case ProtoSocket::ERROR_:
            PLOG(PL_INFO, "SocketWrapper::OnSocketEvent(ERROR) ...\n");
            break;
        case ProtoSocket::EXCEPTION:
            PLOG(PL_INFO, "SocketWrapper::OnSocketEvent(EXCEPTION) ...\n");
            break;
        case ProtoSocket::INVALID_EVENT:
            PLOG(PL_INFO, "SocketWrapper::OnSocketEvent(INVALID_EVENT) ...\n");
            break;
        case ProtoSocket::CONNECT:
            PLOG(PL_INFO, "SocketWrapper::OnSocketEvent(CONNECT) ...\n");
			generateCallbackforConnectionOK();
            break;  
        case ProtoSocket::ACCEPT: 
            PLOG(PL_INFO, "SocketWrapper::OnSocketEvent(ACCEPT) ...\n");
			generateConnectionRequestCallback();
            break; 
        case ProtoSocket::SEND:
            PLOG(PL_INFO, "SocketWrapper::OnSocketEvent(SEND) ...\n");
            break; 
        case ProtoSocket::RECV:
          //  printf("SocketWrapper::OnSocketEvent(RECEIVE) ...\n");
            PLOG(PL_INFO, "SocketWrapper::OnSocketEvent(RECEIVE) ...\n");
			generateCallbackforData();
			PLOG(PL_DETAIL, "SocketWrapper::OnSocketEvent(RECEIVE) Made Callback to Java ...\n");
            break; 
        case ProtoSocket::DISCONNECT:
            PLOG(PL_INFO, "SocketWrapper::OnSocketEvent(DISCONNECT) ...\n");
            theSocket.Close();
		default : PLOG(PL_FATAL, "SocketWrapper onSocketEvent: EVENT NOT defined - how?? !!! ");
			exit(0);
		}

	PLOG(PL_INFO, "SocketWrapper::OnSocketEvent Callback Exiting, going back to Protolib ...\n");

}  
	
