package agentj.dns;

import sun.net.spi.nameservice.NameService;
import sun.net.spi.nameservice.NameServiceDescriptor;

/**
 * The ... class ...
 * <p/>
 * Created by scmijt
 * Date: May 21, 2008
 * Time: 9:37:04 AM
 */
public final class AgentJDNSDescriptor implements NameServiceDescriptor  {
    public NameService createNameService() throws Exception {
        return new AgentJNameService();
    }
    public String getProviderName() {
        System.out.println("AgentJ Name Service found ..." );
        return "AgentJDNS";
    }
    public String getType() {
        return "dns";
    }
}