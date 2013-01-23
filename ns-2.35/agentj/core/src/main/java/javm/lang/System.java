package javm.lang;

import agentj.AgentJVirtualMachine;
import agentj.AgentJAgent;
import proto.logging.api.Logger;
import proto.logging.api.Log;

/**
 * The ... class ...
 * <p/>
 * Created by scmijt
 * Date: Jul 22, 2007
 * Time: 1:06:18 PM
 */
public class System {
    static Logger logger = Log.getLogger(System.class);

    /**
     * Re-implements the system time method in java.lang.System to call our native
     * implementation instead in protolib
     *
     * @return current time in milliseconds from the start of the simulation
     */
    public static long currentTimeMillis() {
        AgentJAgent agent = AgentJVirtualMachine.getCurrentExecutingAgent();

        if (agent==null) {
            logger.fatal("No Agent available for executing getSystemTime()");
            logger.fatal("Note that System.currentTimemillis() cannot be invokved within an agent's constructor");
            java.lang.System.exit(0);
        }

        double secs = agent.getSystemTimeJava();

        //java.lang.System.out.println("System.currentTimeMillis: Time is " + secs);

        return (long)(secs*1000.0);
    }

}
