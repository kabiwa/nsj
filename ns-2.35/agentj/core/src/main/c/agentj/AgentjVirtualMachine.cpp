/*
 *  AgentjVirtualMachine.cpp
 *  PAI
 *
 *  Created by Ian Taylor on Fri Mar 26 2004.
 *
 */

#include "AgentjVirtualMachine.h"
#include "Agentj.h"

#define JAVA_AGENTJ_VM_CLASS "agentj/AgentJVirtualMachine"
#define DEBUG_CONFIG "native.logging.properties"
#define CONFIG_DIR "config"
#define MODULE_DIR "modules"
#define INSTRUM_DIR "instrumentation"
#define TARGET_DIR "target"
#define CLASS_PATH "class.path"
#ifdef _WIN32
#define FILE_SEP "\\" 
#define PATH_SEP ";"
#else // UNIX 
#define PATH_SEP ":"
#define FILE_SEP "/"
#endif

class Agentj;
 
/** 
 * Create a virtual machine 
 */
AgentjVirtualMachine::AgentjVirtualMachine() {
	PLOG(PL_INFO, "AgentjVirtualMachine: Entering\n");

	jint res;
	char *cpinst;
	int debuglevel;
	char *debugFileName;
	char *cpFileName;
	//char *agentinstrumentstring;
	FILE *debugFile;
	char str[20];

	PLOG(PL_DEBUG, "initVM: Setting up JVM\n");

	char *ld_library_path = getenv ("LD_LIBRARY_PATH");
	char *agentJHome = getenv ("AGENTJ");

	if (ld_library_path==NULL) {
	    PLOG(PL_ERROR, "AgentjVirtualMachine: FATAL!! LD_LIBRARY_PATH is not set, can't continue.... See the Manual\n");
		exit(1);
	}

	if (agentJHome==NULL) { // Every good code needs a home
		printf("AGENTJ environment variable not set - you must set this to use AgentJ.  See the manual\n");
		exit(1);
    }

	debugFileName= new char[strlen(agentJHome) + strlen(CONFIG_DIR) + 2 + strlen(DEBUG_CONFIG) + 1];
	sprintf(debugFileName, "%s%s%s%s%s", agentJHome, FILE_SEP, CONFIG_DIR, FILE_SEP, DEBUG_CONFIG);

    PLOG(PL_DEBUG, "native debug file = %s\n", debugFileName);

    debugFile = fopen (debugFileName,"r");
    fscanf (debugFile, "%s", str);

    char *p;
	
	p = strtok (str,"="); // property name
	p = strtok (NULL,"="); // last string

	if (p==NULL) {
		PLOG(PL_ERROR, "Cannot read from %s or format is incorrect\n", debugFileName);
		exit(0);
	}
    printf("Setting Logging LEVEL to = %s\n", p);
	
    debuglevel = atoi(p);
    printf("Logging LEVEL is set to = %i\n", debuglevel);
	
	SetDebugLevel(debuglevel); // SET DEBUG LEVEL in Agentj/config/nativedebug.txt

    fclose (debugFile);

	/*
	GET classpath.
	The class path is defined relatively to the $AGENTJ env variable. Each path should be on a new line
	and the file should end with a new line.
	*/
	cpFileName= new char[strlen(agentJHome) + strlen(CONFIG_DIR) + 2 + strlen(CLASS_PATH) + 1];
	sprintf(cpFileName, "%s%s%s%s%s", agentJHome, FILE_SEP, CONFIG_DIR, FILE_SEP, CLASS_PATH);

    FILE *cpFile = fopen ( cpFileName, "r" );
	char cpLines [2048] = "-Djava.class.path=";
	if ( cpFile != NULL ) {
		char line [128];
		while ( fgets ( line, sizeof line, cpFile ) != NULL ) {
			char *newline = strchr ( line, '\n' );
			if ( newline != NULL ) {
				*newline = '\0';
			}
			newline = strchr ( line, '\r' );
			if ( newline != NULL ) {
				*newline = '\0';
			}
			int len = strlen(line);
			if(len == 0) {
				continue;
			}
			if(PATH_SEPARATOR == ';') {
			    // we're on windows. swap out file separators
			    for(int i = 0; i < len; i++) {
			        if(line[i] == '/') {
			            line[i] = '\\';
                    }
                }
            }
			char *comment = line;
			char hash = *comment;
			char expanded[200];
			if(hash != '#') {
			    sprintf(expanded, "%s", line);
			    strcat(expanded, PATH_SEP);
				strcat(cpLines,expanded);
			}
		}
		strcat(cpLines,".");
		fclose (cpFile);
	} else {
		perror ( cpFileName );
	}

    // instrumentation agent

    // int sep = strlen(FILE_SEP);

	//agentinstrumentstring = new char[strlen("-javaagent:") + strlen(agentJHome) + sep +
	//            strlen(MODULE_DIR) + sep + strlen(INSTRUM_DIR) + sep + strlen(TARGET_DIR)
	//            + sep + strlen("instrumentation-0.1.jar") + 1];
	//sprintf(agentinstrumentstring, "-javaagent:%s%s%s%s%s%s%s%sinstrumentation.jar", agentJHome, FILE_SEP, MODULE_DIR,
	//            FILE_SEP, INSTRUM_DIR, FILE_SEP, TARGET_DIR, FILE_SEP);

	JavaVMInitArgs vm_args;
   	JavaVMOption options[5];

	cpinst = new char[strlen("-Djava.library.path=") + strlen(ld_library_path) + 1];
	sprintf(cpinst, "-Djava.library.path=%s", ld_library_path);

	options[0].optionString =  cpinst;
    options[1].optionString =  "-Xmx512m";
	options[2].optionString = "-Xms100m";
	options[3].optionString = cpLines;
	options[4].optionString = "-Djava.system.class.loader=agentj.instrument.AgentJClassLoader";
   //options[5].optionString =  "-Xcheck:jni";
   //options[6].optionString =  "-verbose:jni";

    vm_args.version = 0x00010004;
	vm_args.options = options;
	vm_args.nOptions = 5;

	vm_args.ignoreUnrecognized	= JNI_TRUE;

	PLOG(PL_DEBUG, "AgentjVirtualMachine: Creating JVM \n");
    int i = 0;
    PLOG(PL_DEBUG, "JavaVM args:\n");
    for (i = 0; i < vm_args.nOptions; i++)
      printf("    option[%2d] = '%s'\n", i, vm_args.options[i].optionString);
      
	/* Create the Java VM */
	res = JNI_CreateJavaVM(&jvm, (void**)&env, &vm_args);
	if (res < 0) {
		env->ExceptionDescribe();
        env->ExceptionClear();
		PLOG(PL_ERROR, "AgentjVirtualMachine: Fatal, can't create Java VM\n");
		exit(0);
    }
	
	delete []cpinst;
	delete []debugFileName;
	delete []cpFileName;
	//delete []agentinstrumentstring;
	
	  
	PLOG(PL_INFO, "AgentjVirtualMachine: Exiting\n");
}

