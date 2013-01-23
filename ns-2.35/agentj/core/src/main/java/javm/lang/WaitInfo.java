package javm.lang;

import agentj.thread.Controller;

/**
 * The ... class ...
 * <p/>
 * Created by scmijt
 * Date: Aug 21, 2008
 * Time: 9:20:28 AM
 */
public class WaitInfo {
    private Controller cont;
    private int objectsWaiting;
    private java.lang.Object sourceWaitObject;

    public WaitInfo(Controller cont, int objectsWaiting, java.lang.Object sourceWaitObject) {
        this.cont = cont;
        this.objectsWaiting = objectsWaiting;
        this.sourceWaitObject = sourceWaitObject;
    }

    public Controller getCont() {
        return cont;
    }

    public int getObjectsWaiting() {
        return objectsWaiting;
    }

    public java.lang.Object getSourceWaitObject() {
        return sourceWaitObject;
    }

    public void setObjectsWaiting(int objectsWaiting) {
        this.objectsWaiting = objectsWaiting;
    }
}
