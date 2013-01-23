package agentj.instrument;

import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import proto.logging.api.Log;
import proto.logging.api.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.net.URL;

/**
 * The class rewrites the class files using Javassist, which focuses at the method invocation
 * level of java bytecode rewriting.
 * <p/>
 * Created by Ulrich Herberg
 * Date: Sep 17, 2008
 * Time: 12:32:02 PM
 */
public class JavassistRewriteAgentJProfile{

    static Logger logger = Log.getLogger();


    /**
     * Override this method if you want to alter a class before it gets actually
     * loaded. Does nothing by default.
     */
    public static CtClass modifyClass(final CtClass clazz) throws IOException, CannotCompileException {
//		System.out.println("modifyClass: " + clazz.getName());

        // Do Methods

        CtBehavior[] methodsAndConstructors = clazz.getDeclaredBehaviors();

        for (int i = 0; i < methodsAndConstructors.length; ++i) {
            final CtBehavior cm = methodsAndConstructors[i];

            try {
		// added by Ulrich:
		//System.out.println("rewriting method: " + cm.getName());
		if (cm.getMethodInfo().getCodeAttribute() != null){
			cm.addLocalVariable("__before", CtClass.longType);
			cm.insertBefore("{ __before = System.nanoTime(); }"); 
			cm.insertAfter("{ agentj.instrument.ProfileHashtable.methodCalled(\"" + cm.getLongName() + "\", (System.nanoTime() - __before) / 1000); }"); 
		}
		// -------


            } catch (CannotCompileException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (Exception ep) {
                ep.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        //if (TransformClasses.debugInstrumentation)
            logger.debug("Instrument Agent --> finished class...");


        return clazz;
    }
}
