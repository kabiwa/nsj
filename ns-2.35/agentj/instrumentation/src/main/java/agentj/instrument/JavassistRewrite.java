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

/**
 * The class rewrites the class files using Javassist, which focuses at the method invocation
 * level of java bytecode rewriting.
 * <p/>
 * Created by Ian Taylor
 * Date: Sep 17, 2008
 * Time: 12:32:02 PM
 */
public class JavassistRewrite {

    static Logger logger = Log.getLogger();

    static ClassPool myPool;

    private static HashMap<String, String> transformed = new HashMap<String, String>();
    private static HashMap<String, String> processing = new HashMap<String, String>();
    static ArrayList<String> paths = new ArrayList<String>();

    final String print = "System.out.println(\"Calling ";

    private static String agentj=null;

    static {
        myPool = new ClassPool(ClassPool.getDefault());

        agentj = System.getenv("AGENTJ");

        if ((agentj==null) || (agentj.length()==0)) {
            System.err.println("AGENTJ environment Variable NOT SET. Can't continue without it");
            System.exit(1);
        }

        String agentjCP = System.getenv("AGENTJ_CLASSPATH");

        String pathsep = System.getProperty("path.separator");

        logger.debug("Setting Up Agentj ClassPath ");
        logger.debug("+++++++++++++++++++++++++++");

        if (agentjCP != null) {
            String[] cppaths = agentjCP.split(pathsep);
            for (String cppath : cppaths) {
                try {
                    if (!paths.contains(cppath)) {
                        paths.add(cppath);
                        //System.out.println("JavassistRewrite.static intializer adding to classpath " + cppath);
                        myPool.insertClassPath(cppath);
                        logger.debug("Adding to Agentj ClassPath: " + cppath);
                    }
                } catch (NotFoundException e) {
                    logger.warn("could not add" + cppath + " to class path because of " + e);
                }
            }
        }

        String appClassPath = System.getProperty("agentj.class.path");
        if (appClassPath != null && appClassPath != "")
            addSearchPathstoClasspath(appClassPath);

        appClassPath = agentj + File.separator + "config" + File.separator + "agentj.class.path"; // add centralized
                                                                        // classpath also
        System.out.println("Agentj User Class path = " + appClassPath);
        
        if (appClassPath != null && appClassPath != "")
            addSearchPathstoClasspath(appClassPath);
        
    }

