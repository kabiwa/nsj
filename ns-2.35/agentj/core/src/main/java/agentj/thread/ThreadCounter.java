package agentj.thread;

import proto.logging.api.Log;
import proto.logging.api.Logger;

/**
 * Simple class to implement a thread counter at multiple levels for a node.  The level
 * of the count increases when callbacks are initiated and are decreased when they
 * the callback finishes (or the if that threadcount goes to zero).  The enables
 * multiple local callbacks to be scoped differently so that local connections
 * get executed gracefully.
 *
 * <p/>
 * Created by scmijt
 * Date: Mar 23, 2008
 * Time: 9:17:13 AM
 */
public class ThreadCounter {
    static Logger logger = Log.getLogger(ThreadCounter.class);
    private int threadCount=0;


    public ThreadCounter() {
    }

    public void increment() {
        ++threadCount;
        logger.debug("Thread count has been incremented = "  + threadCount + "++++++++++++++++++++++");
    }

    public void decrement() {
        --threadCount;
        logger.debug("Thread count is been decremented = "  + threadCount + "++++++++++++++++++++++");
    }

    public int getValue() {
        return threadCount;
    }

    public void reset() {
        threadCount=0;
    }
}
