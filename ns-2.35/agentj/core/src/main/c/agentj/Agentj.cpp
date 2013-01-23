#include "Agentj.h"
#include <typeinfo>
#include <stdlib.h>
#include "protoDebug.h"

#define TCL_COMMAND_BUFFER 100

// NOTE: evalf was crashing on snow leopard with a malloc, variable freed was 
// not allocated ecxeption. did a workaround by copying everything into a buffer
// and then used the eval method instead.  This is the size of the input/output 
// buffer for those calls.

AgentjVirtualMachine *Agentj::agentJVM=0;

static class AgentJInstantiator : public TclClass
{
    private:
        Agentj* agent;
    public:
        AgentJInstantiator() : TclClass("Agent/Agentj") {}
        TclObject *create(int argc, const char*const* argv)
        {
            return new Agentj();
        }
} class_protean_example;


Agentj::Agentj() : routerAgent(NULL)
{
//agentJVM=NULL;
    PLOG(PL_INFO, "Agentj: Being Created ...\n");
}


void Agentj::initAgent()
{
    PLOG(PL_INFO, "InitAgent Entering\n");
    printf("InitAgent Entering\n" );


    PLOG(PL_TRACE, "Agentj: My pointer is ... %i\n", this);

    Tcl& tcl = Tcl::instance();

    agentref = dynamic_cast<Agent*>(this);
    tcl.evalf("%s set node_", agentref->name());
    const char* nodeName = tcl.result();

    strcpy(tclNodeName, nodeName);

    PLOG(PL_TRACE, "Agentj: My Ns2 Agent's TCL Name is ... %s\n", tclNodeName);
		
	
	printf("Agentj: Simulator instance name is ... %s\n", invokeGlobalTCLCommand("Simulator instance"));

    PLOG(PL_INFO, "InitAgent Exiting\n");
}


void Agentj::SetDebugLevel(unsigned int level)
{
	::SetDebugLevel(level);
// Invalid access of stack red zone b068eff4 eip=4d6f8a7d - can't get to this?
}


Agentj::~Agentj()
{
    PLOG(PL_TRACE, "Agentj: destructor ...\n");
    if (agentJVM!=0)                              // create a virutal machine for our Java broker...
        agentJVM->finish();
}

const char* Agentj::invokeGlobalTCLCommand(const char* cmd)
{
    if (agentref==NULL)
    {
        PLOG(PL_FATAL, "Agent is not initialised - You need to use \"init\" command at start up in TCL\n in order to use AgentJ agents");
        exit(1);
    }
	
    Tcl& tcl = Tcl::instance();
    
	PLOG(PL_TRACE, "Agentj: about to evaluate globally %s\n", cmd);
	
    tcl.evalf("%s", cmd);
	
    return tcl.result();
}

const char* Agentj::invokeTCLCommand(const char* cmd)
{
    if (agentref==NULL)
    {
        PLOG(PL_FATAL, "Agent is not initialised - You need to use \"init\" command at start up in TCL\n in order to use AgentJ agents");
        exit(1);
    }

    Tcl& tcl = Tcl::instance();
    
	PLOG(PL_TRACE, "Agentj: about to evaluate %s\n", cmd);

    tcl.evalf("%s %s", tclNodeName, cmd);

    return tcl.result();
}


bool Agentj::OnStartup(int argc, const char*const* argv)
{
    PLOG(PL_INFO, "Startup Called for Agentj object\n");
    initAgent();
    return true;
}


void Agentj::OnShutdown()
{
    PLOG(PL_INFO, "Shutdown Called for Agentj object\n");
    agentJVM->finish();
    agentJVM->invokeCommand(this, "shutdown", "please");
// execute shutdown on Java agent command
}