    private static void addSearchPathstoClasspath(String appClassPath) {
        File cpfile = new File(appClassPath);
        if (cpfile.exists() && cpfile.length() > 0) {
            BufferedReader reader;
            try {
                reader = new BufferedReader(new FileReader(cpfile));
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim().replace('/', File.separatorChar);
                    if (line.length() == 0 || line.startsWith("#")) {
                        continue;
                    }
                    try {
                        if (!paths.contains(line)) {
                            if (!(new File(line).exists())) // try relative path to agentj
                                line = agentj + File.separator + line;

                            if ((new File(line).exists())) {
                                paths.add(line);
                                myPool.insertClassPath(line);
                                System.out.println("Adding to Agentj ClassPath: " + line);
                            }
                        }
                    } catch (NotFoundException e) {
                        logger.warn("could not add" + line + " to class path because of " + e);
                    }
                }
            } catch (FileNotFoundException e) {
                logger.warn("could not find agentj.class.path file!!");
            } catch (IOException e) {
                logger.warn("error reading agentj.class.path file!!");
            }
        }

    }

    /**
     * Main hook for rewriting using Javassist
     *
     * @param className name of class to rewrite
     * @return resulting classbytes
     */
    static byte[] getClassBytes(String className) {
        try {
            CtClass clazz = ClassPool.getDefault().get(className);
            return clazz.toBytecode();
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CannotCompileException e) {
            e.printStackTrace();
        }
        return null;

    }

    /**
     * Main hook for rewriting using Javassist
     *
     * @param cls class to rewrite
     * @return resulting classbytes
     */
    static Class rewrite(Class cls) throws ClassNotFoundException {

        CtClass clazz = null;
        String className = cls.getName();

        try {
            clazz = ClassPool.getDefault().get(className);

            clazz.defrost();

            clazz = modifyClass(clazz);
            byte[] bytes = clazz.toBytecode();
            bytes = BytecodeRewrite.rewrite(className, bytes);
            clazz.detach();
            clazz = ClassPool.getDefault().makeClass(new ByteArrayInputStream(bytes));
            clazz.detach();
            cls = clazz.toClass();
            return cls;
        } catch (Exception e) {
            throw new ClassNotFoundException("cannot find class " + className, e);
        }

    }

    /**
     * Main hook for rewriting using Javassist
     *
     * @return resulting classbytes
     */
    static byte[] rewrite(byte[] bytes) {
        try {
            ClassPool cp = ClassPool.getDefault();

            CtClass clazz = cp.makeClass(new ByteArrayInputStream(bytes));
            //CtClass clazz = myPool.makeClass(new ByteArrayInputStream(bytes));
            //CtClass clazz = myPool.get(className);
            clazz = modifyClass(clazz);
            //newClazz.stopPruning(true);
            bytes = clazz.toBytecode();
            //newClazz.defrost();
            clazz.detach();
            //bytes = modifyClass(clazz);
            //clazz.stopPruning(true);
            //clazz.defrost();
            //byte [] bytes = newClazz.toBytecode();
            //clazz.defrost();

            return bytes;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    private static Class internalLoadClass(String className) throws Exception {
        if (processing.get(className) != null) {
            return new TempClassLoader().loadClass(className);
        }
        return TransformClassLoader.getClass(className);
    }

    static String getPackage(String className) throws NotFoundException {
        CtClass clazz = myPool.get(className);
        return clazz.getPackageName();
    }

    // NOTE: ANDREW. This is what is used as of 22 Oct 2008
    // NOTE!!! This calls BytecodeRewrite.
    static byte[] rewrite(String className) throws ClassNotFoundException {
        byte[] bytes = null;
        try {
            CtClass clazz = myPool.get(className);
            clazz.stopPruning(true);

            if (transformed.get(className) != null) {
                logger.warn("JavassistRewrite.rewrite ***************** WTF????? CLASS BEING LOADED TWICE!!! ****************");
                bytes = clazz.toBytecode();

            }
            processing.put(className, className);

            clazz = modifyClass(clazz);

            bytes = clazz.toBytecode();
            bytes = BytecodeRewrite.rewrite(className, bytes);
            transformed.put(className, className);
            processing.remove(className);

            clazz.defrost();


        } catch (Exception e) {
            //e.printStackTrace();
            throw new ClassNotFoundException("cannot find class " + className, e);
        }
        return bytes;
    }

    static byte[] getClassAsBytes(String className, boolean modify) {
        try {
            CtClass clazz = ClassPool.getDefault().get(className);
            if (modify) {
                clazz.stopPruning(true);
                clazz.defrost();
                clazz = modifyClass(clazz);
            }
            return clazz.toBytecode();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Override this method if you want to alter a class before it gets actually
     * loaded. Does nothing by default.
     */
    private static CtClass modifyClass(final CtClass clazz) throws IOException, CannotCompileException {

        // Do Methods

        CtConstructor initializer = clazz.getClassInitializer();
        try {
            processinitializerCall(initializer, clazz);
        } catch (CannotCompileException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        CtBehavior[] methodsAndConstructors = clazz.getDeclaredBehaviors();

        for (int i = 0; i < methodsAndConstructors.length; ++i) {
            final CtBehavior cm = methodsAndConstructors[i];

            try {
		// added by Ulrich:
		if (cm.getMethodInfo().getCodeAttribute() != null){
			cm.addLocalVariable("__before", CtClass.longType);
			cm.insertBefore("{ __before = System.nanoTime(); }"); 
			cm.insertAfter("{ agentj.instrument.ProfileHashtable.methodCalled(\"" + cm.getLongName() + "\", (System.nanoTime() - __before) / 1000);  }"); 
		}
		// -------


                if (cm.getName().equals("run")) {

                    if (TransformClasses.debugInstrumentation)
                        logger.debug("TTT-> Detected a Run in " + clazz.getName());
                    Class targetclass = internalLoadClass(clazz.getName());
                    boolean isThread = isThread(targetclass);

                    if (isThread) {
                        if (TransformClasses.debugInstrumentation)
                            logger.debug("Instrument Agent --> run Detected - inserting code");
                        cm.insertAfter(invokeThreadStop());
                        /*cm.insertAfter("" +
                                "{ " +
                                "   System.out.println(\"THREAD DETECTION: Detected a run() end - deregistering now\"); " +
                                "   agentj.thread.Controller controller = agentj.AgentJVirtualMachine.getCurrentNS2NodeController(); " +
                                "   if (controller!=null) {" +
                                "       System.out.println(\"Controller is not null, calling register now\"); " +
                                "       controller.getThreadMonitor().registerThreadStop(this); " +
                                "   } else {" +
                                "   System.out.println(\"WARNING: Controller is null, cannot degister thread stop after a run()\"); " +
                                "   }" +
                                "}");
                                */
                    }
                }
                processMethodCalls(cm, clazz);
            } catch (CannotCompileException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (Exception ep) {
                ep.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        if (TransformClasses.debugInstrumentation)
            logger.debug("Instrument Agent --> finished class...");


        return clazz;

    }

    private static String invokeThreadStop() {
        //StringBuilder s = new StringBuilder("{ System.out.println(\"THREAD DETECTION: Detected a run() end - deregistering now\");");
        StringBuilder s = new StringBuilder("{ ");
        s.append(" try { ")
                .append(" java.lang.Class cls = Class.forName(\"agentj.AgentJVirtualMachine\", false, Thread.currentThread().getContextClassLoader());")
                .append(" java.lang.reflect.Method m = cls.getMethod(\"getCurrentNS2NodeController\", null);")
                .append(" Object controller = m.invoke(null, null);")
                .append(" if (controller!=null) {")
                        //.append(" System.out.println(\"Controller is not null, calling register now\");")
                .append(" java.lang.reflect.Method mon = controller.getClass().getMethod(\"getThreadMonitor\", null);")
                .append(" Object monitor = mon.invoke(controller, null);")
                .append(" java.lang.reflect.Method reg = monitor.getClass().getMethod(\"registerThreadStop\", new Class[]{java.lang.Object.class});")
                .append(" reg.invoke(monitor, new Object[]{this});")
                .append(" } else {")
                .append(" System.out.println(\"WARNING: Controller is null, cannot degister thread stop after a run()\");")
                .append(" } ")
                .append(" } catch (Exception ex) { ex.printStackTrace(); } }");
        String str = s.toString();
        if (TransformClasses.debugInstrumentation)
            logger.trace("Instrument Agent --> reflection code = " + str);
        return str;
    }


    private static String invokeThreadStart() {
        //StringBuilder s = new StringBuilder("{ System.out.println(\"THREAD DETECTION: Detected a start() - registering thread now\"); ");
        StringBuilder s = new StringBuilder("{ ");

        s.append(" try { ")
                .append(" java.lang.Class cls = Class.forName(\"agentj.AgentJVirtualMachine\", false, Thread.currentThread().getContextClassLoader());")
                .append(" java.lang.reflect.Method m = cls.getMethod(\"getCurrentNS2NodeController\", null);")
                .append(" Object controller = m.invoke(null, null);")
                .append(" java.lang.reflect.Method mon = controller.getClass().getMethod(\"getThreadMonitor\", null);")
                .append(" Object monitor = mon.invoke(controller, null);")
                .append(" java.lang.reflect.Method reg = monitor.getClass().getMethod(\"registerThreadStart\", new Class[]{java.lang.Object.class});")
                .append(" reg.invoke(monitor, new Object[]{$0});")
                .append(" $_ = $proceed($$); ")
                .append(" } catch (Exception ex) { ex.printStackTrace(); } }");
        String str = s.toString();
        if (TransformClasses.debugInstrumentation)
            logger.trace("Instrument Agent --> reflection code = " + str);
        return str;
    }

    private final static void processinitializerCall(CtConstructor cm, final CtClass clazz) throws CannotCompileException {
        if (TransformClasses.debugInstrumentation)
            logger.trace("Instrument Agent --> Processing class initializer (static {})....");

        if (cm == null) return;

        cm.instrument(new ExprEditor() {
            public void edit(MethodCall m) throws CannotCompileException {
                try {
                    processMethodCall(m, clazz);
                } catch (Exception e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        });
    }

    private final static void processMethodCalls(CtBehavior cm, final CtClass clazz) throws CannotCompileException {
        if (TransformClasses.debugInstrumentation)
            logger.trace("Instrument Agent --> Processing Methods ...");

        cm.instrument(new ExprEditor() {
            public void edit(MethodCall m) throws CannotCompileException {
                try {
                    processMethodCall(m, clazz);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    protected void addIgnoredPkgs(String[] ignored_packages) {
        String[] new_p = new String[ignored_packages.length + ClassList.ignored_packages.length];

        java.lang.System.arraycopy(ClassList.ignored_packages, 0, new_p, 0, ClassList.ignored_packages.length);
        java.lang.System.arraycopy(ignored_packages, 0, new_p, ClassList.ignored_packages.length,
                ignored_packages.length);

        ClassList.ignored_packages = new_p;
    }

    public final static void processMethodCall(MethodCall m, CtClass clazz)
            throws Exception {
        if (TransformClasses.debugInstrumentation)
            logger.trace("Instrument Agent --> Method Call = " + m.getMethodName());

        if (m.isSuper())
            if (TransformClasses.debugInstrumentation)
                logger.trace("Instrument Agent --> is a super method");

        if (m.getMethodName().equals("start")) {
            boolean isTargetAThread = false;

            if (m.getClassName().equals("java.lang.Thread")) {
                isTargetAThread = true;
            } else {

                // NOTE: ANDREW changed this to avoid calling class directly
                // instead uses the CtClass object to introspect.
                //Class targetclass = Class.forName(m.getClassName());
                Class targetclass = internalLoadClass(m.getClassName());

                if (isThread(targetclass)) {
                    isTargetAThread = true;

                }
            }
            if ((isTargetAThread) && (m.getSignature().equals("()V"))) {
                m.replace(invokeThreadStart());

            }
            /*m.replace("{ " +
            "    System.out.println(\"THREAD DETECTION: Detected a start() - registering thread now\"); " +
            "    agentj.thread.Controller controller = agentj.AgentJVirtualMachine.getCurrentNS2NodeController(); " +
            "    controller.getThreadMonitor().registerThreadStart($0); " +
            "    $_ = $proceed($$); " +
            "}");
            */
        } else if (m.getMethodName().equals("interrupt")) {
            boolean isTargetAThread = false;

            if (m.getClassName().equals("java.lang.Thread")) {
                isTargetAThread = true;
            } else {
                Class targetclass = internalLoadClass(m.getClassName());

                if (isThread(targetclass)) {
                    isTargetAThread = true;
                }
            }
            if ((isTargetAThread) && (m.getSignature().equals("()V")))
                m.replace("$_ = ($r)javm.lang.Thread.interrupt($0);");
        } else if (m.getMethodName().equals("currentTimeMillis")) {
            if (TransformClasses.debugInstrumentation)
                logger.trace("Instrument Agent --> System.currentTimeMillis() Rewritten");
            //m.replace("$_ = ($r)javm.lang.System.currentTimeMillis();");
            m.replace(invokeNoArgs("javm.lang.System", "currentTimeMillis"));

        } /*else if (m.getMethodName().equals("getBundle") && m.getClassName().equals("java.util.ResourceBundle")) {
            //if (TransformClasses.debugInstrumentation)
            if ((m.getSignature().equals("(Ljava/lang/String;)Ljava/util/ResourceBundle;"))) {
                if (TransformClasses.debugInstrumentation)
                    logger.trace("Instrument Agent --> ResourceBundle.getBundle() Rewritten");
                m.replace("$_ = ($r)$0.getBundle($1, java.util.Locale.getDefault(), agentj.instrument.TransformClassLoader.getLoader());");
            } else if ((m.getSignature().equals("(Ljava/lang/String;Ljava.util.Locale)Ljava/util/ResourceBundle;"))) {
                if (TransformClasses.debugInstrumentation)
                    logger.trace("Instrument Agent --> ResourceBundle.getBundle() Rewritten");
                m.replace("$_ = ($r)$0.getBundle($1, $2, agentj.instrument.TransformClassLoader.getLoader());");
            }
            //m.replace(invokeNoArgs("javm.lang.System", "currentTimeMillis"));

        } */else if (m.getMethodName().equals("yield")) {
            if (TransformClasses.debugInstrumentation)
                logger.trace("Instrument Agent --> Thread.yield() Rewritten");
            //m.replace("javm.lang.Thread.yield();");
            m.replace(invokeVoidNoArgs("javm.lang.Thread", "yield"));

        } else if (m.getMethodName().equals("sleep")) {
            if (TransformClasses.debugInstrumentation)
                logger.trace("Instrument Agent --> Thread.sleep() Rewritten");

            if (m.getSignature().equals("(J)V")) // sleep()
                //m.replace("javm.lang.Thread.sleep($1);");
                m.replace(invokeVoid("javm.lang.Thread", "sleep", new String[]{"long.class"}, new String[]{"new Long($1)"}));

            else if (m.getSignature().equals("(JI)V")) // sleep()
                //m.replace("javm.lang.Thread.sleep($1, $2);");
                m.replace(invokeVoid("javm.lang.Thread", "sleep", new String[]{"long.class", "int.class"}, new String[]{"new Long($1), new Integer($2)"}));

        } else if (m.getMethodName().equals("wait")) {
            if (TransformClasses.debugInstrumentation)
                logger.trace("Instrument Agent --> wait() signature = " + m.getSignature());
            if (m.getSignature().equals("()V")) {// wait()
                //m.replace("javm.lang.StaticObject.wait($0);");
                m.replace(invokeVoid("javm.lang.StaticObject", "wait", new String[]{"java.lang.Object.class"}, new String[]{"$0"}));

                if (TransformClasses.debugInstrumentation)
                    logger.trace("Instrument Agent --> wait() Rewritten");
            } else if (m.getSignature().equals("(J)V")) { // wait()
                //m.replace("javm.lang.StaticObject.wait($0, $1);");
                m.replace(invokeVoid("javm.lang.StaticObject", "wait", new String[]{"java.lang.Object.class", "long.class"}, new String[]{"$0, new Long($1)"}));

                if (TransformClasses.debugInstrumentation)
                    logger.trace("Instrument Agent --> wait(long) Rewritten");
            } else if (m.getSignature().equals("(JI)V")) { // wait()
                //m.replace("javm.lang.StaticObject.wait($0, $1, $2);");
                m.replace(invokeVoid("javm.lang.StaticObject", "wait", new String[]{"java.lang.Object.class", "long.class", "int.class"}, new String[]{"$0, new Long($1), new Integer($2)"}));

                if (TransformClasses.debugInstrumentation)
                    logger.trace("Instrument Agent --> wait(long,int) Rewritten");
            }
        } else if (m.getMethodName().equals("notify")) {
            if (TransformClasses.debugInstrumentation)
                logger.trace("Instrument Agent --> Thread.notify() Rewritten");
            //m.replace("javm.lang.StaticObject.notify($0);");
            m.replace(invokeVoid("javm.lang.StaticObject", "notify", new String[]{"java.lang.Object.class"}, new String[]{"$0"}));

        } else if (m.getMethodName().equals("notifyAll")) {
            if (TransformClasses.debugInstrumentation)
                logger.trace("Instrument Agent --> Thread.notifyAll() Rewritten");
            //m.replace("javm.lang.StaticObject.notifyAll($0);");
            m.replace(invokeVoid("javm.lang.StaticObject", "notifyAll", new String[]{"java.lang.Object.class"}, new String[]{"$0"}));

        } else if (m.getMethodName().equals("newCondition")) {
            if (TransformClasses.debugInstrumentation)
                logger.trace("Instrument Agent --> Lock.newCondition() Rewritten");
            m.replace("$_ = ($r)javm.util.concurrent.AgentJConditionObject.newCondition($0);");
        } else if (m.getMethodName().equals("interrupt")) {
            if (TransformClasses.debugInstrumentation)
                logger.trace("Instrument Agent --> Lock.newCondition() Rewritten");
            m.replace("$_ = ($r)javm.util.concurrent.AgentJConditionObject.newCondition($0);");
        } else if (m.getMethodName().equals("lock")) {
            Class targetClass = internalLoadClass(m.getClassName());
            Class[] interfaces = targetClass.getInterfaces();

            if (interfaces != null) {
                for (int in = 0; in < interfaces.length; ++in) {
                    if (TransformClasses.debugInstrumentation)
                        logger.debug("TTT-> Checking interface " + interfaces[in].getName());
                    if (interfaces[in].getName().equals("java.util.concurrent.locks.Lock")) {
                        // object is a lock object, rewrite ...
                        m.replace("$_ = ($r)javm.util.concurrent.locks.LockDetector.lock($0);");
                    }

                }
            }
        } else if (m.getMethodName().equals("unlock")) {
            Class targetClass = internalLoadClass(m.getClassName());
            Class[] interfaces = targetClass.getInterfaces();

            if (interfaces != null) {
                for (int in = 0; in < interfaces.length; ++in) {
                    if (TransformClasses.debugInstrumentation)
                        logger.debug("TTT-> Checking interface " + interfaces[in].getName());
                    if (interfaces[in].getName().equals("java.util.concurrent.locks.Lock")) {
                        // object is a lock object, rewrite ...
                        m.replace("$_ = ($r)javm.util.concurrent.locks.LockDetector.unlock($0);");
                    }

                }
            }
        }  else if (m.getMethodName().equals("getLocalHost")) {
            Class targetClass = internalLoadClass(m.getClassName());
            if (targetClass.getName().equals("java.net.InetAddress"))
                 m.replace("$_ = java.net.InetAddress.getByAddress(java.net.InetAddress.getLocalHost().getAddress());");
            }


        if (TransformClasses.debugInstrumentation)
            logger.trace("Finished Method Call...");
    }

    private static String invoke(String cls, String mthd, String[] classes, String[] objects, boolean retVal) {
        String clsString = "java.lang.Class cls = Class.forName(\"" + cls + "\", false, Thread.currentThread().getContextClassLoader());";
        String clss = mergeArray(classes);
        String classArr = "null";
        if (clss != null) {
            classArr = "new Class[]{" + clss + "}";
        }
        String objs = mergeArray(objects);
        String objArr = "null";
        if (objs != null) {
            objArr = "new Object[]{" + objs + "}";
        }
        String mthdString = "java.lang.reflect.Method m = cls.getMethod(\"" + mthd + "\", " + classArr + ");";
        String inv = " m.invoke(null, " + objArr + "); ";
        if (retVal) {
            inv = " $_ = ($r)" + inv;
        }
        String ret = clsString + mthdString + inv;
        if (TransformClasses.debugInstrumentation)
            logger.trace("Instrument Agent --> reflection code = " + ret);
        return ret;
    }

    private static String invokeVoid(String cls, String mthd, String[] classes, String[] objects) {
        return invoke(cls, mthd, classes, objects, false);
    }

    private static String invokeVoidNoArgs(String cls, String mthd) {
        return invoke(cls, mthd, null, null, false);
    }

    private static String invokeNoArgs(String cls, String mthd) {
        return invoke(cls, mthd, null, null, true);
    }


    private static String mergeArray(String[] ss) {
        if (ss == null) {
            return null;
        }

        String s = "";
        for (int i = 0; i < ss.length; i++) {
            s += ss[i];
            if (i < ss.length - 1) {
                s += ", ";
            }
        }
        return s;
    }

    private static boolean isThread(CtClass clazz, boolean checkRunnable) {
        boolean isThread = false;
        String superclass = null;

        CtClass thisclass = clazz;
        do {
            try {
                thisclass = thisclass.getSuperclass();
                superclass = thisclass.getName();
            } catch (NotFoundException e) {
                break;
            }

            // test to see whether the class is either a Thread or is Runnable

            if (TransformClasses.debugInstrumentation)
                logger.debug("TTT-> Superclass is " + superclass);

            if (superclass != null) {
                if (superclass.equals("java.lang.Thread"))
                    return true;
            }
        } while (!superclass.equals("java.lang.Object"));

        if ((checkRunnable) && (!isThread)) { //test interfaces too)
            CtClass[] interfaces = null;

            try {
                interfaces = clazz.getInterfaces();
            } catch (NotFoundException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            if (interfaces != null) {
                for (int in = 0; in < interfaces.length; ++in) {
                    if (TransformClasses.debugInstrumentation)
                        logger.debug("TTT-> Checking interface " + interfaces[in].getName());
                    if (interfaces[in].getName().equals("java.lang.Runnable"))
                        isThread = true;
                }
            }
        }
        return isThread;
    }

    private static boolean isThread(Class targetClass) {
        String superclass = null;
        Class thisclass = targetClass;

        do {
            thisclass = thisclass.getSuperclass();
            if (thisclass != null)
                superclass = thisclass.getName();

            // test to see whether the class is either a Thread or is Runnable

            if (TransformClasses.debugInstrumentation)
                logger.debug("TTT-> Superclass is " + superclass);

            if (superclass != null) {
                if (superclass.equals("java.lang.Thread"))
                    return true;
            }
        } while ((superclass != null) && (!superclass.equals("java.lang.Object")));
        //test interfaces too)
        Class[] interfaces = targetClass.getInterfaces();

        if (interfaces != null) {
            for (int in = 0; in < interfaces.length; ++in) {
                if (TransformClasses.debugInstrumentation)
                    logger.debug("TTT-> Checking interface " + interfaces[in].getName());
                if (interfaces[in].getName().equals("java.lang.Runnable")) {

                    return true;
                }
            }
        }
        return false;
    }

    private static class TempClassLoader extends ClassLoader {

        public TempClassLoader(ClassLoader classLoader) {
            super(classLoader);
        }

        public TempClassLoader() {
            super(ClassLoader.getSystemClassLoader());
        }

        protected Class findClass(String name) throws ClassNotFoundException {
            Class ret = null;
            for (int i = 0; i < paths.size(); i++) {
                String path = paths.get(i);
                if (path.endsWith(".jar")) {
                    ret = findJarEntry(path, name);
                    if (ret != null) {
                        return ret;
                    }
                } else {
                    ret = findClassFile(path, name);
                    if (ret != null) {
                        return ret;
                    }
                }
            }
            return ret;
        }

        private Class findClassFile(String root, String classname) {
            if (!root.endsWith(File.pathSeparator)) {
                root += File.separator;
            }
            String filename = root + classname.replace('.', File.separatorChar) + ".class";
            File file = new File(filename);
            try {
                if (file.exists()) {
                    FileInputStream reader = new FileInputStream(file);
                    byte[] classdata = new byte[(int) file.length()];
                    reader.read(classdata);
                    reader.close();
                    return defineClass(classname, classdata, 0, classdata.length);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        private Class findJarEntry(String jar, String classname) {
            try {
                JarInputStream jis = new JarInputStream(new FileInputStream(jar));
                String zipform = classname.replace('.', '/') + ".class";
                ZipEntry ze;
                while ((ze = jis.getNextEntry()) != null) {
                    String name = ze.getName();
                    if (zipform.equals(name)) {
                        ByteArrayOutputStream bout = new ByteArrayOutputStream();
                        byte[] bytes = new byte[1024];
                        int c;
                        while ((c = jis.read(bytes)) != -1) {
                            bout.write(bytes, 0, c);
                        }
                        bout.flush();
                        bout.close();
                        bytes = bout.toByteArray();
                        bytes = BytecodeRewrite.rewrite(classname, bytes);
                        return defineClass(classname, bytes, 0, bytes.length);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

    }

}

// }
/*   }  else if (m.getMethodName().equals("park")) {
String target = m.getClassName();
logger.trace("Instrument Agent --> class park invoked on--" + target + "--");

if (target.equals("java.util.concurrent.locks.LockSupport")) {
//m.replace("javm.util.concurrent.locks.LockSupport.park();");
m.replace(invokeVoidNoArgs("javm.util.concurrent.locks.LockSupport", "park"));
logger.trace("Instrument Agent --> park() Rewritten");
}
} else if (m.getMethodName().equals("unpark")) {
String target = m.getClassName();
logger.trace("Instrument Agent --> class unpark invoked on--" + target + "--");
if (target.equals("java.util.concurrent.locks.LockSupport")) {
//m.replace("javm.util.concurrent.locks.LockSupport.unpark($1);");
m.replace(invokeVoid("javm.util.concurrent.locks.LockSupport", "unpark", new String[]{"java.lang.Thread.class"}, new String[]{"$1"}));
logger.trace("Instrument Agent --> unpark() Rewritten");
}          */


/**class NewClass {
 byte[] bytes;
 CtClass classObject;

 NewClass(byte[] bytes, CtClass classObject) {
 this.bytes = bytes;
 this.classObject = classObject;
 }

 public byte[] getBytes() {
 return bytes;
 }

 public CtClass getClassObject() {
 return classObject;
 }
 }    **/

