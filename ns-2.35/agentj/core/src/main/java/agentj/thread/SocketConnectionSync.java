package agentj.thread;

import proto.logging.api.Log;
import proto.logging.api.Logger;

public class SocketConnectionSync {
    static Logger logger = Log.getLogger(SocketConnectionSync.class);

    private boolean dataIsReady = false;

    boolean serverIsReady =false;
    boolean connectorIsReady =false;
    Controller controller;
    Object source;

    String sourceName;

    public SocketConnectionSync(Controller controller, Object source) {
        this.controller=controller;
        this.source=source;
        this.sourceName=source.getClass().getName();
    }


    public synchronized void waitForTCPConnection() {
        logger.trace("entering");

        serverIsReady =true;

        if (connectorIsReady) {
            logger.debug("NOT Normal connect is already waitng on node " + controller.getLocalHost());
            controller.getThreadMonitor().threadContinued(sourceName); // to bridge the monitor to release the work
        } else {
            logger.debug("Normal Wait for Connect on node " + controller.getLocalHost());
            controller.getThreadMonitor().threadPaused(sourceName);
        }

        while (!dataIsReady) {
            try {
                wait();  // waitForWorkersToFinish and wait for the isWaiting
            } catch (InterruptedException e) { }
        }
        dataIsReady = false;
        serverIsReady =false;
        notifyAll();
        logger.trace("exiting");
    }

    public synchronized void sendNotificationOfConnection() {
        logger.trace("entering");

        connectorIsReady =true;

        if (serverIsReady) {
            logger.debug("Normal connect - server is waiting on node " + controller.getLocalHost());
            controller.getThreadMonitor().threadContinued(sourceName); // to bridge the monitor to release the work
        } else { // if it is not ready, we will block, so release the lock
            logger.debug("NOT Normal connect - server is NOT waiting on node " + controller.getLocalHost());
            controller.getThreadMonitor().threadPaused(sourceName);
        } // need to release sync if there isn't a receive ready to read the data

        while (!serverIsReady) {
            try {
                wait();
            } catch (InterruptedException e) { }
        }

        dataIsReady = true;
        connectorIsReady =false;

        notifyAll();
        try { /// Must wait for receive to go first ...  NEEDS a proper solution ...
            Thread.sleep(100);
        } catch (InterruptedException ee) {
        }
        logger.trace("exiting");
    }

}