package agentj.thread;

import java.io.IOException;

/**
 * The ... class ...
 * <p/>
 * Created by scmijt
 * Date: Nov 29, 2007
 * Time: 9:54:00 AM
 */
public interface AgentJEventObjectInterface {

 
    /**
     * Tells the EventObject to do the work by pasisng it a particular payload.  This reasults
     * in a send for the SocketOutputStream and DatagramPacket classes.
     * 
     * @param payload
     */
    public void doSend(Object payload) throws IOException;

}
