package agentj.thread;

import proto.logging.api.Log;
import proto.logging.api.Logger;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The ... class ...
 * <p/>
 * Created by scmijt
 * Date: Nov 28, 2007
 * Time: 1:15:49 PM
 */
public class AgentJEventQueue  {

    private static ConcurrentLinkedQueue<AgentJEventObject> events
            = new ConcurrentLinkedQueue<AgentJEventObject> ();

    static Logger logger = Log.getLogger(Controller.class);

    private static final AgentJEventQueue instance =
            new AgentJEventQueue();


    private AgentJEventQueue() {} // One is enough - singleton

    public static AgentJEventQueue getInstance() {
        return instance;
    }

    /**
     * Pushes a send instruction onto the FIFO queue (either TCP or UDP).
     */
    public static void pushEvent(AgentJEventObjectInterface source, Object payload) {
        events.add(new AgentJEventObject(source,payload));
    }

    /**
     * Pops all of the elements off the queue and executes each event in turn. This
     * results in the sending of the data 
     *
     * @return number of events popped
     */
    public static int popEvents() {

        int eventsPopped=0;

        AgentJEventObject jobject;

        while ((jobject= events.poll()) !=null) {
            try {
                jobject.getSource().doSend(jobject.getPayload());
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            ++eventsPopped;
        }

        return eventsPopped;


    }
}
