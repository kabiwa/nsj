package agentj.examples.udprouting;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import agentj.AgentJAgent;
import agentj.AgentJVirtualMachine;
import agentj.examples.udprouting.repository.RoutingTuple;
import agentj.routing.AgentJRouter;

public class Agent extends AgentJAgent implements AgentJRouter{
	protected Router _node;
	private static Logger _logger =
		 Logger.getLogger(Agent.class.getName());
	
	public Agent(){
		super();
		try {
			LogManager.getLogManager().readConfiguration(
					new FileInputStream("logging.properties")
			);
		} catch (FileNotFoundException ex) {
			System.out.println(
			  "No logging properties found. Logging level set to: INFO"
			);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	/**
	 * This method is called by Ns2/agentj. It is the "entry" function for Ns2 
	 * to start the routing protocol.
	 */
	public boolean command(String command, String[] args) {
		if (command.equals("startProtocol")) {
			_logger.info("Agent starting LinkStateRouting... ");

			_node = new Router();
			_node.initialize();

			return true;
		} else {
			_logger.warning("UNKNOWN COMMAND: " + command + "; args: " + args);
		}

		return false;
	}
	
	/**
	 * Returns the nexthop (as Ns2 ID) for the given destination. This only works for
	 * IPv4, but could be easily rewritten to support IPv6.
	 */
	public int getNextHop(int destination) {
		try {
			AgentJVirtualMachine.setCurrentNodeforAgent(this);
			InetAddress inetDest = InetAddress.getByName("0.0.0." + destination);
			//_logger.info("getting nexthop for " + destination);
			//_logger.info("RoutingSet: " + _node.getRoutingSet().toString());

			if (_node == null || _node.getRoutingSet() == null)
				return -1;
			
			RoutingTuple tuple = _node.getRoutingSet().getDestination(
					inetDest);
			//_logger.info("Routing tuple found:  " + tuple);
			
			if (tuple == null){
				//System.err.println(""); // empty line
				return -1;
			}
			
			int ns2Address = tuple.getNexthop().getAddress()[3];
			//_logger.info("Returning nexthop:  " + ns2Address + "\n");
			
			return ns2Address;
		} catch (UnknownHostException ex) {
			//ignore
		}
		return -1;
	}
	
	/**
	 * Returns the UDP port that control messages are sent to.
	 */
	public int getRoutingPort() {
		return Constants.PORT;
	}
}