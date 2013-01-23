package agentj.instrument;

/**
 * A list of packages and classes that we either ignore or swap out from the JVM.
 * <p/>
 * Created by scmijt
 * Date: Sep 17, 2008
 * Time: 12:34:14 PM
 */
public class ClassList {

/*    static String[] ignored_packages = {
            "java.lang.", "java.net.", "javax.", "javassist.", "sun.", "com.sun.", "org.xml.", "proto."
    };

    static String[] full_ignored_packages = {
            "java.lang.", "java.net.", "javax.", "javassist.", "sun.", "com.sun.", "org.xml.", "proto."
    };            */

    static String[] ignored_packages = {
            "java.lang.",  "javax.", "javassist.", "sun.", "com.sun.", "org.xml.", "proto."
    };

    static String[] full_ignored_packages = {
            "java.lang.",  "javax.", "javassist.", "sun.", "com.sun.", "org.xml.", "proto."
    };

    static String[] ignored_agentj_packages = {
            "agentj.thread.", "agentj.instrument.", "javm."
    };

    static String[] ignoredClasses = {
            "java.util.concurrent.locks.AbstractQueuedSynchronizer",
            "java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject",
            "java.util.concurrent.locks.Lock",
            //          "java.util.concurrent.locks.LockSupport",
            "agentj.AgentJNode", "agentj.AgentJAgent",
            "agentj.AgentJObjectHashtable", "agentj.AgentJObjectItem",
            "agentj.NAMCommands"};

    public static final String JAVA_NET_PACKAGE = "java/net/";
    public static final String JAVA_NIO_PACKAGE = "java/nio/channels/";
    public static final String JAVA_UTIL_PACKAGE = "java/util/";
//    public static final String JAVA_LOCKS_PACKAGE = "java/util/concurrent/locks/";

    public static final String NEW_NET_PACKAGE = "javm/net/";
    public static final String NEW_NIO_PACKAGE = "javm/nio/channels/";
    public static final String NEW_UTIL_PACKAGE = "javm/util/";
    //   public static final String NEW_LOCKS_PACKAGE = "javm/util/concurrent/locks/";

    // the whole java.net ...

/*    public static String[] REPLACE_NET_CLASSES = new String[]{
            "JarURLConnection", "Socket",
            "Authenticator", "InetSocketAddress", "ServerSocket",
            "SocketImpl", "DatagramSocketImpl", "SocketImplFactory", "DatagramSocketImplFactory",
            "NetPermission", "SocketInputStream", "NetworkInterface",
            "SocketOutputStream", "HttpURLConnection", "PasswordAuthentication",
            "SocketPermission", "Inet4Address", "NsDatagramSocketImpl",
            "SocksConsts", "Inet4AddressImpl", "NsSocketImpl",
            "SocksSocketImpl", "Inet6Address", "Proxy", "Inet6AddressImpl",
            "ProxySelector", "InetAddress",
            "ResponseCache", "InetAddressImpl", "SecureCacheResponse",
            "DatagramPacket", "DatagramSocket", "MulticastSocket", "NsDatagramSocketImpl"
    }; */

    public static String[] REPLACE_NET_CLASSES = new String[0];

    /*public static String[] REPLACE_IO_CLASSES = new String[]{
            "BufferedInputStream", "BufferedReader", "InputStreamReader"
    };*/

/*    public static String[] REPLACE_NIO_CLASSES = new String[] {
      "DatagramChannel", "SocketChannel", "ServerSocketChannel"
    }; */

    public static String[] REPLACE_NIO_CLASSES = new String[0];

    public static String[] REPLACE_UTIL_CLASSES = new String[]{
            "Timer", "TimerTask"
    };
//    public static String[] REPLACE_LOCKS_CLASSES = new String[]{
//            "LockSupport"
    //   };


/*    public static String[] DONT_REPLACE_CLASSES = new String[]{
            "SocketException", "SocketAddress", "SocketTimeoutException", "SocketOptions",
            "InetAddressImplFactory", "InetAddress"
    };  */

    public static String[] DONT_REPLACE_CLASSES = new String[0]; 
}
