package javm.util.concurrent;

import proto.logging.api.Logger;
import proto.logging.api.Log;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * The is a hack around replacing the ConditionObject with our own.  I tried the
 * following routes, which did not work, then ended up with this scheme. Using
 * Javaassist to reroute newCondition() methods to return and AgentJ Condition
 * interface object is all I need to do.  Then I can reroute and hook into
 * AgentJ threading.  However, there are the following obstacles:
 *
 * <ol>
 * <li> ConditionObject.java is an inner class of AbstractQueuedSynchronizer
 * which is in java.util.concurrent.locks.  The only way to instantiate this
 * is to subclass AbstractQueuedSynchronizer and create one internally.  Accessing
 * outside a subclass of AbstractQueuedSynchronizer gives a "AbstractQueuedSynchronizer
 * is not an enclosing class error"
 * <li> Creating a AgentJQueuedSynchronizer to subclass AbstractQueuedSynchronizer
 * and then subclassing ConditionObject also does not work because ConditionObject
 * makes all of the Condition interface methods final in its implementation
 * </ol>
 *
 * So, the only way forward seems to be to create a dummy AbstractQueuedSynchronizer,
 * implemented here, to gain access to a ConditionObject.  Then once, accessed
 * we can create a AgentJConditionObject that does not subclass anything but
 * implements the methods in the Condition interface. AgentJConditionObject
 * therefore acts as a singleton that just creates a
 * ConditionObject and passes the calls along to this object.  But first it
 * does what it needs to in AgentJ...
 *
 * <p/>
 * Created by Ian Taylor
 * Date: Sep 15, 2008
 * Time: 3:01:36 PM
 */
public class AgentJQueuedSynchronizer extends AbstractQueuedSynchronizer {
    static Logger logger = Log.getLogger(AgentJQueuedSynchronizer.class);

    public AgentJQueuedSynchronizer() {
    }

    public ConditionObject newCondition() {
        return new AbstractQueuedSynchronizer.ConditionObject();
    }
}
