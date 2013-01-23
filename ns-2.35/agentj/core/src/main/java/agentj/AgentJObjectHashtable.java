package agentj;

import agentj.thread.Controller;

import java.util.Enumeration;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Map;

import proto.logging.api.Log;

/**
 * Created by IntelliJ IDEA.
 * User: scmijt
 * Date: Jan 11, 2006
 * Time: 9:57:43 AM
 * To change this template use File | Settings | File Templates.
 */
public class AgentJObjectHashtable extends Hashtable {

    boolean closedDown=false;
    Hashtable idIndex = new Hashtable(); // maps the agent ID to the AgentJObjectItem

    public AgentJObjectHashtable() {
        //new ShutdownHook().createHook();
    }

    @Override
    public void clear(){
	idIndex.clear();
	super.clear();
    }

    @Override
    public Object put(Object key, Object value){
	Object ret = super.put(key, value);
	AgentJObjectItem item = (AgentJObjectItem) value;
	AgentJAgent agent = item.getAgentJObject();
	if (agent != null)
		idIndex.put(agent.getID(), value);

	return ret;
    }

    @Override
    public Object remove(Object key){
	Object value = get(key);
	if (value == null)
		return null;
	AgentJObjectItem item = (AgentJObjectItem) value;
        AgentJAgent agent = item.getAgentJObject();

	idIndex.remove(agent.getID());
        Object ret = super.remove(key);
	return ret;
    }


    public AgentJObjectItem getItemById(int id){
       Object obj = idIndex.get(id);
       if (obj == null)
		return null;

       AgentJObjectItem item = (AgentJObjectItem) obj;
       return item;
    }

    public AgentJObjectItem getItem(int nsAgentPtr) {
        return (AgentJObjectItem) get(String.valueOf(nsAgentPtr));

    }

    public boolean isClosedDown() {
        return closedDown;
    }

    /**
     * Convenience method to search for the AgentJ object which has the
     * given controller
     *
     * @param controller
     * @return the AgentJ object which has the given controller
     */
    public AgentJObjectItem getItem(Controller controller) {
        AgentJObjectItem item;

        for (Enumeration e = elements(); e.hasMoreElements() ;) {
            item = (AgentJObjectItem)e.nextElement();
            if (item.getController()==controller)
                return item;
            }
        return null;
    }

    /**
     * Convenience method to search for the AgentJ item which represents the
     * given AgentJAgent
     *
     * @param obj - the agentJ object 
     * @return the AgentJ object which has the given AgentJNode
     */
    public AgentJObjectItem getItem(AgentJAgent obj) {
        AgentJObjectItem item;

        for (Enumeration e = elements(); e.hasMoreElements() ;) {
            item = (AgentJObjectItem)e.nextElement();
            if (item.getAgentJObject()==obj)
                return item;
            }
        return null;
    }


    public void closeDown() {
        Log.getLogger().info("Object Hashtable closing down");
        try {
            for (Enumeration e = elements(); e.hasMoreElements();) {
                AgentJObjectItem item = (AgentJObjectItem) e.nextElement();
                item.getAgentJObject().shutdown();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.close();
        clear();
        closedDown=true;
        Log.getLogger().info("Object Hashtable closed down");
    }


    private class ShutdownHook extends Thread {

        private void add() {
            try {
                Method shutdownHook = java.lang.Runtime.class.getMethod("addShutdownHook", new Class[]{java.lang.Thread.class});
                shutdownHook.invoke(Runtime.getRuntime(), new Object[]{this});
                Log.getLogger().info("ShutdownHook.add ENTER");

            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void createHook() {
            add();
        }

        public void run() {
            Log.getLogger().info("AgentJObjectHashtable$ShutdownHook.run ENTER");
            try {
                for (Enumeration e = elements(); e.hasMoreElements();) {
                    AgentJObjectItem item = (AgentJObjectItem) e.nextElement();
                    item.getAgentJObject().shutdown();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(500);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.getLogger().info("AgentJObjectHashtable$ShutdownHook.run EXIT");
            Log.close();
            clear();
        }
    }
}
