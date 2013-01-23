package agentj;

import agentj.thread.Controller;
import agentj.thread.ThreadMonitor;

/**
 * Created by IntelliJ IDEA.
 * User: scmijt
 * Date: Jan 11, 2006
 * Time: 9:45:33 AM
 * To change this template use File | Settings | File Templates.
 */
public class AgentJObjectItem {

    private AgentJAgent agentJNode;
    private String ID;
    private Controller controller;
    private ThreadMonitor currentWorker;
    private String ns2Address;

    public AgentJObjectItem(AgentJAgent obj) {
        this.agentJNode =obj;
    }

    public void setID(String id) {
        ID=id;
    }



    public void setController(Controller cont) {
        controller=cont;
    }

    public AgentJAgent getAgentJObject() {
        return agentJNode;
    }

    public String getID() {
        return ID;
    }

    public Controller getController() {
        return controller;
    }

    public String toString() {
	StringBuffer txt = new StringBuffer();
	txt.append("NS-2 Node ID = ");
	txt.append(ID);
	txt.append(" and ClassName = ");
	txt.append(agentJNode.getClass().getName());
	txt.append(" and Ns-2 node ID: ");
	txt.append(agentJNode.getID());
	return txt.toString();
    }
}
