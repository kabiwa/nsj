package agentj;

import agentj.nativeimp.NsSocketImpl;
import agentj.nativeimp.NsDatagramSocketImpl;

import java.net.*;
import java.io.IOException;

/**
 * The class implements the factories for the native sockets used in AgentJ.  The can be set
 * before deployment by the first agent created if the agent calls one of the SetTransportBindings
 * methods to pass their implementation at run-time to agentj.
 * 
 * <p/>
 * Created by scmijt
 * Date: Mar 19, 2009
 * Time: 11:13:00 AM
 */
public class AgentJSocketFactory implements SocketImplFactory, DatagramSocketImplFactory {
    private static boolean deployed =false;
    private static SocketImpl socketImpl=null;
    private static DatagramSocketImpl updSocketImpl=null;

    public static void setTransportBinding(SocketImpl sockimpl) {
        AgentJSocketFactory.socketImpl=sockimpl;
    }

    public static void setTransportBinding(DatagramSocketImpl updSocketImpl) {
        AgentJSocketFactory.updSocketImpl=updSocketImpl;
    }

    /**
     * Factory for TCP Sockets
     *
     * @return
     */
    public SocketImpl createSocketImpl() {
        if (socketImpl==null) return new NsSocketImpl();
        else
            return socketImpl;
    }

    public DatagramSocketImpl createDatagramSocketImpl() {
        if (updSocketImpl==null) return new NsDatagramSocketImpl();
        else
            return updSocketImpl;
    }

    /**
     * Factory for UDP Sockets
     *
     * @return
     */

    /**
     * Call this method to deploy the this Factory into the JVM for choosing the TAPS socket
     * binding.
     */
    public static void deployFactories() {
        if (deployed) return;
        
        try {
            DatagramSocket.setDatagramSocketImplFactory(new AgentJSocketFactory());
            Socket.setSocketImplFactory(new AgentJSocketFactory());
            ServerSocket.setSocketFactory(new AgentJSocketFactory());
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        deployed =true;
    }

}
