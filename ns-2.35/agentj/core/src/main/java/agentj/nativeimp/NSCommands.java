package agentj.nativeimp;

import agentj.AgentJAgent;

import java.util.concurrent.locks.ReentrantLock;

/**
 * The ... class ...
 * <p/>
 * Created by scmijt
 * Date: Dec 31, 2007
 * Time: 10:09:00 AM
 */
public class NSCommands {

    public boolean tclEvalfJava(String cmd){
        AgentJAgent.getLock().lock();
        boolean ret=false;

        try {
            ret=tclEvalf(cmd);
        } finally {
            AgentJAgent.getLock().unlock();
        }

        return ret;
    }


    public double getSystemTimeJava() {
        AgentJAgent.getLock().lock();

        double time=0.0;
        try {
           time=getSystemTime();
        } finally {
            AgentJAgent.getLock().unlock();
        }
        return time;
    }

    /**
     * Sends a TCL command to evaluate within NS2.
     *
     * @param cmd
     * @return true or false on whether it was executed ok or not.
     */
    private synchronized native boolean tclEvalf(String cmd);

    /**
     * Gets the system time for NS2 (in seconds)
     *
     * @return
     */
    private synchronized native double getSystemTime();
}
