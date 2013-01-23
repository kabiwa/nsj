package agentj.thread.test;

import agentj.thread.ReleaseSafeSync;

/**
 * The ... class ...
 * <p/>
 * Created by scmijt
 * Date: Oct 21, 2008
 * Time: 8:02:04 PM
 */
public class Producer extends Thread {
    ReleaseSafeSync lockobj;

    public Producer(ReleaseSafeSync lockobj) {
        this.lockobj=lockobj;
    }

    public void run() {
        int i=0;
        while (i<10) {
            String toput = "String #" + String.valueOf(i);
            System.out.println("Putting " + toput);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            lockobj.putAndRelease(toput);
            System.out.println("Done Put ");
            ++i;
        }
    }
}
