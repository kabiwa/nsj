package agentj.dns;

import proto.logging.api.Logger;

import util.StringSplitter;

import java.net.UnknownHostException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.math.BigInteger;
import java.util.Hashtable;
import java.util.Enumeration;

import agentj.AgentJAgent;

/**
 * The class specifiies the mappings between IP4/6 Addresses and NS-2.  It
 * also figures out if an address is a multicast and if so, dynamically
 * sets it according to the model one is applying e.g. wored or wireless.
 * For standard NS addresses we have the following mappings for DNS
 *
 * <ol>
 * <li> <b>Name</b> is the NS node address + the identifier defined to prepend this name (for aesthetics). For
 * example nsNode33
 * <li> <b>Address</b> is the IP address equivalent for this node number. So, a node number 33, would have the address
 * 0.0.0.33 and a node 256 would have the address 0.0.1.0
 * <li> getting the NS-2 address. There are mapping methods here to extract the ns-2 node address from the Name
 * but this is also handled internally before being passed to Ns-2 i.e. "nsNode55" will always be passed to ns-2 as "55".
 * </ol>
 *
 * <p/>
 * Created by scmijt
 * Date: Mar 20, 2009
 * Time: 6:28:54 PM
 */
public class Addressing {
    static Logger logger = Logger.getInstance();

    final static int INT16SZ = 2;
    final static int IPV6ADDRSIZE = 16;
    final static int IPV4ADDRSIZE = 4;

    private static String nsAddressPrefix ="nsNode"; // prepended to the address part of the DNS lookup for an address.
    private static String nsMulticastPrefix ="nsMC"; // prepended to the address part of the DNS lookup for an address.
    private static String nsBroadcastPrefix ="nsBC"; // prepended to the address part of the DNS lookup for an address.

    // Multicast 224.0.0.0 through 239.255.255.255

    private static int MC_START_ADDRESS = 224;
    private static int MC_END_ADDRESS = 239;
    static Hashtable<String,String> ns2multicastlookup = new Hashtable<String,String>();


    // MANET mapping also uses BROADCAST address -1 

    private static String mcMANETMapping = "-1";

    private static int nsBroadcastAddress = -1;

    // end defines.

    // Get and set methods

    public static String getNsAddressPrefix() {
        return nsAddressPrefix;
    }

    public static void setNsAddressPrefix(String nsAddressPrefix) {
        Addressing.nsAddressPrefix = nsAddressPrefix;
    }

    public static String getNsMulticastPrefix() {
        return nsMulticastPrefix;
    }

    public static void setNsMulticastPrefix(String nsMulticastPrefix) {
        Addressing.nsMulticastPrefix = nsMulticastPrefix;
    }

    public static String getNsBroadcastPrefix() {
        return nsBroadcastPrefix;
    }

    public static void setNsBroadcastPrefix(String nsBroadcastPrefix) {
        Addressing.nsBroadcastPrefix = nsBroadcastPrefix;
    }

    public static long getNsBroadcastAddress() {
        return nsBroadcastAddress;
    }


    // Standard Addressing

    /**
     * Returns a ns2 node address (i.e. an integer) from the name of the host, which is form
     * ns_identifier<node address>
     *
     * @param nsHostName
     * @return
     */
    public static long hostNameToNS2Address(String nsHostName) throws UnknownHostException {
        try {
            String digits = nsHostName.substring(nsAddressPrefix.length());
            long nsnode = Long.parseLong(digits);

            return nsnode;
        } catch (Exception ee) {
            throw new UnknownHostException("Host " + nsHostName + " has an invalid format");
        }
    }

    /**
     * Returns a host name for a ns2 node address (i.e. an integer) by prepending the
     * identifier
     *
     * @param nsAddress
     * @return
     */
    public static String nS2AddressToHostName(long nsAddress) {
        String digits = Long.toString(nsAddress);
        return nsAddressPrefix + digits;
    }

    /**
     * Whether address is a standard ns-2 node address
     *
     * @param hostName
     * @return
     */
    static boolean isNsHostName(String hostName) {
        return hostName.startsWith(nsAddressPrefix);
    }


    /**
     * Returns the IP address as a byte array for the given hostName (Forward DNS lookup).
     * Returns an IPV6 or IPV4 address depending on which on is chosen
     *
     * @param hostName
     * @return
     */
    public static byte[] getAddressAsBytes(String hostName) throws UnknownHostException {
        long nodeAddress = hostNameToNS2Address(hostName);
        byte[] addr;

        logger.trace("Addressing: Resolving Host " + nodeAddress + " ..... ");

        if (Boolean.parseBoolean(System.getProperty("java.net.preferIPv6Stack"))) {
            // see if it is IPv6 address
            addr = nsToNumericFormatV6(Long.toString(nodeAddress));
            if (addr == null)
                throw new UnknownHostException(hostName + ": illegal address " + hostName);
        } else { // default is IPv4 address
            addr = nsToNumericFormatV4(Long.toString(nodeAddress));
            if (addr == null)
                throw new UnknownHostException(hostName + ": illegal address" + hostName);
        }

        logger.trace("getAddressAsBytes() returning " + addr[0] + ":" + addr[1] + ":" + addr[2] + ":" + addr[3]);

        return addr;
    }