bool Agentj::ProcessCommands(int argc, const char*const* argv)
{
    if (argc == 3)
        PLOG(PL_INFO, "COMMAND IN AGENTJ AGENT: %s %s\n", argv[1], argv[2]);
    else if (argc == 2)
        PLOG(PL_INFO, "COMMAND IN AGENTJ AGENT: %s\n", argv[1]);

    if (strcmp(argv[1], "agentj")==0)             // 2 is command, 3 onwards are the arguments
    {
        int i;
        int charsNeeded=0;
        char *args=NULL;

// Put ALL arguments back in a String - gives us much more flexibility about how we deal
// with them on the Java side of things

        for (i=3; i< argc; ++i)
            charsNeeded += strlen(argv[i]) + 1;   // 1 for a space

        if (charsNeeded != 0)
        {
            args=new char[charsNeeded];
            char *ptr=args;

            for (i=3; i< argc; ++i)
            {
                if (i==argc-1)
                    sprintf(ptr, "%s", argv[i]);
                else
                {
                    sprintf(ptr, "%s ", argv[i]);
                    ptr += strlen(argv[i]) + 1;
                }
            }
        }
        else                                      // send "NONE" - need to send something ....
        {
            args=new char[1];
            sprintf(args, "");
        }

        PLOG(PL_INFO, "AGENTJ COMMAND: %s, Arguments = %s\n", argv[2], args);

        if (agentJVM==0)                          // No class path set so bomb out...
        {
            PLOG(PL_FATAL, "Agentj.cpp: ERROR in CLASSPATH, check your environment\n");
            return false;
        }
                                                  // execute command
        bool ok = agentJVM->invokeCommand(this, argv[2], args);
// this calls calls the invokeCommand in the Agentj.cpp class which in turn
// invokes the method in the Agentj.java class which routes the
// call to the appropriate Java object
        delete[] args;
        if (ok) return true;
    }
    if (strcmp(argv[1], "router")==0)             // 2 is command, 3 onwards are the arguments
    {
        if (argc < 3 || routerAgent == NULL)
            return false;

        bool ok = routerAgent->ProcessCommands(argc-2, argv+2);
        if (ok) return true;
    }
    else if (3 == argc)
    {
        if (!strcmp(argv[1], "attach-agentj"))    // 2 is classpath, 3 is classname
        {
            PLOG(PL_INFO, "Agentj: Attach-agent\n");
            if (agentJVM==NULL)                   // create a virtual machine ...
            {
                PLOG(PL_INFO, "Agentj: Creating JVM\n");
                                                  // get classpath from CLASSPATH variable
                agentJVM=new AgentjVirtualMachine();
                PLOG(PL_INFO, "Agentj: Created Java Virtual Machine...\n");
            }

                                                  // create Java object and use this
            agentJVM->attachJavaAgent(argv[2], this, getLocalAddress());
// agents pointer as its ID.
            PLOG(PL_INFO, "Agentj: Attached Agent %s ok \n", argv[2]);
            return true;
        }
        else if (!strcmp(argv[1], "setDelimiter"))// 2 is the delimiter used to divide the instructions
        {
            if (agentJVM==0)                      // No class path set so bomb out...
            {
                PLOG(PL_ERROR,"Agentj.cpp: ERROR no classpath set - use setClass command\n");
                return false;
            }
                                                  // execute command
            bool ok= agentJVM->invokeCommand(this, argv[1], argv[2]);
            if (ok) return true;
        }
        else if (strcmp(argv[1], "port-dmux") == 0)
        {
            PortClassifier *dmux = (PortClassifier *)TclObject::lookup (argv[2]);

            if (dmux == 0)
            {
                fprintf (stderr, "%s: %s lookup of %s failed\n", __FILE__, argv[1], argv[2]);
                return false;
            }
            else
            {
                if (routerAgent!=NULL)
                    routerAgent->setPortClassifier(dmux);
            }
            PLOG(PL_DEBUG, "Agentj: Got port-dmux: %s\n", argv[2]);
            return true;
        }
        else if (strcmp(argv[1], "log-target") == 0 || strcmp(argv[1], "tracetarget") == 0)
        {
            Trace *tracetarget = (Trace *)TclObject::lookup (argv[2]);
            if (tracetarget == 0)
            {
                fprintf (stderr, "%s: %s lookup of %s failed\n", __FILE__, argv[1], argv[2]);
                return false;
            }
            else
            {
                if (routerAgent!=NULL)
                    routerAgent->setTraceTarget(tracetarget);
                logtarget_ = tracetarget;
            }
            return true;
        }
        else if (strcmp(argv[1], "drop-target") == 0)
        {
            NsObject *droptarget = (NsObject *)TclObject::lookup (argv[2]);
            if (droptarget == 0)
            {
                fprintf (stderr, "%s: %s lookup of %s failed\n", __FILE__, argv[1], argv[2]);
                return false;
            }
            else
            {
                drop_ = droptarget;
                if (routerAgent!=NULL)
                    routerAgent->setDropTarget(drop_);
            }
            return true;
        }
        else if (strcmp(argv[1], "setNativeAgentRouter") == 0)
        {
            char routerval[60];
            sprintf(routerval, "eval new %s", argv[2]);
            Tcl& tcl = Tcl::instance();
            tcl.evalf(routerval);

            const char *router = tcl.result();

            if (router==NULL)
            {
                PLOG(PL_FATAL, "Could not find router agent class - %s\n", argv[2]);
                return false;
            }

            routerAgent = (AgentJRouter *)tcl.lookup(router);
            routerAgent->setAgentJAgent(this);

            routerAgent->target(target_);
            routerAgent->setDropTarget(drop_);

            return true;
        }
        else if (strcmp(argv[1], "setNativeRouterPort") == 0)
        {
            int port = atoi(argv[2]);

            if (routerAgent!=NULL)
                routerAgent->setRoutingProtocolPort(port);

            return true;
        }
    }
    else if (2 == argc)
    {
        if (!strcmp(argv[1], "initAgent"))        // init is a reserved word so cannot use it ...
        {
            PLOG(PL_WARN, "Agentj: initAgent called - this is DEPRECATED. Use \"startup\" command instead\n");
            initAgent();
            return true;
        }
        else if (strcmp(argv[1], "getNativeAgentRouter") ==0)
        {
            Tcl& tcl = Tcl::instance();
            tcl.resultf("%s", routerAgent->name());
            return true;
        }
        else if (!strcmp(argv[1], "printTime"))
        {
            struct timeval currentTime;
            double now;                               // current time in seconds

            GetSystemTime(currentTime);

            now = currentTime.tv_sec + currentTime.tv_usec/1.0e06;

            printf("AgentJ C++ Agent, Call to printTime-----> getSystemTime returning current time %f.6 (sec)\n", now);
            return true;
        }
    }

    return false;
}                                                 // end Agentj::command()