JavaVM *AgentjVirtualMachine::getVM(JNIEnv *env) {
    PLOG(PL_DETAIL, "getVM() in SocketStore.cpp: Getting the JVM Reference from JNI\n");
	static JavaVM *vm=NULL;
	if (vm==NULL) {
		env->GetJavaVM(&vm);
		PLOG(PL_DEBUG, "AGENTJ successfully hacked java.net package :)\n");
	}
	
    PLOG(PL_DETAIL, "getVM() in SocketStore.cpp: Exiting\n");
	return vm;
	}

/**
 * Destroy a virtual machine
 */
AgentjVirtualMachine::~AgentjVirtualMachine() {
	jvm->DestroyJavaVM();
	PLOG(PL_INFO, "AgentjVirtualMachine: Deleting\n");
	}
/**
 * Registers an AgentJ object with the following - see header for info
 *
 * @param className java classmane for the class
 * @param ptr pointer to the C++ agent
 * @param nodeAddress the node address (i.e. number) for this node in Ns-2
 */		
int AgentjVirtualMachine::attachJavaAgent(const char *className, Agentj *ptr, const char *nodeAddress) {
	jmethodID mid;
	jstring applicationArg0;
	jstring applicationArg1;
	jstring applicationArg2;
	char *idString;
	jobjectArray args;
	jobject agentJagent;
	
	PLOG(PL_DEBUG, "AgentjVirtualMachine: attachJavaAgent: Entering \n");

	// unsigned long = 64 bits
	
	idString = new char[20];
	sprintf(idString, "%p", ptr);
	
	PLOG(PL_DETAIL, "AgentjVirtualMachine: attachJavaAgent: Getting Java VM class \n");

	jclass cls = getJavaVMClass();
	
	mid = getMethodID(cls, "attachJavaAgent", "([Ljava/lang/String;)Lagentj/AgentJAgent;");
		
	PLOG(PL_DEBUG, "AgentjVirtualMachine: attachJavaAgent: Creating method ref \n");

//     ARGUMENTS ARE PACKED AS FOLLOWS:
//     arg[0] = the java class name
//     arg[1] is its id (i.e. the pointer to the NS2 agent)

	applicationArg0 = env->NewStringUTF(className);
	applicationArg1 = env->NewStringUTF(idString);
	applicationArg2 = env->NewStringUTF(nodeAddress);

	if ((applicationArg0 == 0) || (applicationArg1 == 0) || (applicationArg2 == 0)) {
		PLOG(PL_ERROR, "AgentjVirtualMachine: Fatal, out of memory\n");
		return -1;
	}

	args = env->NewObjectArray(3, env->FindClass("java/lang/String"), NULL);
		
	if (args == 0) {
		PLOG(PL_ERROR, "AgentjVirtualMachine:attachJavaAgent Fatal, out of memory\n");
		exit(1);
		}

	env->SetObjectArrayElement(args, 0, applicationArg0);
	env->SetObjectArrayElement(args, 1, applicationArg1);
	env->SetObjectArrayElement(args, 2, applicationArg2);
		    
	PLOG(PL_DEBUG, "AgentjVirtualMachine: attachJavaAgent: Calling Java attachJavaAgent \n");

	agentJagent = env->CallStaticObjectMethod(cls, mid, args);

	PLOG(PL_DEBUG, "AgentjVirtualMachine: attachJavaAgent: Called Java attachJavaAgent \n");

	if ((agentJagent==0) || (env->ExceptionOccurred())) {
		PLOG(PL_DEBUG, "AgentjVirtualMachine.cpp:attachJavaAgent Fatal, ERROR in register function: registering %s\n", className);
		env->ExceptionDescribe();
		env->ExceptionClear();
		exit(1);
		}
	
	ptr->setJavaAgent(agentJagent);
	ptr->setJNIEnv(env);

	PLOG(PL_DEBUG, "AgentjVirtualMachine: Finishing \n");
   
    delete []idString;
	
	return 1;
}

