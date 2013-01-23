/*
 * Copyright 2004 - 2007 University of Cardiff.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package agentj.instrument;

import proto.logging.api.Log;
import proto.logging.api.Logger;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;


/**
 * Class Description Here...
 *
 * @author Andrew Harrison
 * @version $Revision: 1.17 $
 * @created Oct 10, 2008: 2:06:01 PM
 * @date $Date: 2009-02-20 17:21:28 $ modified by $Author: harrison $
 * @todo Put your notes here...
 */

public class TransformClassLoader extends URLClassLoader {

    static Logger log = Log.getLogger();
    boolean debug = false; // Ian fudge to slect debug or not ...

    private static TransformClassLoader instance;

    public TransformClassLoader(ClassLoader classLoader) {
        super(new URL[0], classLoader);
        //JavassistRewrite.init();
        //if (debug) log.setLogLevel(Logger.LogLevel.TRACE);
        log.trace("created with parent " + getParent().getClass().getName());
        for (String path : JavassistRewrite.paths) {
            File f = new File(path);
            if (f.exists()) {
                log.trace("parsing " + f.getAbsoluteFile());
                try {
                    String s = f.toURI().toURL().toString();
                    if (f.isDirectory() && !s.endsWith("/")) {
                        s += "/";
                    }
                    URL u = new URL(s);
                    addURL(u);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    public TransformClassLoader() {
        this(null);
    }

    public static synchronized ClassLoader getLoader() {
        if (instance == null) {
            instance = new TransformClassLoader(ClassLoader.getSystemClassLoader());
        }
        return instance;
    }


    public static Class getClass(String name) throws ClassNotFoundException {
        return getLoader().loadClass(name);
    }


    protected Class loadClassProtected(String name) {
        Class cls = findLoadedClass(name);
        if (cls != null) {
            return cls;
        }
        try {
            Class ret = findClass(name);
            return ret;
        } catch (ClassNotFoundException e) {
        }
        return null;
    }


    protected Class findClass(String name) throws ClassNotFoundException {
        if (debug) log.trace("class name " + name);

        byte[] bytes = JavassistRewrite.rewrite(name);

        if (bytes != null) {
            /*try {
                definePackage(JavassistRewrite.getPackage(name), null, null, null, null, null, null, null);
            } catch (Exception e) {

            }*/
            return defineClass(name, bytes, 0, bytes.length);
        }
        throw new ClassNotFoundException("could not find class " + name);
    }

}
