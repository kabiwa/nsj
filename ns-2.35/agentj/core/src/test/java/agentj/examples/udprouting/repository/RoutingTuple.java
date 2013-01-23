package agentj.examples.udprouting.repository;

import java.net.InetAddress;

public final class RoutingTuple implements Comparable<RoutingTuple> {

	private final InetAddress _nexthop;
	private final InetAddress _destination;
	private final int _distance;

	public RoutingTuple(InetAddress dest, InetAddress nexthop, int distance) {
		_nexthop = nexthop;
		_destination = dest;
		_distance = distance;
	}

	public InetAddress getDestination() {
		return _destination;
	}

	public InetAddress getNexthop() {
		return _nexthop;
	}

	public int getDistance() {
		return _distance;
	}

	public int compareTo(RoutingTuple o) {
		return _distance - o._distance;
	}
	
	public String toString(){
		StringBuffer txt = new StringBuffer();
		txt.append("* RoutingTuple to ");
		txt.append(_destination.getHostAddress());
		txt.append(" nexthop: ");
		txt.append(_nexthop.getHostAddress());
		txt.append(" distance: ");
		txt.append(_distance);
		return txt.toString();
	}
}
