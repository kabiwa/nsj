#ifndef SocketWrapper_h
#define SocketWrapper_h

/*
 *  SocketStore.h
 *  JavaNetImplementation
 *
 *  Created by Ian Taylor on 30/03/2007.
 *
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
 *  Created by Ian Taylor on 05/03/2007.
 *
 */

#include <jni.h>
#include "Agentj.h"
#include "protoDebug.h"
#include "AgentjVirtualMachine.h"


class SocketWrapper {	
public:	
	enum SocketType {TCP, UDP, ERROR};  

    SocketWrapper(JNIEnv *theEnv, jobject theJavaObj, SocketType type, jint socketid, Agentj *nsAgent);
	
	~SocketWrapper();
	
	void *getSocket() {
		return socket;
		}
		
	jobject getJavaObject() {
		return javaObject;
		}
		
	JNIEnv *getJNIInterfacePointer() {
		return env;
		}

	int getID() {
		return id;
		}

	SocketType getType() {
		return type;
		}

	Agentj *getNSAgentPtr() {
		return nsAgentPtr;
		}

	void OnSocketEvent(ProtoSocket& theSocket, ProtoSocket::Event theEvent);

	void generateCallback(const char *forMethod);
	
	void generateCallbackforData();

	void generateCallbackforConnectionOK();
	
	void generateConnectionRequestCallback();

private:
	SocketType type;
	int id;
	
// The JNI interface pointer (env below) is only valid in the current thread.
// A native method, therefore, must not pass the interface pointer from one thread to 
// another. A VM implementing the JNI may allocate and store thread-local 
// data in the area pointed to by the JNI interface pointer.
// Native methods receive the JNI interface pointer as an argument. 
// The VM is guaranteed to pass the same interface pointer to a native method 
// when it makes multiple calls to the native method from the same 
// Java thread. However, a native method can be called from different 
// Java threads, and therefore may receive different JNI interface pointers.

	JNIEnv *env; // the original interface pointer 	
#if 0
	JNIEnv *currentThreadEnv; // the interface pointer for current thread 
#endif

	jobject javaObject; // the JNI reference to the Java object that made the invocation
	void *socket; // can be a datagram or a TCP socket.
	Agentj *nsAgentPtr;

};

/**
 * Gets the dataBufferEmpty field from the java object. 
 */
jboolean getIsDataBufferEmptyID(JNIEnv *env, jobject javaobj);

/**
 * Sets the dataBufferEmpty field in the java object. 
 */
void setIsDataBufferEmptyID(JNIEnv *env, jobject javaobj, jboolean val);

/**
 * Gets the socketid from the java object that made this invocation. SokcetIDs are used
 * to store and retrieve C++ sockets from the SocketStore.
 */
jint getSocketID(JNIEnv *env, jobject javaobj);

/**
 * Gets the scoket wrapper that is paired with the provided java object 
 */
SocketWrapper *getSocketWrapper(JNIEnv *env, jobject javaobj);

/**
 * Gets the C++ socket corresponding to the provided JNI reference to the java object of
 * the socket in the Java world.
 */
void *getSocket(JNIEnv *env, jobject javaobj);

jfieldID getSocketPtrFieldID(JNIEnv *env, jobject obj);	  
	  
void *getSocketPtr(JNIEnv *env, jobject obj);

void setSocketPtr(JNIEnv *env, jobject obj, jlong ptr);


#endif // SocketWrapper_h
