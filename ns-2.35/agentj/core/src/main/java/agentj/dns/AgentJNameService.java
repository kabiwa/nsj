package agentj.dns;

import sun.net.spi.nameservice.NameService;

import java.net.UnknownHostException;
import java.net.InetAddress;

import agentj.AgentJAgent;
import proto.logging.api.Logger;


/**
 * The class implements a simple look up service that ignores names i.e. does not lookup
 * from IP->name. To use this look-up service, you have to set the property:
 * sun.net.spi.nameservice.provider.1="dns,nrlnameservice"
 *
 * Created by Ian Taylor
 * Date: May 21, 2008
 * Time: 9:00:47 AM
 */
public class AgentJNameService implements NameService {
    static Logger logger = Logger.getInstance();

    public AgentJNameService() {
    }

    /**
     * Does a forward DNS lookup i.e. taking an domain name to find the IP address.
     *
     * @param host
     * @return
     * @throws UnknownHostException
     */
    public InetAddress[] lookupAllHostAddr(String host)
            throws UnknownHostException {
        byte[][] ret = new byte[1][];

        logger.debug("Resolving Host " + host + " ..... ");

        if (Addressing.isNsHostName(host)) { // straight forward
            logger.trace("An Agentj Name");
            ret[0] = Addressing.getAddressAsBytes(host);
        } else if (Addressing.isNsMulticastAddressName(host)){  // multicast name
            logger.trace("Multicast Address");
            ret[0] = Addressing.getMulticastAddressAsBytes(host);
        } else if (Addressing.isNsBroadcastAddressName(host)){  // broadcast name
            logger.trace("Broadcast Address");
            ret[0]= Addressing.getBroadcastAddress();
        } else { // not an nsNode address so see if its a raw IP address first
            logger.trace("This is a raw IP Address " + host);
            int[] addr =null;
            try {
                addr = Addressing.getHostAsInts(host);
            } catch (Exception e) {
                logger.trace("Host " + host + " is not an IP address nor a multicast or broacdcast address");
            }
            if (addr==null) { // not a RAW IP Address or other name, so take this as the local host for the machine and map
                try {
                    String hostName = AgentJAgent.getLocalHost().getHostName(); // gets the ns node number of running host
                    ret[0]= Addressing.getAddressAsBytes(hostName);
                } catch (Exception e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            } else  { // a raw IP address so:
                if (Addressing.isMulticastAddress(host)) {
                    String mcaddrName = Addressing.nS2MulticastAddressToName(Addressing.ipMulitcastToNsMulticast(host));
                    ret[0]= Addressing.getMulticastAddressAsBytes(mcaddrName);
                } else if (Addressing.isBroadcastAddress(host)) {
                    ret[0]= Addressing.getBroadcastAddress();
                } else {
                    // raw normal address, so treat as it is.
                    try {
                        ret[0] = Addressing.ipToNumeric(host);
                    } catch (Exception e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            }
        }

        logger.debug("Returning " + ret[0][0] + ":" + ret[0][1] + ":" + ret[0][2] + ":" + ret[0][3]);
        InetAddress retAddr = InetAddress.getByAddress(ret[0]); 
        return new InetAddress[]{retAddr};
    }

    /**
     *  Performs a reverse DNS lookup i.e. given the IP address it returns the name of the domain.
     *
     * @param address
     * @return
     * @throws java.net.UnknownHostException
     */
    public String getHostByAddr(byte[] address)
            throws UnknownHostException {

        logger.debug("Resolving address: " + Addressing.numericIPv4ToTextFormat(address));

        if (Addressing.isMulticastAddress(address))
            return Addressing.getMulticastNameForAddress(address);
        else if (Addressing.isBroadcastAddress(address)) {
            return Addressing.getNsBroadcastPrefix(); // only one broadcast address ..
        } else {
            return Addressing.getNameForAddress(address);
        }
    }


    /**
     * Used by the native implementations to convert between a host name 
     * and its ns counterpart.
     *
     * @param hostName
     * @return
     * @throws UnknownHostException
     */
    public static long getNSAddress(String hostName)
            throws UnknownHostException {

        logger.debug("Resolving Host " + hostName + " ..... ");

        if (Addressing.isNsHostName(hostName)) { // straight forward
            logger.debug("An Agentj Name");
            return Addressing.hostNameToNS2Address(hostName);
        } else if (Addressing.isNsMulticastAddressName(hostName)){  // multicast name
            return Addressing.getNSMulticastAddressForName(hostName);
        } else if (Addressing.isNsBroadcastAddressName(hostName)){  // broadcast name
            return Addressing.getNsBroadcastAddress();
        } else {
            throw new UnknownHostException(hostName + " cannot be found");
        }
    }

    /**
     * Converts between an ns address and an IP host name
     *
     * @param nsAddress
     * @return
     */
    public static String getIPAddress(long nsAddress) {

        logger.debug("Resolving NS Host " + nsAddress + " ..... ");

        if (nsAddress<0){  // multicast or broadcast name ...
            String ipMulticastAddress = Addressing.nsMulticastToIPMulticast(nsAddress);
            if (ipMulticastAddress==null)  // must be broadcast
                return Addressing.getNsBroadcastPrefix();
            else
                return ipMulticastAddress;
        } else { // normal address so
            return Addressing.nS2AddressToHostName(nsAddress);
        }
    }
}
