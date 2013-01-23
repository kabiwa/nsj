package agentj.instrument;

import proto.logging.api.Log;
import proto.logging.api.Logger;

import java.lang.instrument.*;
import java.security.ProtectionDomain;
import java.net.URL;

/**
 * The ... class ...
 * <p/>
 * Created by scmijt
 * Date: Oct 9, 2008
 * Time: 12:05:56 PM
 */
public class FindMissingClassesTransformer implements ClassFileTransformer {
    static Logger logger = Log.getLogger();

    public static boolean debugInstrumentation = true;
    private Instrumentation i;


    public FindMissingClassesTransformer(Instrumentation i) {
        this.i = i;
    }


    public byte[] transform(ClassLoader loader, String theClassName, Class redefiningClass,
                            ProtectionDomain domain, byte[] bytes) throws IllegalClassFormatException {

        String className = theClassName.replace('/', '.');

        if (debugInstrumentation)
            logger.debug("Missing Classes Instrument Agent --> class " + className);
        
        Class[] all = i.getAllLoadedClasses();

        for (Class aClass : all) {
            String n = aClass.getName(); // a name with dots ...
            if ((n.startsWith("[")) || (n.startsWith("java."))) {
                continue;
            }
            if (!TransformClasses.loaded.contains(n)) {
                TransformClasses.loaded.add(n);
                if (!TransformClasses.isIgnored(n)) {
                    if (debugInstrumentation)
                        logger.debug("Missing Class Instrument Agent --> rewriting " + n);
                    byte[] cbn = JavassistRewrite.rewrite(bytes);
                    cbn = BytecodeRewrite.rewrite(n, cbn);
                    ClassDefinition cd = new ClassDefinition(aClass, cbn);
                    ClassDefinition classes[] = new ClassDefinition[1];
                    classes[0] = cd;
                    
                    if (debugInstrumentation)
                        logger.debug("Missing Class Instrument Agent --> redefining " + n + " now !!! Loader is=" + loader);
                    try {
                        i.redefineClasses(classes);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    } catch (UnmodifiableClassException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                    if (debugInstrumentation)
                        logger.debug("Missing Class Instrument Agent --> finished redefining " + n);
                }
            }

        }


        if (debugInstrumentation)
            logger.debug("Finished searching for classes for " + className);

        return null;
    }

}
