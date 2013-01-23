package util;

import agentj.thread.Controller;
import agentj.thread.ReleaseSafeSync;
import proto.logging.api.Log;
import proto.logging.api.LogProperties;
import proto.logging.api.LogProcessor;
import proto.logging.impl.LoggerProcessor;
import proto.logging.impl.XmlLogProcessor;
import proto.logging.impl.HtmlLogProcessor;

import java.net.UnknownHostException;
import java.io.File;

/**
 * @author Ian Taylor
 *
 * Simple logging facility for enabling STD output or not. Interfaced
 * through a method so we can change method at a later stage here
 * and not affect the implementation.
 *
 */
public class Logging {

    public static final String LOG_NAME = "AJLog";

    static {


        try {
// set up the logger.
            // this adds standard Java logging functionality to system.out or err.
            Log.addLogProcessor(new LoggerProcessor(LOG_NAME));
            // this adds xml logging
            // if you use this, you must tell Log to close
            File logDir = new File("agentjlogs");
            logDir.mkdirs();
            XmlLogProcessor xmlp = new XmlLogProcessor(logDir, LOG_NAME);
            Log.addLogProcessor(xmlp);

            HtmlLogProcessor htmlprocessor = new HtmlLogProcessor(logDir, LOG_NAME);
            htmlprocessor.setColor("Thread Status", HtmlLogProcessor.Colors.Coral);
            htmlprocessor.setColor("Monitor Status", HtmlLogProcessor.Colors.LawnGreen);

            Log.addLogProcessor(htmlprocessor);
            // uncomment this to disable logging
            //Log.setDisabled(true);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
    
    public enum ThreadStatusType {PAUSED, RESUMED, STOPPED, STARTED};
    public enum MonitorStatusType {WAIT, NOTIFYING, RELEASED};
    public enum ConditionStatusType {AWAIT, AWAIT_RELEASED, SIGNAL, SIGNALALL};
    public enum ParkStatusType {PARK, UNPARK, PARKRELEASED};
    public enum WorkStatusType {WAITING, RELEASING, RELEASED};

    static boolean logging=true;


    public static boolean isEnabled() {
        return logging;
    }

    public static void setEnabled(boolean state) {
        logging=state;
    }

    /**
     * For testing purposes ...
     */
    public static void println() {
        System.out.println("Hello Just testing if I can find a class");
        System.exit(0);
    }

    /**
     * Outputs the thread status within agentj
     * 
     * @param controller
     * @param type
     * @throws UnknownHostException
     */
    public static void threadStatus(Controller controller, ThreadStatusType type, String object) {
        Log log = new Log(LOG_NAME, Log.Type.STATUS);
        String actionType = getThreadStatusString(type);
        log.setAction(actionType);
        log.setRole("AgentJ Threads");

        LogProperties props = new LogProperties("Thread Status");

        if (controller.getLocalHost()!=null)
            props.put("NS2 Node", controller.getLocalHost());

        props.put("Thread Count", String.valueOf(controller.getThreadMonitor().getThreadCount()));

        if (object!=null)
            props.put("Target Object", object);
        
        log.addLogProperties(props);
        log.logStart();
    }

    /**
      * Outputs the monitor status within agentj
      *
      * @param controller
      * @param type
      * @throws UnknownHostException
      */
     public static void monitorStatus(Controller controller, MonitorStatusType type, String object) {
         Log log = new Log(LOG_NAME, Log.Type.STATUS);
         String actionType = getMonitorStatusString(type);
         log.setAction(actionType);
         log.setRole("AgentJ Monitor");

         LogProperties props = new LogProperties("Monitor Status");
         props.put("NS2 Node", controller.getLocalHost());
         props.put("Thread Count", String.valueOf(controller.getThreadMonitor().getThreadCount()));
         if (object!=null)
             props.put("Target Object", object);

         log.addLogProperties(props);
         log.logStart();
     }

    /**
      * Outputs a condition status within agentj
      *
      * @param controller
      * @param type
      * @throws UnknownHostException
      */
     public static void conditionStatus(Controller controller, ConditionStatusType type, String object) {
         Log log = new Log(LOG_NAME, Log.Type.STATUS);
         String actionType = getConditionStatusString(type);
         log.setAction(actionType);
         log.setRole("AgentJ Condition");

         LogProperties props = new LogProperties("Condition Status");
         props.put("Ns2 Node", controller.getLocalHost());
         props.put("Thread Count", String.valueOf(controller.getThreadMonitor().getThreadCount()));
         if (object!=null)
             props.put("Target Object", object);

         log.addLogProperties(props);
         log.logStart();
     }

    public static void parkStatus(Controller controller, ParkStatusType  type, String object) {
        Log log = new Log(LOG_NAME, Log.Type.STATUS);
        String actionType = getParkStatusString(type);
        log.setAction(actionType);
        log.setRole("AgentJ Park");

        LogProperties props = new LogProperties("Park Status");
        props.put("Ns2 Node", controller.getLocalHost());
        props.put("Thread Count", String.valueOf(controller.getThreadMonitor().getThreadCount()));
        if (object!=null)
            props.put("Target Object", object);

        log.addLogProperties(props);
        log.logStart();
    }

    public static void workStatus(ReleaseSafeSync sync, Controller controller, WorkStatusType  type, String object) {
        Log log = new Log(LOG_NAME, Log.Type.STATUS);
        String actionType = getWorkStatusString(type);
        log.setAction(actionType);
        log.setRole("AgentJ Worker Status");

        LogProperties props = new LogProperties("Work Status");
        props.put("Ns2 Node", controller.getLocalHost());
        props.put("Thread Count", String.valueOf(controller.getThreadMonitor().getThreadCount()));
        props.put("Worker Queue is ", String.valueOf(sync.getWorkersWaiting()) );
        if (object!=null)
            props.put("Target Object", object);

        log.addLogProperties(props);
        log.logStart();
    }

    private static String getWorkStatusString(WorkStatusType type) {
         switch (type) {
             case WAITING: return "WAITING FOR WORKERS";
             case RELEASING: return "RELEASING WORKER";
             case RELEASED: return "RELEASED WORKER";
             default: return "THREAD STATUS ERROR";
         }
     }

    private static String getParkStatusString(ParkStatusType type) {
         switch (type) {
             case PARK: return "Park";
             case UNPARK: return "Unpark";
             case PARKRELEASED: return "ParkReleased";
             default: return "THREAD STATUS ERROR";
         }
     }

    private static String getThreadStatusString(ThreadStatusType type) {
         switch (type) {
             case PAUSED: return "Paused";
             case RESUMED: return "Resumed";
             case STOPPED: return "Stopped";
             case STARTED: return "Started";
             default: return "THREAD STATUS ERROR";
         }
     }

    private static String getMonitorStatusString(MonitorStatusType type) {
         switch (type) {
             case WAIT: return "Wait";
             case NOTIFYING: return "Notifying";
             case RELEASED: return "Released";
             default: return "MONITOR";
         }
     }

    private static String getConditionStatusString(ConditionStatusType type) {
         switch (type) {
             case AWAIT: return "Await";
             case AWAIT_RELEASED: return "Await Released";
             case SIGNAL: return "Signal";
             case SIGNALALL: return "SignalAll";
             default: return "CONDITION ERROR";
         }
     }

    /**
     * Need to call this before exiting to close the log file.

     */
    public static void closeLog () {
        Log.setDisabled(true);
        Log.close();
    }
}
