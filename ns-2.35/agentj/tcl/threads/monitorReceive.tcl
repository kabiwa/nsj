# This example uses the Java PAICommands class to
# send data between two NS2 nodes.  The actual Java
# code specifies which nodes to communicate with.
# This simple example demonstrates how Java objects
# can be attached to an NS2 nodes and used to
# create sockets and send data between nodes.

# Create multicast enabled simulator instance
set ns_ [new Simulator]

# Create two nodes
set n1 [$ns_ node]
set n2 [$ns_ node]

# Put a link between them
$ns_ duplex-link $n1 $n2 64kb 100ms DropTail
$ns_ queue-limit $n1 $n2 100
$ns_ duplex-link-op $n1 $n2 queuePos 0.5
$ns_ duplex-link-op $n1 $n2 orient right


puts "Creating JavaAgent NS2 agents and attach them to the nodes..."
set p1 [new Agent/Agentj]
$ns_ attach-agent $n1 $p1

set p2 [new Agent/Agentj]
$ns_ attach-agent $n2 $p2

puts "In script: Initializing agents  ..."

$ns_ at 0.0 "$p1 initAgent"
$ns_ at 0.0 "$p2 initAgent"

puts "Setting Java Object to use by each agent ..."

$ns_ at 0.0 "$p1 attach-agentj agentj.examples.threads.MonitorReceive"
$ns_ at 0.0 "$p2 attach-agentj agentj.examples.threads.MonitorSend"

puts "Starting simulation ..."

$ns_ at 0.5 "$p1 agentj open"
$ns_ at 0.5 "$p1 agentj accept"

$ns_ at 5.0 "$p2 agentj open"
$ns_ at 7.0 "$p2 agentj send"

$ns_ at 10.0 "$p1 printTime"
$ns_ at 13.0 "$p1 printTime"
$ns_ at 17.0 "$p1 printTime"
$ns_ at 18.0 "$p1 printTime"
$ns_ at 19.0 "$p1 printTime"
$ns_ at 20.0 "finish $ns_"

proc finish {ns_} {
$ns_ halt
delete $ns_
}

$ns_ run

