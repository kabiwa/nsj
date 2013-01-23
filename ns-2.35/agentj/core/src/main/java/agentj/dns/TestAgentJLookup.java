package agentj.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Security;

/**
 * To use, set the property, sun.net.spi.nameservice.provider.1=dns,dnsfudge.NRLNameService
 * <p/>
 * Created by scmijt
 * Date: May 21, 2008
 * Time: 9:20:37 AM
 */
public class TestAgentJLookup {

    public static void main(String args[]) {

        try {
            // Security.setProperty("networkaddress.cache.ttl", "0");
            System.setProperty("sun.net.spi.nameservice.provider.1", "dns,AgentJDNS");

            InetAddress localHost = InetAddress.getLocalHost();
            String localName = localHost.getHostName();
            String localAddress = localHost.getHostAddress();

            System.out.println("Local Host - " + localHost);
            System.out.println("Local Host as name - " + localName);
            System.out.println("Local Host as address - " + localAddress);

            InetAddress[] all = InetAddress.getAllByName("192.168.0.53");

            for (int i =0; i< all.length; ++i)
                System.out.println("Host is " + all[i].getHostName());

            all = InetAddress.getAllByName("hello.com");

            for (int i =0; i< all.length; ++i)
                System.out.println("Host is " + all[i].getHostName());


        } catch (UnknownHostException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

}