void Agentj::recv(Packet *p,Handler *handle)      //for routing agent ...
{
    if (routerAgent!=NULL)
        routerAgent->receivePacket(p,handle);
}


jmethodID Agentj::getMethodID(const char *method, const char *sig)
{
    jmethodID mid;

    jclass cls = env->GetObjectClass(javaAgent);

    if (cls == NULL)
    {
        PLOG(PL_FATAL, "Agentj: Fatal, can't find %s\n", method);
        exit(1);
    }

    mid = env->GetMethodID(cls, method, sig);

    if (mid == 0)
    {
        PLOG(PL_FATAL, "Agentj: Fatal, can't find %s\n", method);
        exit(1);
    }
    return mid;
}


//	mid = getMethodID(cls, "attachJavaAgent", "([Ljava/lang/String;)I");

// other helper methods

/**
 * returns the current value of the pointer to the NS2
 * agent that is currently using this NI interface to PAI
 * - used by some functions here.
 */
Agentj *getAgentjPtr(JNIEnv *env, jobject obj)
{
    jclass cls = env->GetObjectClass(obj);
    jfieldID fid;

    PLOG(PL_DETAIL, "Agentj.cpp: getAgentjObject Entering\n ");

    fid = env->GetFieldID(cls, "_nsAgentPtr", "J");

    if (fid==0)
    {
        PLOG(PL_FATAL, "Agentj.cpp: getAgentJObject() - ERROR, Can't find _nsAgentPtr variable .... \n");
        exit(0);
    }

    jlong val = env->GetLongField(obj,fid);

    return static_cast<Agentj*>((Agentj *)val);
}


