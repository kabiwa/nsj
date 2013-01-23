package agentj.nativeimp.util;

import proto.logging.api.Log;
import proto.logging.api.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: scmijt
 * Date: Mar 30, 2007
 * Time: 1:30:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class IDFactory {
    static int idCount;

    static Logger logger = Log.getLogger(IDFactory.class);

    static {
        idCount=0;
    }

    /**
     * Gets a unique identifier for use by the sockets so that the remote C++ objects
     * can be inserted nice and neatly into a object array. Ids start from 0 and
     * increment by 1 each time. This way an array can be used to sotre the objects.
     * It can get a little messy as sockets gets deleted but its quick. The alternative is
     * to use a linked list, which will be slow when searching for a lot of sockets ...
     *
     * @return the new id
     */
    public static int getNewID() {
        int id=idCount;
        ++idCount;
        logger.trace("AGENTJ STATS: Total number of sockets created " + idCount);
        return id;
    }
}
