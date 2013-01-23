To autodetect whether a simulation is MANET or WIRED we use the "info vars" on ns-2
variable to search for variables that are specific to Manet.

For MANET this gives:

Multicast INFO VARS TEST = ifqType_ propType_ nconn_ agentTrace_
channel_ movementTrace_ routingAgent_ EotTrace_ mobile_ip_
rtAgentFunction_ annotationSeq_ id macTrace_ nullAgent_ inerrProc_
slinks_ link_ WirelessNewTrace_ routingTable_ RouterTrace_
namWx_ allocAddr_ AgentTrace_ scheduler_ useasim_ AddressFormat_
MovementTrace_ chan TaggedTrace_ nsv1flag nsflows_ namWy_
outerrProc_ sflows_ FECProc_ packetManager_ McastAddr_ macType_
node_factory_ IMEPFlag_ Node_ started_ color_ traceAllFile_
MacTrace_ namNeedsW_ llType_ phyType_ namtraceAllFile_
nix-routing antType_ propInstance_ McastBaseAddr_ propInstCreated_
ifqlen_ topoInstance_ PhyTrace_ addressType_ routerTrace_

and for Wired, this gives:

Multicast INFO VARS = packetManager_ annotationSeq_ nconn_ allocAddr_
Node_ started_ MrtHandle_ useasim_ scheduler_ namNeedsW_ nullAgent_
slinks_ nsflows_ multiSim_ link_ sflows_ routingTable_
