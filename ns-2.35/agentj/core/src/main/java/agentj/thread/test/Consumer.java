package agentj.thread.test;

import agentj.thread.ReleaseSafeSync;

/**
 * The ... class ...
 * <p/>
 * Created by scmijt
 * Date: Oct 21, 2008
 * Time: 8:00:38 PM
 */
public class Consumer extends Thread {
    ReleaseSafeSync lockobj;

    public Consumer(ReleaseSafeSync lockobj) {
        this.lockobj=lockobj;
    }

    public void run() {
        int i=0;
        while (i<10) {
            try {
                Object data = lockobj.blockAndGet();
                System.out.println("Got " + data);
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            ++i;
        }
    }
}

