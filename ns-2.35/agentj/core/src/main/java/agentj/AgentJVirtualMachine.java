package agentj;

import agentj.instrument.AgentJClassLoader;
import agentj.instrument.TransformClassLoader;
import agentj.thread.Controller;
import proto.logging.api.Log;
import proto.logging.api.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.security.Security;

/**
 * This is a broker class for creating Java agentJObjectItems and
 * allowing them to be attached to NS-2 agents.  The AgentJVirtualMachine
 * can use the PAI Native implementations (in C++) in order
 * to communicate with other nodes (if used within the NS-2 mode)
 * or to communicate with other distributed computers in the
 * networked mode.
 * <p/>
 * We use the logger build into JDK for simplicity in the new version.
 * <p/>
 * The levels are SEVERE, WARNING, INFO, CONFIG, FINE, FINER, and FINEST.
 * <p/>
 * User: scmijt
 * Date: Mar 26, 2004
 * Time: 4:16:44 PM
 * To change this template use Options | File Templates.
 */
public class AgentJVirtualMachine {

    public static String agentJHome;

    static int idCount = 0;

    private static Ns2Node currentNs2Node;

    static Logger logger;
    public static final int ERROR = 0;
    public static final int OK = 1;

    protected static AgentJObjectHashtable agentJObjectItems;
    protected static String delimiter = " "; // set delimiter to space by default

    static String nsSchedulerPtr = null;

