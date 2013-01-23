package agentj.nativeimp.util;

/**
 * A Simple interface for callbacks from the native implementation that
 * Lets the Java Socket knnow that data is ready for reading.
 *
 * This is implemented in the socket classes - one of the few addtitions to the Sun code
 *
 */
public interface DataCallback {

    /**
     * This method is invoked when data is ready for reading at this socket
     */
    public void dataArrived();

    /**
     * This is invoked if the data is passed from a node to itself thereby bypassing the
     * underlying TCP implementation - data is just passed to the other TCP socket.
     *
     * @param data
     */
 //   public void dataArrivedLocally(byte[] data);

}
