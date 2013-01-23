/*
 *  AgentJRouter.cpp
 *  AgentJ
 *
 *  Created by Ian Taylor, Ulrich Herberg on 18/10/2008.
 *  Copyright (c) 2008 __MyCompanyName__. All rights reserved.
 *
 */

#include "AgentJRouter.h"

static class AgentJRouterInstantiator : public TclClass {
	public:
		AgentJRouterInstantiator() : TclClass("Agent/AgentJRouter") {}
	 	TclObject *create(int argc, const char*const* argv) {
			return (new AgentJRouter());
			}
} protean_agentj_router;

AgentJRouter::AgentJRouter() : Agent(PT_TCP) {
	PLOG(PL_INFO, "AgentJRouter starting up\n");
}

AgentJRouter::~AgentJRouter() {
	if (javaRouter_ != NULL)
		env->DeleteGlobalRef(javaRouter_);
}

void AgentJRouter::setJavaRouter(jobject router){
	if (router == NULL){
		PLOG(PL_FATAL, "router is NULL\n");
		abort();
	}
	//javaRouter_ = router;
	javaRouter_ = router;
}

void AgentJRouter::setAgentJAgent(Agentj* agentjobj) {
	agentjref_ = agentjobj;
	if (agentjref_ == NULL){
		PLOG(PL_FATAL, "agentjref is NULL\n");
		abort();
	}

	env = agentjref_->agentJVM->getEnv();
}

nsaddr_t AgentJRouter::getNextHop(nsaddr_t destination) {
	jclass routerClass = env->GetObjectClass(javaRouter_);
	if (routerClass == NULL){
		PLOG(PL_FATAL, "routerClass not found\n");
		abort();
	}


        jmethodID methodID = env->GetMethodID(routerClass, "getNextHop", "(I)I");
	if (methodID == NULL){
		PLOG(PL_FATAL, "getNextHop method not found\n");
		abort();
	}

	PLOG(PL_DETAIL, "Calling getNextHop(%d) of Java agent\n", (int) destination);
        jint res = env->CallIntMethod(javaRouter_, methodID, (int)destination);
	if (env->ExceptionCheck()){
		env->ExceptionDescribe();
		env->ExceptionClear();
		abort();
	}
	env->DeleteLocalRef(routerClass);
	
	return (nsaddr_t) res;
}

void AgentJRouter::receivePacket(Packet *p,Handler *handle) {
  PLOG(PL_TRACE, "entering AgentJRouter::receivePacket\n");
  struct hdr_cmn *ch = HDR_CMN(p);
  struct hdr_ip *ih = HDR_IP(p);
  nsaddr_t saddr = ih->src_.addr_;
  nsaddr_t daddr = ih->daddr();

  PLOG(PL_TRACE,"%f : AgentJ::recv at %d : Packet received from %d to %d (port %d), forwards %d\n", Scheduler::instance().clock(), agentjref_->addr(), saddr, ih->daddr(), ih->dport(), ch->num_forwards());

  if(ih->dport() == routingProtocolPort_)
	#ifdef HAVE_AGENTJ_RP
	  ch->ptype_ = PT_AGENTJ; // set the packet type
	#else
	  ch->ptype_ = -1; // set the packet type	
	#endif
	
  if(ih->dport() == routingProtocolPort_ && saddr != agentjref_->addr() && (ch->num_forwards() == 1)) { // send on up to Agent
    ih->ttl_ -= 1;
    PLOG(PL_TRACE, "%f : AgentJ::recv is a control packet\n", Scheduler::instance().clock());
	Tcl& tcl = Tcl::instance();    
    tcl.evalf("%s %s %d", agentjref_->getTclNodeName(), "agent", routingProtocolPort_);
    const char* tclResult = tcl.result();
    Agent* theAgent = (Agent*)TclObject::lookup(tclResult);
    if(theAgent){
       theAgent->recv(p,0);
    } else {
       Packet::free(p);
       DMSG(0,"Agentj::recv theAgent is NULL!\n");
    }
    return;
  }


  else { // send packet back out interface
    /* check ttl */
    if (--ih->ttl_ == 0){
      PLOG(PL_TRACE, "Agent is dropping packet because of ttl\n");
      drop(p, DROP_RTR_TTL);
      return;
    }
    //check to see if its a udp packet or a broadcast packet
    if(daddr==(nsaddr_t)IP_BROADCAST && ih->dport()!=-1 && ch->num_forwards() == 0){//it's broadcast
      PLOG(PL_TRACE, "%f : Sending broadcast packet\n", Scheduler::instance().clock());
      Scheduler::instance().schedule(target_, p, 0.);
      return;
    }

    else { //it has single destination find and send on way
      if(ch->num_forwards_==0) {
       ih->saddr() = agentjref_->addr();
      }
      nsaddr_t nextHop=getNextHop(ih->daddr());
      if(nextHop!=INVALID && (nextHop!=ch->prev_hop_ || ch->num_forwards_==0)){ //make sure we don't send it back up
    	PLOG(PL_TRACE, "%f : Agent trying to forward it to %d\n",Scheduler::instance().clock(), nextHop);
       forward(p,nextHop);
       return;
      }
      if(nextHop==INVALID){
		PLOG(PL_TRACE, "%f : Agentj dropping packet because nextHop is INVALID couldn't find route to %d\n",Scheduler::instance().clock(), ih->daddr());
      } else if(nextHop==ch->prev_hop_) {
        PLOG(PL_TRACE, "Agentj dropping packet because nextHop is the same as the last hop which is %d\n",ih->daddr());
      } else {
        PLOG(PL_TRACE, "Agentj dropping packet because ch->num_forwards!= 0\n");
      }

	  drop(p,DROP_RTR_NO_ROUTE);
      
    } //end else of isbroadcast
  }
}


void AgentJRouter::forward(Packet *p, nsaddr_t nexthop, float delay) {
    struct hdr_cmn *ch = HDR_CMN(p);

    ch->prev_hop_ = agentjref_->addr();
    ch->next_hop_ = nexthop;
    ch->addr_type() = NS_AF_INET;
    ch->direction() = hdr_cmn::DOWN;

    PLOG(PL_TRACE, "%f : Agentj %d is attempting to forward packet %d to %d\n",Scheduler::instance().clock(),agentjref_->addr(), ch->uid_,nexthop);
    // Send the packet

    Scheduler::instance().schedule(target_, p, delay);
}

