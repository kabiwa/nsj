/*
 * Copyright 2004 - 2009 University of Cardiff.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Class Description Here...
 *
 * @author Andrew Harrison
 * @version $Revision:$
 * @created Mar 9, 2009: 8:48:50 PM
 * @date $Date:$ modified by $Author:$
 */

public class AgentJClassLoader extends URLClassLoader {

    private TransformClassLoader child = null;

    public AgentJClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
        init();
    }

    public TransformClassLoader getChild() {
        return child;
    }

    public void setChild(TransformClassLoader child) {
        this.child = child;
    }

    private void init() {
        String agentJHome = System.getenv("AGENTJ");
        if (agentJHome == null) {
            System.err.println("AGENTJ home variable is NOT Defined - set you environment correctly");
            System.exit(1);
        }
        System.setProperty("proto.logging.properties", agentJHome + File.separator + "config" + File.separator + "proto.logging.properties");

        String nativeDebugFile = agentJHome + File.separator + "config" + File.separator + "native.logging.properties";

        System.setProperty("native.logging.properties", nativeDebugFile);
        String agentJprops = agentJHome + File.separator + "config" + File.separator + "agentj.properties";
        FileInputStream fn;
        try {
            fn = new FileInputStream(agentJprops);
            Properties properties = new Properties();
            properties.load(fn);
            Enumeration keys = properties.keys();
            String key, val;
            for (Enumeration e = properties.elements(); e.hasMoreElements();) {
                val = (String) e.nextElement();
                key = (String) keys.nextElement();
                System.setProperty(key, val);
            }

        } catch (FileNotFoundException e) {

        } catch (IOException e) {
        }

        String classpath = System.getProperty("java.class.path");
        String[] paths = classpath.split(System.getProperty("path.separator"));
        for (String path : paths) {
            File f = new File(path);
            if (f.exists()) {
                try {
                    String s = f.toURI().toURL().toString();
                    if (f.isDirectory() && !s.endsWith("/")) {
                        s += "/";
                    }
                    URL u = new URL(s);
                    addURL(u);
                } catch (MalformedURLException e) {
                }
            }

        }
    }

    public Class loadClass(String name) throws ClassNotFoundException {
       // System.out.println("AgentJClassLoader: Loading " + name);

        try {
            Class ret = super.loadClass(name);
            return ret;
        } catch (ClassNotFoundException e) {
            if (child != null) {
                Class ret = child.loadClassProtected(name);
                if (ret != null) {
                    return ret;
                }
            }
        }
        throw new ClassNotFoundException(name);
    }

}
