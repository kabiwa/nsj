package agentj.examples.udprouting.repository;

import java.net.InetAddress;
import java.util.ArrayList;

/**
 * Stores the next hops for all destinations
 * @author herberg
 *
 */
public class RoutingSet extends ArrayList<RoutingTuple>{
	private static final long serialVersionUID = 1L;
	

	public boolean containsDestination(InetAddress dest){
		for (RoutingTuple tuple : this){
			if (tuple.getDestination().equals(dest))
				return true;
		}

		return false;
	}
	
	/**
	 * Returns a destination with the given IP address and the given distance in hops (h)
	 * @param dest
	 * @param h
	 * @return tuple or null
	 */
	public RoutingTuple getDestination(InetAddress dest, int h){
		for (RoutingTuple tuple : this){
			if (tuple.getDestination().equals(dest) && tuple.getDistance() == h)
				return tuple;
		}

		return null;
	}
	
	public RoutingTuple getDestination(InetAddress dest){
		for (RoutingTuple tuple : this){
			if (tuple.getDestination().equals(dest))
				return tuple;
		}
	
		return null;
	}
}
