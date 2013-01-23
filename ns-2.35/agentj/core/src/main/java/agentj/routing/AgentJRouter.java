package agentj.routing;

import agentj.AgentJAgent;

/**
 * The AgentJRouter class
 * <p/>
 * Created by Ian Taylor and Ulrich Herberg
 * Date: Nov 27, 2008
 * Time: 10:51:35 AM
 */
public interface AgentJRouter {

    /**
     * Returns the next IP hop for the given destination 
     * that is calculated by the routing algorithm.
     * This is invoked by ns-2 within the routing layer to route the packets.
     *
     * @param destination ns2 address of the destination
     * @return an ns2 address for the next hop.
     */
    public int getNextHop(int destination);

    /**
     * Gets the port number that the routing algorithm sends control traffic on
     *
     * @return port number
     */
    int getRoutingPort();
}