/**
 * - see header for info
 */
int AgentjVirtualMachine::dettachJavaAgent(Agentj *ptr) {
	jmethodID mid;
	jclass cls = getJavaVMClass();
	jint ret;
	char *idString;
	jstring applicationArg;

	// unsigned int = 32 bits - 0 to 65535 but could be represented as a signed so allocated a couple more
	// to be on the safe side (6 needed, 10 allocated - trim if needed) i.e. -32768 to 32767
	
	idString = new char[10];
	sprintf(idString, "%p", ptr);

	mid = getMethodID(cls, "dettachJavaAgent", "(Ljava/lang/String;)I");

	applicationArg = env->NewStringUTF(idString);

	if (applicationArg == 0) {
		PLOG(PL_ERROR, "AgentjVirtualMachine:dettachJavaAgent Unregistering: Fatal, out of memory\n");
		return -1;
	}

	ret = env->CallStaticIntMethod(cls, mid, applicationArg);

	if (ret==0) {
		PLOG(PL_ERROR, "AgentjVirtualMachine.cpp:dettachJavaAgent- WARNING in unregister function. No Such Class\n");
		exit(1);
		}
		
	delete [] idString;

	return 1;
}

bool AgentjVirtualMachine::invokeCommand(Agentj *ptr, const char *command, const char *progArgs) {
	jmethodID mid;
	jstring applicationArg0;
	jstring applicationArg1;
	jstring applicationArg2;
	char *idString;
	jobjectArray args;
	jboolean invokedok;
	 
	PLOG(PL_DEBUG, "AgentjVirtualMachine: invokeCommand: entering\n");

	idString = new char[20];
	sprintf(idString, "%p", ptr);
		
	jclass cls = getJavaVMClass();

	mid = getMethodID(cls, "invokeCommand", "([Ljava/lang/String;)Z");
		        		
//     ARGUMENTS ARE PACKED AS FOLLOWS: 
//     arg[0] is its id (i.e. the pointer to the NS2 agent)
//     arg[1] the command to send to the java Object
//     arg[0] the arguments for the command 

	applicationArg0 = env->NewStringUTF(idString);
	applicationArg1 = env->NewStringUTF(command);
	applicationArg2 = env->NewStringUTF(progArgs);

	if ((applicationArg0 == 0) || (applicationArg1 == 0) || (applicationArg2 == 0)) {
		PLOG(PL_ERROR, "AgentjVirtualMachine: Fatal, out of memory\n");
		return -1;
	}

	args = env->NewObjectArray(3, env->FindClass("java/lang/String"), NULL);
		
	if (args == 0) {
		PLOG(PL_ERROR, "AgentjVirtualMachine: Fatal, out of memory\n");
		exit(1);
		} 

	env->SetObjectArrayElement(args, 0, applicationArg0);
	env->SetObjectArrayElement(args, 1, applicationArg1);
	env->SetObjectArrayElement(args, 2, applicationArg2);
		    
	PLOG(PL_DETAIL, "AgentjVirtualMachine: invokeCommand: calling Java method \n");

	invokedok = env->CallStaticBooleanMethod(cls, mid, args);

	if (invokedok == 0) {
		PLOG(PL_ERROR, "AgentjVirtualMachine: Fatal, invocation for worker failed - see exception\n");
		env->ExceptionDescribe();
		env->ExceptionClear();
		exit(1);
	}

	PLOG(PL_DETAIL, "AgentjVirtualMachine: invokeCommand: called Java method, state = %i \n", invokedok);

    delete[] idString;

	PLOG(PL_DEBUG, "AgentjVirtualMachine: invokerCommand: Exiting \n");

	return (bool)invokedok;
}
	
