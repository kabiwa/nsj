package agentj.thread;

/**
 * The class is a container for the AgentJ event object (SocketOutputStream or DatagramSocket) and
 * its associated payload (SocketPacket or DatagramPacket)
 * <p/>
 *
 * Created by Ian Taylor
 * Date: Nov 29, 2007
 * Time: 10:07:54 AM
 */
public class AgentJEventObject {

    AgentJEventObjectInterface source;
    Object payload;

    public AgentJEventObject(AgentJEventObjectInterface source, Object payload) {
        this.source = source;
        this.payload = payload;
    }

    public AgentJEventObjectInterface getSource() {
        return source;
    }

    public Object getPayload() {
        return payload;
    }

}
