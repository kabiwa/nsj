
#ifndef _AGENTJ
#define _AGENTJ

#undef INVALID
#define INVALID -1
#undef DROP
#undef RECV
#undef SEND
#undef FWRD

#include "nsProtoSimAgent.h"
#include "AgentjVirtualMachine.h"
#include "agentj_AgentJAgent.h"
#include "AgentJRouter.h"
#include "trace.h"

class AgentJRouter;

/**
 * Agentj is an NS2 agent for the PAI library, which can also
 * use the Java bridging interface to allow Java programs to send data
 * between NS2 nodes.
 * This is the central class for the NS2-P2PS integration which ties in
 * a JVM, the PAI interface, Protolib and the P2PS middleware which utlizes
 * all the levels.
 */
 class Agentj : public NsProtoSimAgent
{
	// Just in case anything is hidden ....
	
	using NsProtoSimAgent::OnStartup;
	using NsProtoSimAgent::ProcessCommands;
	using NsProtoSimAgent::OnShutdown;

	public:
	
        Agentj();
        ~Agentj();

        static AgentjVirtualMachine *agentJVM;

		char *getLocalAddress() {
		    char *buf= new char[50];
		    // sprintf(buf, "%li\0", (unsigned long)addr());
		    sprintf(buf, "%li", (unsigned long)addr());
			return buf;
			}
            
		bool OnStartup(int argc, const char*const* argv);

		bool ProcessCommands(int argc, const char*const* argv);

        void OnShutdown();
		
		void SetDebugLevel(unsigned int level);
		
		const char* invokeTCLCommand(const char* cmd); 
		const char* invokeGlobalTCLCommand(const char* cmd);
		
		void setJavaAgent(jobject agentref) { javaAgent=agentref; }
		void setJNIEnv(JNIEnv *jnienv) { env=jnienv; }

		void recv(Packet *p,Handler *handle);
		
		jmethodID getMethodID(const char *method, const char *sig); 

		char *getTclNodeName() { return tclNodeName; }
		
// Routing protocol

		nsaddr_t getNextHop(nsaddr_t destination);
	AgentJRouter* getRouterAgent(){
		return routerAgent;
	}
 			
    private:
		void initAgent(); // initialise the NS2 node
        void startTimer(double delay, int repeat);
		bool OnTxTimeout(ProtoTimer& theTimer);
		
		char tclNodeName[10];
		JNIEnv *env;
		
		char *myAddress;	
		Agent* agentref; // pointer cast as an agent (it is actually a reference to this)
		jobject javaAgent;
		AgentJRouter *routerAgent;
		Trace* logtarget_;
		
};  // end class


/**
 * Given the Java calling object, this method extracts the "_nsAgentPtr" class variable value,
 * which should be set to the pointer to the AgentJ C++ Ns2 Agent that created the Java object i.e.
 * the Ns2 agent that is instructing all of these commands within this context.
 */
Agentj *getAgentjPtr(JNIEnv *env, jobject obj); 

void throwException(JNIEnv *env, const char *message);

#endif // _AGENTJ
