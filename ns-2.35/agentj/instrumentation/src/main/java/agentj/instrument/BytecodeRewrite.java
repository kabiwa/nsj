package agentj.instrument;

import proto.logging.api.Logger;
import proto.logging.api.Log;

/**
 *
 * This class applies direct bytecode rewriting on the class files.  Used for swapping
 * individual classes in the bytecode directly i.e. for the java.net and java.io classes.
 *
 * User: Ian Taylor
 * Date: May 24, 2006
 * Time: 9:56:12 AM
 */
public class BytecodeRewrite {
    static Logger logger = Log.getLogger();

    /**
     * Rewrite the bytes in the classfile to change packages to the new implementations of
     * those packages.
     *
     * @param name name of class
     * @param bytes bytes of current classfile
     * @return bytecode rewritten version of classfile
     *
     */
     static byte[] rewrite(String name, byte[] bytes) {


        // replace java.net with javm.net

        byte[] newBytes = rewriteBytes(bytes, ClassList.JAVA_NET_PACKAGE,
                ClassList.NEW_NET_PACKAGE, ClassList.REPLACE_NET_CLASSES);

        // replace java.io with javm.io
        // NOTE: ANDREW I DID THIS. MY VERSION BLOWS UP HERE
        // NOTE THE ERROR IS TO DO WITH THE IO REPLACEMENTS, NOT THE FACT THAT
        // rewriteBytes() IS BEING CALLED TWICE

        newBytes = rewriteBytes(newBytes, ClassList.JAVA_NIO_PACKAGE,
                ClassList.NEW_NIO_PACKAGE, ClassList.REPLACE_NIO_CLASSES);

        newBytes = rewriteBytes(newBytes, ClassList.JAVA_UTIL_PACKAGE,
                ClassList.NEW_UTIL_PACKAGE, ClassList.REPLACE_UTIL_CLASSES);

        return newBytes;
    }


    /**
     * Replaces all supplied packages with the new packages that implement
     * the new version of these classes
     *
     * @param clsbytes bytes from the classfile
     * @param javaPackage the Java package that contains the current
     * implementation
     * @param newPackage the new Java package that contains the runtime
     * implementation we want to swap to
     * @param replaceClasses the classes within this packages we want to
     * bytecode rewrite so they run the new version of the class
     * @return the resulting swapped bytecode
     */
    private static byte[] rewriteBytes(byte[] clsbytes, String javaPackage, String newPackage, String[] replaceClasses) {
        byte[] packbytes = javaPackage.getBytes();

        for (int ptr = 0; ptr < clsbytes.length - packbytes.length; ptr++) {
            if ((clsbytes[ptr] == packbytes[0]) && isMatch(replaceClasses, clsbytes, packbytes, ptr))
                clsbytes = replacePack(newPackage, clsbytes, ptr);
        }
        return clsbytes;
    }

     private static boolean isMatch(String[] replaceClasses, byte[] clsbytes, byte[] packbytes, int ptr) {
        boolean packmatch = true;

        for (int packit = 1; (packit < packbytes.length) && (packmatch); packit++)
            packmatch = packmatch && (clsbytes[ptr + packit] == packbytes[packit]);

        if (!packmatch)
            return false;

        int offset = ptr + packbytes.length;
        boolean classmatch = false;
        String replace;

        for (int count = 0; (count < replaceClasses.length) && (!classmatch); count++) {
            replace = replaceClasses[count];

            if ((offset + replace.length() <= clsbytes.length) && (new String(clsbytes, offset, replace.length()).equals(replace)))
                classmatch = true;
        }

        if (!classmatch) return false;

        // This checks for classnames that are supersets of the ones found e.g. Socket and SocketException

        String possibleSubset;
        boolean subset=false;

        for (int count = 0; (count < ClassList.DONT_REPLACE_CLASSES.length) && (!subset); count++) {
            possibleSubset = ClassList.DONT_REPLACE_CLASSES[count];

            if ((offset + possibleSubset.length() <= clsbytes.length) && (new String(clsbytes, offset, possibleSubset.length()).equals(possibleSubset)))
                subset = true;
        }

        if (subset)
            return false;
        else
            return true;
    }


    private static byte[] replacePack(String newPackage, byte[] clsbytes, int ptr) {
        byte[] newbytes = newPackage.getBytes();

        if (TransformClasses.debugInstrumentation)
            logger.trace("Instrument Agent --> replacing package with " + newPackage);

        for (int count = 0; count < newbytes.length; count++)
            clsbytes[ptr + count] = newbytes[count];
        return clsbytes;
    }
}
