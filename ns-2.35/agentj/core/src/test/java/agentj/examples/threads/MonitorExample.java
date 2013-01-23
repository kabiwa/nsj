package agentj.examples.threads;

import agentj.thread.Controller;
import proto.logging.api.Logger;
import proto.logging.api.Log;

/**
 * The ... class ...
 * <p/>
 * Created by scmijt
 * Date: Sep 10, 2008
 * Time: 1:33:13 PM
 */
public class MonitorExample {
    static Logger logger = Log.getLogger(MonitorExample.class);
    private boolean dataIsReady = false;
    Object data;
    Object source;

    public MonitorExample(Object source) {
        this.source=source;
    }

    public synchronized Object receive() {
        logger.info("Entering");

        while (!dataIsReady) {
            try {
                wait();  
            } catch (InterruptedException e) { }
        }
        dataIsReady = false;
        logger.trace("Notifying !!!");
        notify();
        logger.trace("Exiting");
        return data;
    }

    public synchronized void send(Object data) {
        logger.info("Entering");

        while (dataIsReady) {
            try {
                wait();
            } catch (InterruptedException e) { }
        }

        this.data=data;

        dataIsReady = true;

        logger.trace("Notifying !!!");
        notify();
        logger.trace("Exiting");
    }
}
