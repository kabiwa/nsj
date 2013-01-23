#ifndef JAVMTimer_h
#define JAVMTimer_h

#include <jni.h>
#include "Agentj.h"
#include "protoDebug.h"

class AgentJTimer;

/*
 *  TimerWrapper.h
 *  AgentJNativeCode
 *
 *  Created by Ian Taylor on 05/01/2008.
 *  Copyright 2008 __MyCompanyName__. All rights reserved.
 *
 */

class TimerWrapper {	
public:	
    TimerWrapper(JNIEnv *theEnv, jobject theJavaObj, Agentj *nsAgent, double timerdelay);
	
	void startTimer(); 

	void cancelTimer(); 
	
	bool timerTriggered();

	~TimerWrapper();
			
private:
	double delay;

	JNIEnv *env; // the original interface pointer 
#if 0
	JNIEnv *currentThreadEnv; // the interface pointer for current thread 
#endif
	jobject javaObject; // the JNI reference to the Java object that made the invocation
	Agentj *nsAgentPtr;
	AgentJTimer *timer;
};

class AgentJTimer : public TimerHandler {
private:
	TimerWrapper *timerHandler_;
public:
	AgentJTimer(TimerWrapper *target) : TimerHandler() { timerHandler_ = target; }
protected:
	virtual void expire(Event *e) { timerHandler_->timerTriggered(); }
};

#endif