    static {
        agentJHome = System.getenv("AGENTJ");
        Properties props = new Properties();
        InputStream in = null;
        String f = System.getProperty("native.logging.properties");
        if (f != null) {
            try {
                in = new FileInputStream(f);
            } catch (Exception e) {

            }
        }
        if (in != null) {
            try {
                props.load(in);
                String nativeDebug = props.getProperty("native.debug");
                if (nativeDebug != null) {
                    try {
                        AgentJAgent.nativeDebugLevel = Integer.parseInt(nativeDebug);
                    } catch (NumberFormatException e) {
                        System.err.println("Format problem with reading the ");
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        agentJObjectItems = new AgentJObjectHashtable();
        logger = Log.getLogger();
        logger.info("+++++  AgentJ library loading ... ++++++");

        System.loadLibrary("agentj");
        logger.info("+++++  AgentJ library loaded successfully +++++");
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        if (loader instanceof AgentJClassLoader) {
            ((AgentJClassLoader) loader).setChild((TransformClassLoader) TransformClassLoader.getLoader());
        }

        startNetworkServices();
    }

    public static void startNetworkServices() {
        // Now after an agent has been created, do the factory for the sockets

        AgentJSocketFactory.deployFactories();

        // load Agentj DNS server and trun off local caching ...

        System.setProperty("sun.net.spi.nameservice.provider.1", "dns,AgentJDNS");
        Security.setProperty("networkaddress.cache.ttl", "0");
        Security.setProperty("networkaddress.cache.negative.ttl", "0");

        logger.info("+++++  AgentJ DNS System loaded successfully +++++");
    }

    /**
     * Creates a Java object from the given class name and adds it to an internal
     * Hashtable, registering as an object that can be accessed via the AgentJVirtualMachine
     * Class.  The id value is the actuaal C++ pointer to the Agent class, which is
     * guarenteed to be unique for the agents.  This implies that there is
     * a necessary restriction of only allowing one Java class to be triggered
     * by a NS2 Agent.
     * <p/>
     * Overrides basic functionality - duplicate of BaseVM
     * but extends the PAI sections.
     *
     * @param args - an array containing the parameters to attach the Java agent to an NS2 node
     *             <p/>
     *             args[0] = className - is the Java class name that should be instansiated and attached to this node
     *             args[1] = id - is the pointer to the C+ AgentJ agent
     *             args[2] = nodeAddress - is the Ns-2 address for this node i.e. network address
     * @return OK or ERROR
     */
    public static AgentJAgent attachJavaAgent(String[] args)
            throws InstantiationException, IllegalAccessException,
            ClassNotFoundException {
        logger.trace("Entering");

        logger.debug("Attaching Java class now !!!!!!!!!!!");
        // New controller for this node - only one agent per node is allowed ...

        Class c = null;
        String javaClass = args[0];
        String nsAgentPtrString = args[1];
        String hostName = args[2];

        Controller controller = new Controller();
        Ns2Node nsnode = new Ns2MobileNode(hostName, controller);
        controller.setNodePropperties(nsnode);
        setCurrentNode(nsnode); // sets current AgentJ node running

        AgentJObjectItem item;

        logger.trace("NS-2 Object with ID " + nsAgentPtrString + " Being Registered");

        try {
            // Here instantiate a ClassLoader - segements each agent into a different namespace
            // Classload loader = ClassLoader.getSystemClassLoader() or instantiate our own
            // loader.loadClass(javaClass);
            c = TransformClassLoader.getClass(javaClass);
        } catch (ClassNotFoundException cnf) {
            logger.error("AgentJVirtualMachine.java:  Class " + javaClass + " NOT FOUND\n" +
                    "TIP: Check the your classpath and name of class\n" +
                    "TIP: Check you have inclued the correct Java package");
            throw cnf;
        }

        logger.trace("Class = " + c.getName());

        Object tobj = null;

        try {
            tobj = c.newInstance();
        } catch (InstantiationException ie) {
            logger.error("AgentJVirtualMachine.java:  Class " + javaClass + " could not be instantiated"
                    + "\nTIP: Does it extend the AgentJNode ?");
            throw ie;
        } catch (IllegalAccessException iae) {
            logger.error("AgentJVirtualMachine.java:  Class " + javaClass + " threw an illegal Access Exception"
                    + "\nTIP: is the class public ?");
            throw iae;
        } catch (Exception ie) {
            ie.printStackTrace();
        }

        AgentJAgent agentJObject = null;

        try {
            logger.trace("Object is " + tobj.getClass().getName());
            agentJObject = (AgentJAgent) tobj;
        } catch (ClassCastException e) {
            logger.fatal("Class " + tobj.getClass().getName() + " does not implement necessary interface, agentj.AgentJNode");
            e.printStackTrace();
            System.exit(1);
        }

        logger.trace("Registered classname: " + javaClass);

        long nsAgentPtr = Long.parseLong(nsAgentPtrString.substring(2), 16);

        // set local variables

        nsnode.setAgent(agentJObject);
        agentJObject.ns2Node = nsnode;

        controller.setAgentProperties(nsAgentPtr, agentJObject);
        agentJObject.controller = controller; // set the controller for the node.
        agentJObject.id = idCount;
        ++idCount;
        agentJObject._nsAgentPtr = nsAgentPtr; // sets the agentJ pointer for this node

        item = new AgentJObjectItem(agentJObject);
        item.setID(nsAgentPtrString);
        item.setController(controller);

        agentJObjectItems.put(nsAgentPtrString, item);
        logger.trace("Exiting");

        return agentJObject;
    }


    static String getDelimiter() {
        return delimiter;
    }

    /**
     * invokes the command, which the given arguments on the object identified by the
     * supplied identifier
     *
     * @param args: args[0] = id; args[1]= the command to be performed and
     *              args[2] is the arguments for that command.
     * @return
     */
    public static boolean invokeCommand(String args[]) {
        logger.trace("Entering");
        boolean shutdown = false;

        String nsNodePtrAsString = args[0];
        String command = args[1];
        String progArgs = args[2];

        logger.trace("Command: " + command + ", args = " + progArgs);

        if (command.equals("setDelimiter")) { // special case
            delimiter = progArgs; // just set the delimiter and return
            return true;
        } else if (command.equals("enable-wireless-multicast")) { // special case
            logger.info("++++++++++Enabling Wireless Multicast+++++++++++++");
            System.setProperty("ns2.wirelessmode", "true");
            return true;
        } else if (command.equals("shutdown")) {
            if (progArgs.equals("please")) { // joke ... but also
                // just to check if someone doesn't use
                // "finishAgent" for something else ...
                shutdown = true;
                if (agentJObjectItems.isClosedDown()) // another node has already closed, we are done
                    return true;
            }
        }

        AgentJAgent agentJAgent = null;

        AgentJObjectItem item = (AgentJObjectItem) agentJObjectItems.get(nsNodePtrAsString);
        // gets the Java object item containing our java object for this id

        if (item != null)
            agentJAgent = item.getAgentJObject();
        else {
            logger.fatal("NS-2 Object with ID " + nsNodePtrAsString + "NOT FOUND!!!: Details:");
            logger.fatal("command: " + command);
            logger.fatal("Program Arguments: " + progArgs);

            for (Enumeration e = agentJObjectItems.elements(); e.hasMoreElements();) {
                logger.fatal("AgentJObjectItem Details: " + e.nextElement());
            }

            System.exit(1);
        }

        if (agentJAgent == null) {
            logger.fatal("Cannot find AgentJAgent for command ");
            logger.fatal("Cannot Continue ...");
            System.exit(1);
        }

        setCurrentNodeforAgent(agentJAgent); // sets current AgentJ node running

        if (shutdown) { // finishAgent is invoked
            agentJAgent.shutdown();
            logger.trace("Exiting");
	    return true;
        }


        logger.trace("Exiting");

        // use the controller to execute the command

        return item.getController().executeCommand(agentJAgent, command, progArgs);
    }

    /**
     * Unregisters the given Java object and frees the reference
     *
     * @param id
     * @return
     */
    public static int dettachJavaAgent(String id) {
        logger.trace("Entering");

        if (agentJObjectItems.remove(id) == null)
            return ERROR; // Not able to remove this object - already deleted perhaps ?

        logger.trace("Exiting");
        return OK;
    }

    /**
     * Central method that is called when an agentj node receives a finish command. Can be used
     * to clean up some things.  Note though this is called upon every node cleanup to protect
     * yourself.  Also, the nodes' finish method is invoked too if you want more specific
     * behaviour.
     */
    public static void finish() {
        logger.trace("Entering");
        agentJObjectItems.closeDown();
        logger.trace("Exiting");
    }


    /**
     * Sets the current node executing - used for callbacks
     *
     * @param agent
     */
    public static void setCurrentNodeforAgent(AgentJAgent agent) {
        currentNs2Node = agent.getNs2Node();

    }

    /**
     * Sets the current node executing - used for callbacks
     *
     * @param node
     */
    public static void setCurrentNode(Ns2Node node) {
        currentNs2Node = node;

    }

    /**
     * Gets the NS2 node that is cuurrently processing. This is called
     * from within the execution of a node, so it will return an ID
     * reference to the node which is currently executing.  This is useful
     * within functions, which have no direct access to AgentJ e.g.
     * the receive function in DatagramSocket needs to figure out who
     * issued the call, so it can contact the worker and controller for this
     * node to coordinate the asynchronous blocking of the Java
     * receive call.
     *
     * @return the current NS2 node that is in focus
     */
    public static Ns2Node getCurrentAgentJNode() {
        return currentNs2Node;
    }

    /**
     * Gets the agent with the given node ID
     * @param id 
     * @returns Agent
     */
    public static AgentJAgent getAgentById(int id){
        AgentJObjectItem item = agentJObjectItems.getItemById(id);
        if (item != null)
            return item.getAgentJObject();
        return null;
    }

    public static AgentJAgent getCurrentExecutingAgent() {
        return currentNs2Node.getAgent();
    }

    public static Controller getCurrentNS2NodeController() {
        if (currentNs2Node == null) return null;
        else return currentNs2Node.getController();
    }

    public static String getAgentJHome() {
        return agentJHome;
    }

}
