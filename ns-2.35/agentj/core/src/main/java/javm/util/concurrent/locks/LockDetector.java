package javm.util.concurrent.locks;

import proto.logging.api.Logger;
import proto.logging.api.Log;

import java.util.concurrent.locks.Lock;

/**
 * Just for debugging to make sure we don't hang on the locks ...
 * 
 * <p/>
 * Created by scmijt
 * Date: Nov 5, 2008
 * Time: 8:05:39 AM
 */
public class LockDetector {
    static Logger logger = Log.getLogger(LockDetector.class);

    public static void lock(Lock objectBeingLocked) {
        logger.debug("Lock about to be Called on Object " + objectBeingLocked.getClass().getName());
        objectBeingLocked.lock();
        logger.debug("Lock Called on Object " + objectBeingLocked.getClass().getName());
    }

    public static void unlock(Lock objectBeingUnlocked) {
        logger.debug("UnLock about to be Called on Object " + objectBeingUnlocked.getClass().getName());
        objectBeingUnlocked.unlock();
        logger.debug("UnLock Called on Object " + objectBeingUnlocked.getClass().getName());
    }
}
