/*
 *  TimerWrapper.cpp
 *  AgentJNativeCode
 *
 *  Created by Ian Taylor on 05/01/2008.
 *  Copyright 2008 __MyCompanyName__. All rights reserved.
 *
 */

#include "TimerWrapper.h"

TimerWrapper::TimerWrapper(JNIEnv *theEnv, jobject theJavaObj, Agentj *nsAgent, double timerdelay) {
	env=theEnv;
	javaObject=theEnv->NewGlobalRef(theJavaObj);
	nsAgentPtr=nsAgent;
	delay=timerdelay;
	timer = new AgentJTimer(this);
	}

TimerWrapper::~TimerWrapper() {
	PLOG(PL_DETAIL, "TimerWrapper::destructor called...\n");
	if (timer->status()==TIMER_PENDING)  {
		PLOG(PL_DETAIL, "TimerWrapper::timer pending, attempting to cancel...\n");
		timer->cancel();
	}
	delete timer;
    PLOG(PL_DETAIL, "TimerWrapper::destructor Deleting Global Reference...\n");
	JavaVM *jvm =AgentjVirtualMachine::getVM(NULL);
	JNIEnv *currentThreadEnv;
	int res = jvm->AttachCurrentThread((void**)&currentThreadEnv, NULL);
	if (res>=0) {
	    currentThreadEnv->DeleteGlobalRef(javaObject);
	    jvm->DetachCurrentThread();
	}
	PLOG(PL_DETAIL, "TimerWrapper::destructor leaving...\n");	
	}
	

void TimerWrapper::startTimer() { 
	PLOG(PL_DETAIL, "TimerWrapper::startTimer: starting now...\n");

	if (timer!=NULL) {
	    timer->resched(delay);
	} else  {
    	throwException(env, "TimerWrapper: FATAL: Could not create timer instance - no resources left ?\n");
    	exit(0);
    	}
	}

void TimerWrapper::cancelTimer() { 
	PLOG(PL_DETAIL, "TimerWrapper::cancelTimer: starting now...\n");
	
	if (timer!=NULL) {
	    timer->cancel();
	} else  {
    	throwException(env, "TimerWrapper: FATAL: Could not create timer instance - no resources left ?\n");
    	exit(0);
	}
}

	 	
bool TimerWrapper::timerTriggered() {
	
	PLOG(PL_DEBUG, "TimerWrapper::generateCallback\n");

	jint res;
		
	PLOG(PL_DETAIL, "TimerWrapper::generateCallbackFunc, Getting JVM...\n");
	
	JavaVM *jvm;
	env->GetJavaVM(&jvm);
	
	PLOG(PL_DETAIL, "TimerWrapper::generateCallbackFunc, Attaching Thread...\n");

	JNIEnv *currentThreadEnv;	
	res = jvm->AttachCurrentThreadAsDaemon((void**)&currentThreadEnv, NULL);

	if (res < 0) {
		PLOG(PL_FATAL, "TimerWrapper::generateCallbackFunc, Attach failed. Aborting\n");
		exit(0);
		}

	jclass cls = currentThreadEnv->GetObjectClass(javaObject);
	
	PLOG(PL_DETAIL, "TimerWrapper::generateCallback - get class object\n");

    jmethodID mid = currentThreadEnv->GetMethodID(cls, "timerTriggered", "()V"); 
		  
	if (mid==0) {
		PLOG(PL_FATAL, "TimerWrapper: FATAL: Can't find Callback method timerTriggered - check ProtolibTimer.java");
    	throwException(currentThreadEnv, "TimerWrapper: FATAL: Can't find Callback method timerTriggered - check your classpath\n");
    	exit(0);
    	}

	PLOG(PL_DETAIL, "TimerWrapper::generateCallback - got method id - calling method now\n");

	currentThreadEnv->CallVoidMethod(javaObject, mid);

	PLOG(PL_DETAIL, "TimerWrapper::Callback completed\n");

#if 0
	// talmage: You can't do this.  The callback, above, deletes this
	// TimerWrapper.  The validity of currentThreadEnv is undefined.
	// Most of the time, it seems to be OK.  Occasionally,
	// dereferencing currentThreadEnv results in SIGSEGV.

    jthrowable exc;
	exc = currentThreadEnv->ExceptionOccurred();

	if (exc) {
         currentThreadEnv->ExceptionDescribe();
         currentThreadEnv->ExceptionClear();
		 PLOG(PL_FATAL, "Can't generate timer Callback\n");
		 throwException(currentThreadEnv, "TimerWrapper: getAddress() - Can't invoke callback, unknown error, FATAL\n");
		 }		
#endif

	PLOG(PL_DEBUG, "TimerWrapper::generateCallback exiting\n");
	
	return true;
}