    /**
     * Returns the host name for the given IP address as bytes.  Reverse DNS lookup
     *
     * @param address
     * @return
     */
    public static String getNameForAddress(byte[] address) throws UnknownHostException {
        int nodeAddress;

        if (Boolean.parseBoolean(System.getProperty("java.net.preferIPv6Stack"))) {
            // see if it is IPv6 address
            nodeAddress = numericIPv6ToNS2(address); // to NS2 number
        } else { // default is IPv4 address
            nodeAddress = numericIPv4ToNS2(address); // to NS2 number
        }
        return nS2AddressToHostName(nodeAddress);

    }

    // BROADCAST ADDRESSES

    /**
     * Whether address is an ns-2 broadcast (BC) address
     *
     * @param hostName
     * @return
     */
    static boolean isNsBroadcastAddressName(String hostName) {
        return hostName.startsWith(nsBroadcastPrefix);
    }

    /**
     * Tests if IP address (in form 255.255.255.255) as a string is a broadcast address
     *
     * @param address
     * @return
     */
    public static boolean isBroadcastAddress(String address) {
        String adddressLC = address.toLowerCase();
        return (adddressLC.startsWith("255") || adddressLC.startsWith("0xff")
                || adddressLC.startsWith("ff"));
    }

    /**
     * Tests if IP address (in form 255.255.255.255) as a string is a broadcast address
     *
     * @param addr
     * @return
     */
    public static boolean isBroadcastAddress(byte[] addr) {
        logger.trace("isBroadcastAddress, addr[0] = " + addr[0]);
        return ((addr[0]&0xff) == 0xff);
    }


    public static byte[] getBroadcastAddress() {
        if (Boolean.parseBoolean(System.getProperty("java.net.preferIPv6Stack"))) {
            // All-nodes multicast address
            return new byte[]{(byte) 0xff, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01};
        } else {
            byte maxbyte=(byte)0xff;
            byte[] broadcast = {maxbyte, maxbyte, maxbyte, maxbyte};
            return broadcast;
        }
    }

    public static String getBroadcastAddressString() {
        if (Boolean.parseBoolean(System.getProperty("java.net.preferIPv6Stack"))) {
            return "ff02::1";
        } else {
            return "255.255.255.255";
        }
    }


    // MUTICAST ADDRESSES

    /**
     * Whether address is an ns-2 multicast (MC) address
     *
     * @param hostName
     * @return
     */
    static boolean isNsMulticastAddressName(String hostName) {
        return hostName.startsWith(nsMulticastPrefix);
    }

