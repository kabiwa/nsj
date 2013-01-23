/*
 *  AgentjVirtualMachine.h
 *
 *  Created by Ian Taylor on Fri Mar 26 2004.
 *
 */
 
#ifndef _JVM_REF
#define _JVM_REF

#include <jni.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include "scheduler.h"
#include "protoDebug.h"

// FOR THE MAC ...

//#ifdef UNIX
//#define _REENTRANT
//#include <pthread.h>
//#endif

// #import <Cocoa/Cocoa.h>

#ifdef _WIN32
#define PATH_SEPARATOR ';'
#else /* UNIX */
#define PATH_SEPARATOR ':'
#endif

#define USER_CLASSPATH "./" /* where our is */

class Agentj;

/**
 * This class provides a mechanism for creating and destroying a virtual machine. It
 * also provides a conveneinece method for calling command() functions on Java 
 * objects and it also interacts with a remote container that stores references
 * to the Java objects so the garbage collector doesn't come along and delete them.
 *
 * The is only one instance of AgentjVirtualMachine class and only one instance of its java
 * counterpart, JavaAgent. The JavaAgent simply registers the Java object
 * with their supplied IDs.  The IDs are the pointers to the NS2 agents
 * that want to invoke Java commands, which makes sure that this reference
 * is unique to the agent itself (this also implies that only ONE Java 
 * object can be manipulated by one agent).
 *
 * We also use the Agent's pointer here so we can reuse this later if
 * the Java object uses the PAI interface to communicate data
 * between NS2 nodes.  
 *
 * The actual Java object that is being invoked implemented the Java 
 * CommandInterface and its command method can be invoked by using the
 * invokeCommand method in this class by supplying the appropriate ID.
 */
class AgentjVirtualMachine {

	private:
	    JavaVM *jvm; /* The virtual machine instance */
        JNIEnv *env;
        
		/**
		 * Convenience methods for obtaining a method reference to a method in
		 * our JavaAgent class
		 */
		jmethodID getMethodID(jclass cls, char *method, char *sig);
		
		jclass getJavaVMClass();

	public:
	    /** 
	     * Create a virtual machine: initialise it with the
	     * CLASSPATH and LD_LIBRARY_PATH variables
	     */
	    AgentjVirtualMachine();
    	
		/**
	     * Destroy a virtual machine
	     */
	    ~AgentjVirtualMachine();
		JNIEnv* getEnv(){ return env; }
		/*
		 * Registers an instance of the java class with the given 'className'
		 * with the JavaAgent class.
		 *
		 * className - is the Java class name that should be instansiated and attached to this node
		 * id - is the pointer to the C+ AgentJ agent
		 * nodeAddress - is the Ns-2 address for this node i.e. network address
		 */
		int attachJavaAgent(const char *className, Agentj *ptr, const char *nodeAddress);
		
		/**
		 * Unregisters java class that is associated with the Agent's ID.
		 * id - is the pointer to the C+ AgentJ agent
		 */
		int dettachJavaAgent(Agentj *ptr);
		
		/**
		 * Invokes the command with the supplied arguments on the native Java
		 * object which is referenced by the supplied id. 
		 *
		 * id - is the pointer to the C+ AgentJ agent
		 * command - the command to be passed to the agent
		 * args - the arguments for the command to be passed to the agent
		 */
        bool invokeCommand(Agentj *ptr, const char *command, const char *args);

   //     int setNSScheduler();

        void finish();

		static JavaVM *getVM(JNIEnv *env);
};

#endif // _JVM_REF

