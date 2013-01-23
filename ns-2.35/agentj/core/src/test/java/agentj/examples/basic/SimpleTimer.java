package agentj.examples.basic;

import agentj.AgentJAgent;
import proto.logging.api.LogProcessor;
import proto.logging.api.Logger;

/**
 * The ... class ...
 * <p/>
 * Created by scmijt
 * Date: Jan 2, 2008
 * Time: 5:27:29 PM
 */
public class SimpleTimer extends AgentJAgent {
    int repeats=10;

    public SimpleTimer() {
    }

    public void init() {
        setNativeDebugLevel(AgentJDebugLevel.debug);
        setJavaDebugLevel(Logger.LogLevel.ALL);
    }



    public void go() {
        try {
            for (int i=0; i<repeats; ++i ) {

                System.out.println("Simple Timer count - " + i + " at time " + System.currentTimeMillis() + " -------------");
                Thread.sleep(1000);  // wait one second then continue
            }
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }


    public boolean command(String command, String args[]) {
        if (command.equals("init")) {
            init();
            return true;
        }
        else if (command.equals("go")) {
            go();
            return true;
        }
        return false;
    }

    public static void main(String[] args) {
        SimpleTimer sim = new SimpleTimer();
        sim.init();
        sim.go();
    }

}
