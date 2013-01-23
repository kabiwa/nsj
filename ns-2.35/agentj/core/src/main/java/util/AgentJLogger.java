package util;

import proto.logging.api.Log;
import proto.logging.api.LogProcessor;
import proto.logging.api.Logger;


/**
 * A Class to simplify the logging capabilities for AgentJ. Makes it easier
 * to switch later on if we need to
 * 
 * <p/>
 * Created by scmijt
 * Date: Dec 13, 2007
 * Time: 6:46:06 PM
 */
public class AgentJLogger {

    private static Logger logger;

    static {
        logger = Log.getLogger(AgentJLogger.class);
        logger.setLogLevel(Logger.LogLevel.INFO);
    }

    public static Logger getLogger(Object callingClass) {
        return logger;
    }

    /**
     * Ignore calling class reference - stick to one logger.
     * 
     * @param callingClass
     * @return
     */
    public static Logger getLogger(Class callingClass) {
        return logger;
    }
}
