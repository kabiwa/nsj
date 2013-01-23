package agentj.examples.basic;

import agentj.AgentJAgent;
import proto.logging.api.LogProcessor;
import proto.logging.api.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: scmijt
 * Date: Mar 26, 2004
 * Time: 4:48:44 PM
 * To change this template use Options | File Templates.
 */
public class ChangeDelimiter extends AgentJAgent {

    static int count=0;

    int myID;

    public ChangeDelimiter() {
        ++count;
        myID=count;
        this.setJavaDebugLevel(Logger.LogLevel.ALL);
        this.setNativeDebugLevel(AgentJDebugLevel.max);
     }

    public boolean command(String command, String args[]) {

        if (command.equals("hello")) {
            System.out.println("ChangeDelimiter(" + myID + ") has "
                    + args.length + " arguments");
            for (int i=0; i<args.length; ++i) {
                System.out.println("Arg[" + i + "] = " + args[i]);
            }
            return true;
        }

        return false;
    }
}
