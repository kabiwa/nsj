/*
 *  JAVMTimer.cpp
 *  AgentJ NativeCode
 *
 * Implements the hooks into the protlib timers for creting timers that callback to Java upon a timeout
 *
 *  Created by Ian Taylor on 22/12/2007.
 *
 */

#include "agentj_nativeimp_ProtolibTimer.h"
#include "TimerWrapper.h"

/**
 * @return the JNI field identifier for the _timerPtr field within the calling Java object
 */
jfieldID getTimerWrapperPtrFieldID(JNIEnv *env, jobject obj) {
	  jclass cls = env->GetObjectClass(obj);
	  jfieldID fid;

	  PLOG(PL_DETAIL, "helpermethods: getTimerWrapperPtrFieldID Entering\n ");
	  
	  fid = env->GetFieldID(cls, "_timerPtr", "J");
	  
	  if (fid==0) {
	      PLOG(PL_FATAL, "helpermethods: geTimerMethodID() - ERROR, Can't find _timerPtr variable .... \n");
		  exit(0);
		  }
	  return fid;
	  }
	  
	  
/**
 * @return the pointer to the TimerWrapper object that contains the native implementation of this timer.
 * This is retrieved from the _timerPtr value within the calling Java object, which is set upon construction
 * of the native timer wrapper object.
 */
TimerWrapper *getTimerWrapperPtr(JNIEnv *env, jobject obj) {
	jfieldID fid=getTimerWrapperPtrFieldID(env, obj);
	jlong ptr = env->GetLongField(obj,fid);
	return (TimerWrapper *)ptr;
} 

/*
 * Class:     javm_nativeimp_ProtolibTimer
 * Method:    createTimer
 * Signature: (DI)V
 * REPEAT is NOT implemented HERE !!!!  not needed at time of writing - complain to Ian if you get this far ...
 */
JNIEXPORT void JNICALL Java_agentj_nativeimp_ProtolibTimer_createNativeTimer
  (JNIEnv *env, jobject javaobj, jdouble interval, jint repeat) {

	AgentjVirtualMachine::getVM(env);
	
	Agentj *agentjPtr = getAgentjPtr(env, javaobj); // gets the pointer to the C++ agent from 
													// the Ns2SocketImpl class's _brokerPtr variable
													// set at construction

	PLOG(PL_DETAIL, "Native createTimer - Creating Wrapper for Timer now\n");
	TimerWrapper *wrapper = new TimerWrapper(env, javaobj, agentjPtr, interval);
	PLOG(PL_DETAIL, "Native createTimer - Creatied Native Wrapper for Timer =  %l\n", wrapper);
	PLOG(PL_DETAIL, "Native createTimer - Creatied Native Wrapper for Java ProtolibTimer =  %i\n", javaobj);
	PLOG(PL_DETAIL, "Native createTimer - JNIEnv is =  %i\n", env);
	
	jfieldID fid=getTimerWrapperPtrFieldID(env, javaobj);
	
	if (fid==0) {
		PLOG(PL_FATAL, "Native createTimer - Can't set Timer pointer in Java");
		exit(0);
	}
	
	env->SetLongField(javaobj,fid, (long)wrapper);

	PLOG(PL_DETAIL, "Native CreateTimer Leaving\n");
	}

/*
 * Class:     javm_nativeimp_ProtolibTimer
 * Method:    startTimer
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_agentj_nativeimp_ProtolibTimer_startNativeTimer
  (JNIEnv *env, jobject javaobj) {
	PLOG(PL_DETAIL, "Native StartTimer Entering\n");
	TimerWrapper *timer = getTimerWrapperPtr(env, javaobj); 
	
	if (timer!=NULL) {
		PLOG(PL_DETAIL, "Native StartTimer --> starting timer\n");
		timer->startTimer();
		}
	else {
		PLOG(PL_ERROR, "Native StartTimer --> No timer to start !!!!!!!!!!!!!!!\n");
	}
	
	PLOG(PL_DETAIL, "Native StartTimer Exiting\n");
  }


/*
 * Class:     javm_nativeimp_ProtolibTimer
 * Method:    deleteTimer
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_agentj_nativeimp_ProtolibTimer_deleteNativeTimer
  (JNIEnv *env, jobject javaobj) {
	PLOG(PL_DETAIL, "Native deleteTimer Entering\n");
	
	TimerWrapper *timer = getTimerWrapperPtr(env, javaobj); 

	PLOG(PL_DETAIL, "Native deleteTimer, timer = %i \n", timer);
	
	delete timer;
	PLOG(PL_DETAIL, "Native deleteTimer Exiting\n");
  }

/*
 * Class:     javm_nativeimp_ProtolibTimer
 * Method:    cancelTimer
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_agentj_nativeimp_ProtolibTimer_cancelNativeTimer
(JNIEnv *env, jobject javaobj) {
	PLOG(PL_DETAIL, "Native cancelTimer Entering\n");
	
	TimerWrapper *timer = getTimerWrapperPtr(env, javaobj); 
	
	if (timer!=NULL) {
		PLOG(PL_DETAIL, "Native cancelTimer --> starting timer\n");
		timer->cancelTimer();
	}
	else {
		PLOG(PL_ERROR, "Native cancelTimer --> No timer to start !!!!!!!!!!!!!!!\n");
	}

	PLOG(PL_DETAIL, "Native cancelTimer Exiting\n");
}
