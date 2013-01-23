package agentj.instrument;

import proto.logging.api.Log;
import proto.logging.api.Logger;

import java.lang.instrument.*;
import java.security.ProtectionDomain;
import java.util.Vector;


/**
 * The is the AgentJ java instrumentation class implementation, which allows AgentJ to transform
 * the classes before they get loaded into the JVM.  Here, we apply a two pronged strategy
 * for swapping method invocations (using Javassist) and direct bytecode rewriting to
 * swap out the necessary methods and classes from the core JVM so we can re-implement these
 * for ns-2.
 *
 * <p/>
 * Created by Ian Taylor
 * Date: Sep 17, 2008
 * Time: 12:26:25 PM
 */
public class TransformClasses implements ClassFileTransformer {

    static Logger logger = Log.getLogger();

    public static boolean debugInstrumentation=false;
    private Instrumentation i;
    static Vector<String> loaded = new Vector<String>();

    
    public TransformClasses(Instrumentation i) {
        this.i = i;
    }

    public byte[] transform(ClassLoader loader, String theClassName, Class redefiningClass, ProtectionDomain domain, byte[] bytes) throws IllegalClassFormatException {

        String className = theClassName.replace('/', '.');
        if (debugInstrumentation)
            logger.debug("Instrument Agent --> class " + className);

        if (debugInstrumentation) {
            if(redefiningClass != null) {
                logger.debug("Instrument Agent --> CLASS IS BEING REDEFINED");
            }
        }

        if (className.equals("java.lang.Shutdown")) {
            logger.debug("Instrument Agent --> detected shutdown ...");
        }

        if (loaded.contains(className)) {
            if (debugInstrumentation)
                logger.debug("Instrument Agent --> already loaded this class " + className);
            return null;
        }

        //logger.debug("Instrument Agent --> classloader " + loader);

        if (isIgnored(className)) {
            if (debugInstrumentation)
                logger.debug("Instrument Agent --> class is being ignored " + className);
            loaded.add(className);
            return null;
        }

        if (debugInstrumentation)
            logger.debug("Instrument Agent --> " + className + " not ignored, processing... ");

        byte[] newBytes = JavassistRewrite.rewrite(bytes);
        newBytes = BytecodeRewrite.rewrite(className, newBytes);

        if (debugInstrumentation)
            logger.debug("Instrument Agent --> " + className + " being added to loaded class list... ");

        loaded.add(className);
 
        if (debugInstrumentation)
             logger.debug("Instrument Agent --> Finished " + className);

        return newBytes;
    }


    
    public static void premain(String agentArgument,
                               Instrumentation i) {
        try {
            //FindMissingClassesTransformer cft = new FindMissingClassesTransformer(i);
            //cft.debugInstrumentation=debugInstrumentation;
            //i.addTransformer(cft);
            //i.addTransformer(new TransformClasses(i));
            if (debugInstrumentation) {
                //logger.debug("Logging set to  " + System.getProperty("instrumentation.logging"));
                //debugInstrumentation=Boolean.parseBoolean(System.getProperty("instrumentation.logging"));
                //logger.debug("Instrument Debugging set to " + debugInstrumentation);

                logger.debug("Instrument Agent --> redefine support = " + i.isRedefineClassesSupported());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    static boolean isIgnored(String className) {

        for(int i=0; i < ClassList.ignored_packages.length; i++)
             if(className.startsWith(ClassList.ignored_packages[i]))
                  return true;

        for(int i=0; i < ClassList.ignored_agentj_packages.length; i++)
             if(className.startsWith(ClassList.ignored_agentj_packages[i]))
                 return true;

        for (String ignored : ClassList.ignoredClasses) {
            if(className.equals(ignored)) {
                return true;
            }
        }
        return false;
    }

    static boolean isIgnoredPackage(String className) {

        for (int i = 0; i < ClassList.full_ignored_packages.length; i++)
            if (className.startsWith(ClassList.full_ignored_packages[i]))
                return true;

        return false;
    }

}