/*
 * Class:     agentj_AgentJNode
 * Method:    tclEvalf
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_agentj_AgentJAgent_tclEvaluateOnAgent
(JNIEnv *env, jobject obj, jstring cmd)
{

    PLOG(PL_DEBUG, "Java_agentj_AgentJAgent_tclEvaluateOnAgent entering \n");

    Agentj *agentjPtr = getAgentjPtr(env,obj);

    const char *cstring = env->GetStringUTFChars(cmd, 0);

    PLOG(PL_DETAIL, "Java_agentj_AgentJAgent_tclEvaluateOnAgent, Command = %s\n", cstring);

    const char* nativeresult = agentjPtr->invokeTCLCommand(cstring);

    env->ReleaseStringUTFChars(cmd, cstring);

	char ouputString[TCL_COMMAND_BUFFER];
	
    sprintf(ouputString, "%s", nativeresult);
	
    jstring result = env->NewStringUTF(ouputString);

    PLOG(PL_DEBUG,"Java_agentj_AgentJAgent_tclEvaluateOnAgent returning = %s\n", ouputString);

    return result;
}


/*
 * Class:     agentj_AgentJNode
 * Method:    getAgentTCLName
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_agentj_AgentJAgent_getAgentTCLName
(JNIEnv *env, jobject obj)
{

    PLOG(PL_DEBUG, "Java_agentj_AgentJAgent_getAgentTCLName entering \n");

    Agentj *agentjPtr = getAgentjPtr(env,obj);

    const char* nativeresult = agentjPtr->name();
	
	char ouputString[TCL_COMMAND_BUFFER];
		
    sprintf(ouputString, "%s", nativeresult);
	
    PLOG(PL_DEBUG,"Java_agentj_AgentJAgent_getAgentTCLName returning = %s\n", ouputString);

    return env->NewStringUTF(ouputString);
}


JNIEXPORT jstring JNICALL Java_agentj_AgentJAgent_tclEvaluateOnSimulator
(JNIEnv *env, jclass, jstring cmd)
{

    PLOG(PL_DEBUG, "Java_agentj_AgentJAgent_tclEvaluateOnSimulator, entering \n");

	const char *cstring = env->GetStringUTFChars(cmd, 0);

	PLOG(PL_DEBUG, "Java_agentj_AgentJAgent_tclEvaluateOnSimulator, invoking command %s\n", cstring);

    Tcl& tcl = Tcl::instance();	
	
	if (&tcl==NULL) {
		PLOG(PL_DEBUG, "Java_agentj_AgentJAgent_tclEvaluateOnSimulator - null tcl object \n");
	}
	
	tcl.eval("Simulator instance");

	PLOG(PL_DEBUG, "Java_agentj_AgentJAgent_tclEvaluateOnSimulator, got a result \n");

    char inputString[TCL_COMMAND_BUFFER];

	PLOG(PL_DEBUG, "1 \n");

    sprintf(inputString, "%s %s", tcl.result(), cstring);

    PLOG(PL_DETAIL, "Java_agentj_AgentJAgent_tclEvaluateOnSimulator, Command = %s\n", inputString);

	tcl.eval((const char *)inputString);

    PLOG(PL_DEBUG, "Java_agentj_AgentJAgent_tclEvaluateOnSimulator, got tcl result \n");

    char nativeresult[TCL_COMMAND_BUFFER];
    strncpy(nativeresult, tcl.result(), TCL_COMMAND_BUFFER);

	//    const char* nativeresult = tcl.result();

    env->ReleaseStringUTFChars(cmd, cstring);

    PLOG(PL_DEBUG, "Java_agentj_AgentJAgent_tclEvaluateOnSimulator, returing %s \n", nativeresult);

    return env->NewStringUTF(nativeresult);
}


/*
 * Class:     agentj_AgentJNode
 * Method:    tclEvaluate
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_agentj_AgentJAgent_tclEvaluate
(JNIEnv *env, jclass, jstring cmd)
{
	
   PLOG(PL_DEBUG, "Java_agentj_AgentJAgent_tclEvaluate, entering \n");

    Tcl& tcl = Tcl::instance();

    const char *cstring = env->GetStringUTFChars(cmd, 0);

	char inputString[TCL_COMMAND_BUFFER];
	
	PLOG(PL_DEBUG, "1 \n");
	
    sprintf(inputString, "%s", cstring);
	
    PLOG(PL_DETAIL, "Java_agentj_AgentJAgent_tclEvaluate, Command = %s\n", cstring);

    tcl.eval((const char *)inputString);

    char nativeresult[TCL_COMMAND_BUFFER];
    strncpy(nativeresult, tcl.result(), TCL_COMMAND_BUFFER);

    env->ReleaseStringUTFChars(cmd, cstring);

    PLOG(PL_DEBUG, "Java_agentj_AgentJAgent_tclEvaluate, returing %s \n", nativeresult);

    return env->NewStringUTF(nativeresult); 
}


/*
 * Class:     agentj_AgentJNode
 * Method:    getSystemTime
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL Java_agentj_AgentJAgent_getSystemTime(JNIEnv *env, jobject obj)
{

    PLOG(PL_DEBUG, "Java_agentj_AgentJAgent_getSystemTime: getSystemTime, entering \n");

    Agentj *agentjPtr = getAgentjPtr(env,obj);

    struct timeval currentTime;
    double now;                                   // current time in seconds

    agentjPtr->GetSystemTime(currentTime);

    now = currentTime.tv_sec + currentTime.tv_usec/1.0e06;

    PLOG(PL_DEBUG, "Java_agentj_AgentJAgent_getSystemTime: getSystemTime returning current time %f.6 (sec)\n", now);

    return (now);
}


/*
 * Class:     agentj_AgentJNode
 * Method:    setDebugLevel
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_agentj_AgentJAgent_setNativeDebugLevel
(JNIEnv *env, jobject obj, jint level)
{

    PLOG(PL_DEBUG, "Java_agentj_AgentJAgent_setNativeDebugLevel - changed debug level to %i\n", (int)level);

//    Agentj *agentjPtr = getAgentjPtr(env,obj);

//	agentjPtr->SetDebugLevel(level);

    SetDebugLevel(level);

/*	if (level == agentj_AgentJNode_PL_FATAL)
        SetDebugLevel(PL_FATAL);
    else if (level == agentj_AgentJNode_PL_ERROR)
        SetDebugLevel(PL_ERROR);
    else if (level == agentj_AgentJNode_PL_WARN)
        SetDebugLevel(PL_WARN);
    else if (level == agentj_AgentJNode_PL_INFO)
        SetDebugLevel(PL_INFO);
    else if (level == agentj_AgentJNode_PL_DEBUG)
        SetDebugLevel(PL_DEBUG);
    else if (level == agentj_AgentJNode_PL_TRACE)
SetDebugLevel(PL_TRACE);
else if (level == agentj_AgentJNode_PL_DETAIL)
SetDebugLevel(PL_DETAIL);
else if (level == agentj_AgentJNode_PL_MAX)
SetDebugLevel(PL_MAX); */
}


void throwException(JNIEnv *env, const char *message)
{

    jclass newExcCls;
    newExcCls = env->FindClass("java/lang/Exception");
    if (newExcCls == NULL)
    {
/* Unable to find the exception class, give up. */
        return;
    }
    env->ThrowNew(newExcCls, message);
}


/*
 * Class:     agentj_AgentJAgent
 * Method:    storeReference
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_agentj_AgentJAgent_storeReference
(JNIEnv* env, jobject obj)
{
    Agentj *agentjPtr = getAgentjPtr(env,obj);

    if (obj == NULL)
    {
        PLOG(PL_FATAL, "routerAgent is NULL\n");
        abort();
    }

    agentjPtr->getRouterAgent()->setJavaRouter(env->NewGlobalRef(obj));

}
