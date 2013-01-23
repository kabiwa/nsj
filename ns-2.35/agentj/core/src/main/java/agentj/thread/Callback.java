package agentj.thread;

/**
 * The ... class ...
 * <p/>
 * Created by scmijt
 * Date: Oct 23, 2008
 * Time: 8:37:42 PM
 */
public interface Callback {
    ReleaseSafeSync getReleaseSafeSync();

    Object executeCallback();    
}
