package agentj;

import agentj.routing.AgentJRouter;
import agentj.dns.Addressing;
import agentj.thread.AgentJEventQueue;
import agentj.thread.Controller;
import agentj.thread.Worker;
import proto.logging.api.Log;
import proto.logging.api.Logger;
import util.StringSplitter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Inet6Address;
import java.net.Inet4Address;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Lock;

/**
 * AgentJNode is a AgentJ's base class for a AgentJ Java object.
 * AjentJObject therefore contains methods
 * that need to be able to be called on your Java object.
 *
 * @author Ian Taylor
 *         User: scmijt
 *         Date: Mar 26, 2004
 *         Time: 4:30:15 PM
 */
public abstract class AgentJAgent implements Worker {

    private final static ReentrantLock lock = new ReentrantLock();

    static int nativeDebugLevel = -1;

    // for future use...
    long nsNode_;

    NAMCommands namCommands = new NAMCommands(this);

    static String groupAddress = "-1";

    static protected Logger logger = Log.getLogger();

    static { //  create object store and load dynamic library
        logger.debug("Entering");
        AgentJEventQueue.getInstance(); // Set Default Event Queue
        logger.debug("Exiting");
    }

// PLOG DEBUG Levels

    private static final int PL_FATAL = 0; // The FATAL level designates very severe error events that will presumably lead the application to abort.
    private static final int PL_ERROR = 1; //The ERROR level designates error events that might still allow the application to continue running.
    private static final int PL_WARN = 2; // The WARN level designates potentially harmful situations.
    private static final int PL_INFO = 3; // The INFO level designates informational messages that highlight the progress of the application at coarse-grained level.
    private static final int PL_DEBUG = 4; // The DEBUG Level designates fine-grained informational events that are most useful to debug an application.
    private static final int PL_TRACE = 5; // The TRACE Level designates finer-grained informational events than the DEBUG
    private static final int PL_DETAIL = 6; // The TRACE Level designates even finer-grained informational events than the DEBUG
    private static final int PL_MAX = 7; // Turn all comments on

    public enum AgentJDebugLevel {
        fatal, error, warn, info, debug, trace, detail, max
    };

    public long _nsAgentPtr=0; // the pointer to our JNI C++ NS Agent that this socket lives on

    int id; // set by AgentJVirtual Machine upon construction

    Controller controller; // set by AgentJVirtual Machine upon construction
    Ns2Node ns2Node; // set by AgentJVirtual Machine upon construction

    public AgentJAgent() {
        if (nativeDebugLevel != -1)
            this.setNativeDebugLevel(nativeDebugLevel);
    }

    /**
     * Gets the node ID - just a simple object count on number of nodes created. This
     * node is the n'th node.
     *
     * @return the node's id
     */
    public int getID() {
        return id;
    }

    /**
     * Called when a node's finish method is called form tcl.  Override for specific behaviour.
     */
    public void shutdown() {
    }


    /**
     * @return the variable name in TCL for the node which this agent is on.
     */
    public String getNodeTCLName() {
        return tclEvaluate(getAgentTCLNameJava() + " set node_");
    }

    /**
     * Gets the NAMCommand object for this node that allows you to set colors and so
     * forth on your NAM animations.
     *
     * @return the NAMCommands object
     */
    public NAMCommands getNamCommands() {
        return namCommands;
    }



    /**
     * Sets the debug level for the native C++ code to one of the values specified in this class. The
     * default is PL_ERROR. These are the choices:
     * <p/>
     * <p/>
     * fatal // The FATAL level designates very severe error events that will presumably lead the application to abort.
     * error //The ERROR level designates error events that might still allow the application to continue running.
     * warn // The WARN level designates potentially harmful situations.
     * info // The INFO level designates informational messages that highlight the progress of the application at coarse-grained level.
     * debug // The DEBUG Level designates fine-grained informational events that are most useful to debug an application.
     * trace // The TRACE Level designates finer-grained informational events than the DEBUG
     * detail // The TRACE Level designates even finer-grained informational events than the DEBUG
     * max // Turn all comments on
     */
    public void setNativeDebugLevel(AgentJDebugLevel level) {
        int protolibDebug;

        switch (level) {
            case fatal:
                protolibDebug = PL_FATAL;
                break;
            case error:
                protolibDebug = PL_ERROR;
                break;
            case warn:
                protolibDebug = PL_WARN;
                break;
            case info:
                protolibDebug = PL_INFO;
                break;
            case debug:
                protolibDebug = PL_DEBUG;
                break;
            case trace:
                protolibDebug = PL_TRACE;
                break;
            case detail:
                protolibDebug = PL_DETAIL;
                break;
            case max:
                protolibDebug = PL_MAX;
                break;
            default:
                logger.error("Invalid AgentJ Debug Level, setting to ERROR level instead!!");
                protolibDebug = PL_ERROR;
        }

        try {
            setNativeDebugLevel(protolibDebug);
        } catch (Error e) {
            if (_nsAgentPtr!=0) { // running in ns-2 so this is a serious error ...
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                System.exit(0);
            } else {
                if (e instanceof UnsatisfiedLinkError)
                    System.out.println("WARNING: Can't invoke remote method setNativeDebugLevel() ... " +
                        "\nIf this is a unit test ran outside ns-2, this is expected");
            }
        }
    }