    /**
     * Tests if the IPn address (as a String) is a multicast address
     *
     * @param address
     * @return
     */
    static boolean isMulticastAddress(String address) {
        byte[] addr = null;

        try {
            addr = ip4ToNumericFormat(address);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (addr.length==4) { // IP 4 address
            return isMulticastAddress(addr);
        }
        else { // check for Ns2 multicast
            String adddressLC = address.toLowerCase();
            return adddressLC.startsWith("0xff")
                    || adddressLC.startsWith("ff");
        }
    }

    static boolean isMulticastAddress(byte[] addr) {
        int firstVal = (addr[0]&0xff);
        if ((firstVal>=224) && (firstVal<=239))
            return true;
        else return false;
    }

    /**
     * Returns a ns2 node address (i.e. an integer) from the name of the host, which is form
     * ns_identifier<node address>
     *
     * @param multicastName
     * @return
     */
    public static long nameToNS2MulticastAddress(String multicastName) throws UnknownHostException {
        try {
            String digits = multicastName.substring(nsMulticastPrefix.length());
            long mcaddress = Long.parseLong(digits);
            return mcaddress;
        } catch (Exception ee) {
            throw new UnknownHostException("Multicast Name " + multicastName + " is not found !!! Check name and/or prefix");
        }
    }

    /**
     * Returns a host name for a ns2 node address (i.e. an integer) by prepending the
     * identifier
     *
     * @param nsMulticastAddress
     * @return
     */
    public static String nS2MulticastAddressToName(long nsMulticastAddress) {
        String digits = Long.toString(nsMulticastAddress);
        return nsMulticastPrefix + digits;
    }

    /**
     * Gets the NS-2 multicast address for an IP address. You need to test whether the address is multicast before
     * calling this method.
     *
     * @param ipAddress
     * @return
     */
    static long ipMulitcastToNsMulticast(String ipAddress) {
        String resolvedHostName;

        logger.trace("ipMulitcastToNsMulticast: ipAddress = " + ipAddress);

        // lookup first
        resolvedHostName = (String)ns2multicastlookup.get(ipAddress);

        if (resolvedHostName==null) { // not found, create a new one
            if (AgentJAgent.isSimulationMANET())  // Using wireless
                resolvedHostName=mcMANETMapping;
            else
                resolvedHostName=createNSMulticastAddress();
            ns2multicastlookup.put(ipAddress, resolvedHostName); // put the pair in the lookup
        }

        return Long.parseLong(resolvedHostName);
    }

    /**
     * Gets the IP address for an NS-2 multicast address. This works for the test
     * for broadcast also as long as we do not use MANET multicast and broadcast at
     * the same time, which we cannot anyway :)
     *
     * @param nsMulticastAddr
     *
     * @return
     */
    static String nsMulticastToIPMulticast(long nsMulticastAddr) {
        Enumeration ipAddresses = ns2multicastlookup.keys();
        while (ipAddresses.hasMoreElements()) {
            String multicastAddress=(String)ipAddresses.nextElement();
            String nsAddress = ns2multicastlookup.get(multicastAddress);
            long nsNode;
            boolean isAnInt=true;
            try {
                nsNode = Long.parseLong(nsAddress);
            } catch (NumberFormatException e) {
                isAnInt=false;
            }
            if (isAnInt) return multicastAddress;
            // else try and parse the host name also
            try {
                nsNode= nameToNS2MulticastAddress(nsAddress);
                if (nsNode==nsMulticastAddr)
                    return multicastAddress;
            } catch (UnknownHostException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        return null;
    }


    /**
     * Returns the IP address as a byte array for the given hostName (Forward DNS lookup).
     * Returns an IPV6 or IPV4 address depending on which on is chosen
     *
     * @param multicastName
     * @return
     */
    public static byte[] getMulticastAddressAsBytes(String multicastName) throws UnknownHostException {
        long nsMulticastAddress = nameToNS2MulticastAddress(multicastName);
        String ipMulticastAddress = nsMulticastToIPMulticast(nsMulticastAddress);
        byte[] addr=null;

        logger.trace("Multicast IP Address = " + ipMulticastAddress);

        try {
            addr = ipToNumeric(ipMulticastAddress);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return addr;
    }

    /**
     * Returns the IP address as a byte array for the given hostName (Forward DNS lookup).
     * Returns an IPV6 or IPV4 address depending on which on is chosen
     *
     * @param multicastName
     * @return
     */
    public static long getNSMulticastAddressForName(String multicastName) throws UnknownHostException {
        long nsMulticastAddress = nameToNS2MulticastAddress(multicastName);
        String ipMulticastAddress = nsMulticastToIPMulticast(nsMulticastAddress);
        return ipMulitcastToNsMulticast(ipMulticastAddress);
    }


    /**
     * Returns the host name for the given IP address as bytes.  Reverse DNS lookup
     *
     * @param address
     * @return
     */
    public static String getMulticastNameForAddress(byte[] address) throws UnknownHostException {
        String ipMulticastAddress;

        if (Boolean.parseBoolean(System.getProperty("java.net.preferIPv6Stack"))) {
            // see if it is IPv6 address
            ipMulticastAddress = numericIPv6ToTextFormat(address);
        } else { // default is IPv4 address
            ipMulticastAddress = numericIPv4ToTextFormat(address);
        }
        long nsMulticastAddress = ipMulitcastToNsMulticast(ipMulticastAddress);
        return nS2MulticastAddressToName(nsMulticastAddress);
    }

    /**
     * Creates a new NS-2 multicast Address
     * @return
     */
    private static String createNSMulticastAddress() {
        logger.trace("creating mutlicast address");
        String var = "agentjgroup" + String.valueOf(ns2multicastlookup.size());
        // alocate an Ns-2 group address
        String mcastaddr= AgentJAgent.tclEvaluateJava("set " + var + " [Node allocaddr]");

        // return "-"+mcastaddr; // make it negative to fit into an int
        return mcastaddr; 
    }


    
    // helper methods


    /**
     * New implementation - returns the IP address in bytes given an IPv4 address
     *
     * @param hostName
     * @return
     */
    static int[] getHostAsInts(String hostName) {
        int[] addr;

        String [] delims = new String[1];
        delims[0]=".";

        StringSplitter str = new StringSplitter(hostName, delims);

        addr = new int[str.size()];

        for (int i=0; i< str.size(); ++i)
            addr[i] = Integer.parseInt(str.at(i));

        return addr;
    }

    public static byte[] ipToNumeric(String ipAddress) throws Exception {
        if (Boolean.parseBoolean(System.getProperty("java.net.preferIPv6Stack")))
            return ip6ToNumericFormat(ipAddress);
        else
            return ip4ToNumericFormat(ipAddress);
    }

    /**
     * Converts IPv4 address in its textual presentation form into its numeric binary form.
     *
     * @param src
     *            a String representing an IPv4 address in standard format
     * @return a byte array representing the IPv4 numeric address
     */
    static byte[] ip4ToNumericFormat(String src) throws Exception {
        if (src == null || src.length() == 0)
            return null;
        logger.trace("Convert to numeric -" + src + "-");

        byte[] dst = new byte[IPV4ADDRSIZE];

        int pos1=0;
        int pos2;
        src+="."; // to get the last dot - convenience :)
        int i=0;
        while (((pos2=src.indexOf(".", pos1)) != -1) && (i <4)) {
            String next=src.substring(pos1,pos2);
            logger.trace("Next number -" + next + "-");
            dst[i]= (byte)(Integer.parseInt(next)&0xff);
            ++i;
            pos1=pos2+1;
        }

        if (i<3) throw new Exception("Address: " + src + " is in the wrong format. \n" +
                " Expecting IP String address. ");

        return dst;
    }

    public static byte[] nsToNumericFormatV4(String src){

        byte[] res = new byte[IPV4ADDRSIZE];

        Long val = Long.parseLong(src);
        if (val < 0 || val > 0xffffffffL)
            return null;
        res[0] = (byte) ((val >> 24) & 0xff);
        res[1] = (byte) (((val & 0xffffff) >> 16) & 0xff);
        res[2] = (byte) (((val & 0xffff) >> 8) & 0xff);
        res[3] = (byte) (val & 0xff);
        return res;
    }

    public static byte[] nsToNumericFormatV6(String src){
        byte[] res = new byte[IPV6ADDRSIZE];

        Long val = Long.parseLong(src);
        if (val < 0 || val > 0xffffffffL) // ok, we could support larger Ns2 host numbers, but probably that will suffice now
            return null;
        res[12] = (byte) ((val >> 24) & 0xff);
        res[13] = (byte) (((val & 0xffffff) >> 16) & 0xff);
        res[14] = (byte) (((val & 0xffff) >> 8) & 0xff);
        res[15] = (byte) (val & 0xff);
        return res;
    }


    /**
     * Converts IPv6 address in its textual presentation form into its numeric binary form.
     *
     * @param src
     *            a String representing an IPv4 address in standard format
     * @return a byte array representing the IPv4 numeric address
     */
    static byte[] ip6ToNumericFormat(String src) {
        if (src == null || src.length() == 0)
            return null;
        logger.trace("Convert to numeric -" + src + "-");
        try {
            InetAddress addr = Inet6Address.getByName(src);
            return addr.getAddress();
        } catch (Exception ex){
            return null;
        }
    }


    static String numericIPv4ToTextFormat(byte[] src) {
        StringBuffer sb = new StringBuffer(8);
        for (int i = 0; i < src.length; ++i) {
            sb.append(Integer.toString((int)( src[i])& 0xff));
            if (i<(src.length-1)) sb.append(".");
        }
        return sb.toString();
    }

    /*
     * Convert IPv6 binary address into presentation (printable) format.
     *
     * @param src a byte array representing the IPv6 numeric address
     * @return a String representing an IPv6 address in
     *         textual representation format
     * @since 1.4
     */
    static String numericIPv6ToTextFormat(byte[] src) {
        StringBuffer sb = new StringBuffer(39);
        for (int i = 0; i < (IPV6ADDRSIZE / INT16SZ); i++) {
            sb.append(Integer.toHexString(((src[i<<1]<<8) & 0xff00)
                    | (src[(i<<1)+1] & 0xff)));
            if (i < (IPV6ADDRSIZE / INT16SZ) -1 ) {
                sb.append(":");
            }
        }
        return sb.toString();
    }

    // IPV6 format = HEX e.g. 2001:0db8:85a3:0000:0000:8a2e:0370:7334

    static int numericIPv6ToNS2(byte[] src) {
        int val=0;
        for (int i = 0; i < (IPV6ADDRSIZE / INT16SZ); i++) {
            val += Integer.parseInt(Integer.toHexString(((src[i<<1]<<8) & 0xff00)
                    | (src[(i<<1)+1] & 0xff)), 16);
        }
        return val;
    }

    static int numericIPv4ToNS2(byte[] src) {
        return new BigInteger(1,src).intValue(); // easier ...
    }

}


