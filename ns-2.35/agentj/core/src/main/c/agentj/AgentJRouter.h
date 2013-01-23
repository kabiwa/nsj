#ifndef _AGENTJROUTER
#define _AGENTJROUTER

/*
 *  AgentJRouter.h
 *  AgentJ
 *
 *  Created by Ian Taylor on 18/10/2008.
 *  Copyright (c) 2008 __MyCompanyName__. All rights reserved.
 *
 */

#include <agent.h>
#include "Agentj.h"

#include "classifier-port.h"
#include "trace.h"
#include <cmu-trace.h>
#include "protoDebug.h"

#undef INVALID
#define INVALID -1
#undef DROP
#undef RECV
#undef SEND
#undef FWRD

class Agentj;

class AgentJRouter : public Agent {
    public:
        AgentJRouter();
        ~AgentJRouter();

		void setAgentJAgent(Agentj* agentjobj);
		void setJavaRouter(jobject router);
		
		void setPortClassifier(PortClassifier *portClassifier) {
			portClassifier_=portClassifier;
		}
		
		void shutdown() {}
		virtual void setTraceTarget(Trace* trace) { logtarget_ = trace; }
		
		virtual void receivePacket(Packet *p,Handler *handle);
		virtual void forward(Packet *p, nsaddr_t nexthop, float delay=0.0);

		void setRoutingProtocolPort(nsaddr_t port) { routingProtocolPort_=port; }
		nsaddr_t getRoutingProtocolPort() { return routingProtocolPort_; }

		nsaddr_t getNextHop(nsaddr_t destination);
		virtual bool ProcessCommands(int argc, const char* const* argv){
			return false;
		}
		
    protected:
		PortClassifier *portClassifier_;
		nsaddr_t routingProtocolPort_;
		Trace* logtarget_;	
		Agentj* agentjref_; // pointer cast as an agent
		jobject javaRouter_;
		JNIEnv* env;
    private:
		jobject getCurrentJavaAgent();
};  // end class

#endif // _AGENTJROUTER