    /**
     * Sets the logging level
     *
     * @param level
     * @see proto.logging.api.Log
     */
    public void setJavaDebugLevel(Logger.LogLevel level) {
        logger.setLogLevel(level);
    }

    /**
     * Get's the controller for this node
     *
     * @return the controller
     */
    public Controller getController() {
        return controller;
    }

    /**
     * Gets the ns-2 node that this agent is attached to
     *
     * @return the Ns2Node object
     */
    public Ns2Node getNs2Node() {
        return ns2Node;
    }

    /**
     * Gets the ns-2 mobile node that this agent is attached to
     *
     * @return the Ns2Node object
     */
    public Ns2MobileNode getNs2MobileNode() {
        if (!(ns2Node instanceof Ns2MobileNode))
            throw new IllegalArgumentException("This is not a mobile node. Please use getNs2Node()");
        return (Ns2MobileNode) ns2Node;
    }

    /**
     * This is a static method that can be used to find the currently running node i.e. the local host for the
     * host that has code running at this point in the simulation ...
     *
     * @return
     * @throws Exception
     */
    public static final InetAddress getLocalHost() throws Exception {
        String localhost=null;
        try {
            java.lang.Class cls = Class.forName("agentj.AgentJVirtualMachine", false, Thread.currentThread().getContextClassLoader());
            java.lang.reflect.Method m = cls.getMethod("getCurrentNS2NodeController", null);
            Object controller = m.invoke(null, null);
            if (controller!=null) {
                java.lang.reflect.Method node = controller.getClass().getMethod("getLocalHost", null);
                localhost = (String)node.invoke(controller, null);
            } else {
                System.out.println("WARNING: Can't run GetLocalHost!!()");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (localhost == null){
            return null;
        }
        if (Boolean.parseBoolean(System.getProperty("java.net.preferIPv6Stack"))){
            byte[] bytes = Addressing.nsToNumericFormatV6(localhost);
            InetAddress addr = InetAddress.getByAddress(bytes);
            return addr;
        }
        
        return Inet4Address.getByName(localhost);
    }




    /**
     * Determines whether simulations is a MANET simulation or not.
     *
     * @return true is simulation is MANET
     */
    public static boolean isSimulationMANET() {

        String multicastSet = AgentJAgent.tclEvaluateOnSimulator("info vars"); // alocate an Ns-2 group address
        
        logger.debug("Multicast info vars = " + multicastSet);

        if (multicastSet.toLowerCase().indexOf("channel_") != -1)
            return true; // Wireless nets have channels ...
        else
            return false; // and wired don't ...
    }
    

    /**
     * The work performed to run a command
     */
    final public void doWork(String... variables) {
        executeCommand(variables[0], variables[1]);
    }

    private void executeCommand(String command, String progArgs) {
        // Split up argument list

        String[] delims = new String[1];
        delims[0] = AgentJVirtualMachine.getDelimiter();

        StringSplitter s = new StringSplitter(progArgs, delims);

        String[] splitArgs = new String[s.size()];

        for (int i = 0; i < s.size(); ++i)
            splitArgs[i] = s.at(i);

        // process commands

        if (command.equals("setRouterAgent")) {
            if (!isRouterAgent()) {
                logger.fatal("Java Agent does not implement AgentJRouter!");
                System.exit(1);
            }
            String nativeRouter = splitArgs[0];

            logger.info("AgentJRouter Logger set for node " + getNs2Node().getHostName());
            logger.info("Native router is " + nativeRouter);

            // do native side
            tclEvaluate(getAgentTCLNameJava() + " setNativeAgentRouter " + nativeRouter);
            tclEvaluate(getAgentTCLNameJava() + " setNativeRouterPort " + ((AgentJRouter) this).getRoutingPort());

            storeReference();
            controller.setRetVal(true);
            return;
        }

        // else send to the agent for processing

        logger.debug("Executing Java command " + command + " now on agent");

        boolean retVal = command(command, splitArgs);

        logger.debug("Finished Executing Java command");

        controller.setRetVal(retVal);
    }

    /**
     * stores a reference to the Java Agent in the C++ code of AgentJRouter
     */
    private native void storeReference();

    /**
     * Returns true if this agent implements AgentJRouter
     *
     * @return true or false
     */
    public boolean isRouterAgent() {
        return (this instanceof AgentJRouter);
    }

    /**
     * Enables commands invoked in NS2 scripts to be passed along
     * to your Java object. The 'command' is the name of the
     * command and the 'args' are the arguments for the command
     *
     * @param command
     * @param args
     * @return true if the command was successful, false otherwise
     */
    public abstract boolean command(String command, String args[]);

    /**
     * provides a means for an agent to invoke a method that is called on a class
     * that is accessible to the system classloader.
     * This must be a static method with no args and no return type.
     * The class the method is invoked on must be on the system classpath.
     *
     * @param staticClass
     * @param method
     */
    protected static void invokeForSystem(String staticClass, String method) {
        try {
            Class cls = Class.forName(staticClass, true, ClassLoader.getSystemClassLoader());
            Method m = cls.getMethod(method, new Class[0]);
            m.invoke(null, new Object[0]);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }


           /**
     * Returns the name of the NS2 agent
     *
     * @return the return String from the Ns2 name() method i.e. the TCL reference for this agent
     */
    public String getAgentTCLNameJava() {
        lock.lock();
        String ret=null;

        try {
            ret=this.getAgentTCLName();
        } finally {
            lock.unlock();
        }

        return ret;

           }



       /**
     * Gets the system time for NS2 (in seconds)
     *
     * @return
     */
    public double getSystemTimeJava() {
         lock.lock();
        double ret=0.0;

        try {
            ret=this.getSystemTime();
        } finally {
            lock.unlock();
        }

        return ret;
       }

       /**
     * Sends a TCL command to evaluate within NS2 on the agent itself. Can use this directly or use on of the
     * many customised routines that invoke particular TCL NS2 shell commands.
     *
     * @param cmd
     * @return the result.
     */
    public String tclEvaluateOnAgentJava(String cmd) {
        lock.lock();
        String ret=null;

        try {
            ret=this.tclEvaluateOnAgent(cmd);
        } finally {
            lock.unlock();
        }

        return ret;
       }

    /**
     * Sends a TCL command to evaluate within NS2 on the simulator. e.g.
     * Does a $ns command
     *
     * @param cmd
     * @return the result.
     */
    public static String tclEvaluateOnSimulatorJava(String cmd) {
          lock.lock();
        String ret=null;

        try {
            ret=tclEvaluateOnSimulator(cmd);
        } finally {
            lock.unlock();
        }

        return ret;
    }

    /**
     * Sends a TCL command to evaluate within NS2 on the simulator. e.g.
     * Does a $ns command
     *
     * @param cmd
     * @return the result.
     */
    public static String tclEvaluateJava(String cmd) {
        lock.lock();
        String ret=null;

        try {
            ret=tclEvaluate(cmd);
        } finally {
            lock.unlock();
        }

        return ret;
    }

    public void setNativeDebugLevelJava(int level) {
        lock.lock();

        try {
            this.setNativeDebugLevel(level);
        } finally {
            lock.unlock();
        }
    }


    /// native methods

    public static Lock getLock() {
        return lock;
    }

       /**
     * Returns the name of the NS2 agent
     *
     * @return the return String from the Ns2 name() method i.e. the TCL reference for this agent
     */
    private native String getAgentTCLName();


       /**
     * Gets the system time for NS2 (in seconds)
     *
     * @return
     */
    private native double getSystemTime();

       /**
     * Sends a TCL command to evaluate within NS2 on the agent itself. Can use this directly or use on of the
     * many customised routines that invoke particular TCL NS2 shell commands.
     *
     * @param cmd
     * @return the result.
     */
    private native String tclEvaluateOnAgent(String cmd);

    /**
     * Sends a TCL command to evaluate within NS2 on the simulator. e.g.
     * Does a $ns command
     *
     * @param cmd
     * @return the result.
     */
    private native static String tclEvaluateOnSimulator(String cmd);

    /**
     * Sends a TCL command to evaluate within NS2 on the simulator. e.g.
     * Does a $ns command
     *
     * @param cmd
     * @return the result.
     */
    private native static String tclEvaluate(String cmd);

    private native void setNativeDebugLevel(int level);

}
