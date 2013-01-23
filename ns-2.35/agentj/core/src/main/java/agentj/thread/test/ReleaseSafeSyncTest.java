package agentj.thread.test;

import agentj.thread.Controller;
import agentj.thread.ReleaseSafeSync;

import java.io.IOException;

/**
 * The ... class ...
 * <p/>
 * Created by scmijt
 * Date: Oct 21, 2008
 * Time: 8:03:45 PM
 */
public class ReleaseSafeSyncTest {

    public ReleaseSafeSyncTest() {

        ReleaseSafeSync sync = new ReleaseSafeSync(null, ReleaseSafeSync.SyncType.SOCKET);

        Consumer c = new Consumer(sync);
        Producer p = new Producer(sync);

        c.start();
        p.start();
    }


    public static void main(String[] args) throws IOException {
      new ReleaseSafeSyncTest();
    }

    
}
