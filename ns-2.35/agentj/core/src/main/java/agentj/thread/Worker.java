package agentj.thread;

/**
 * The Worker interface allows a class to doWork from within a worker Thread
 *
 * <p/>
 * Created by Ian Taylor
 *
 * @see ThreadWorker
 *
 * Date: Dec 18, 2007
 * Time: 12:31:41 PM
 */
public interface Worker {

    /**
     * This is called on a Worker object to instruct it to execute the work it would like
     * to do within a WorkerThread object.
     */
    public void doWork(String... variables);

}