void AgentjVirtualMachine::finish() { 
	jmethodID mid;

	PLOG(PL_DEBUG, "AgentjVirtualMachine::Finish: entering \n");

	jclass cls = getJavaVMClass();

	mid = getMethodID(cls, "finish", "()V");
		    
	env->CallStaticVoidMethod(cls, mid);

	// leaving Java now
	cls = env->FindClass("java/lang/System");
	mid = env->GetStaticMethodID(cls, "exit", "(I)V");
	env->CallStaticIntMethod(cls, mid, 0);
	
	


    // this cleans up the objects from the Java side of things
	// we don;t need to call unregisterJClass() individually because
	// we keep no reference here - the unregisterJClass() function
	// is left in this implementation for possible future
	// use and completeness. 
	
	//jvm->DestroyJavaVM();
	PLOG(PL_DEBUG, "AgentjVirtualMachine::Finish: leaving \n");
}

jmethodID AgentjVirtualMachine::getMethodID(jclass cls, char *method, char *sig) {
	jmethodID mid;
   
	mid = env->GetStaticMethodID(cls, method, sig);
	if (mid == 0) {
		PLOG(PL_ERROR, "AgentjVirtualMachine: Fatal, can't find JavaAgent.register\n");
		return 0;
		}
	return mid;
}

jclass AgentjVirtualMachine::getJavaVMClass() {
	jclass cls;

	cls = env->FindClass(JAVA_AGENTJ_VM_CLASS);
	if (cls == 0) {
		env->ExceptionDescribe();
		env->ExceptionClear();
		PLOG(PL_ERROR, "AgentjVirtualMachine: Fatal, can't find agentj.AgentJVirtualMachine class\n");
		exit(1);
	}
   	 
	return cls;
}
