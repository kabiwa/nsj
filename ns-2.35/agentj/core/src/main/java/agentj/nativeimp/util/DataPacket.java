package agentj.nativeimp.util;

/**
 * This class is a simple container messages going back and forth between
 * the native implementations. It also server for synchroniation for the
 * incoming TCP packets.
 *
 * The fields and getters are the same as DatagramPacket so we don;t have
 * to change much (except addressing of course).
 * 
 * <p/>
 *
 * Created by Ian Taylor
 * Modified: 3/22/09
 */
public class DataPacket {

    private byte[] data;
    private int offset;
    private int length;
    private long address;
    private int port;

    public DataPacket() {
    }

    /*
    * When used for addressing
    */
    public DataPacket(long address, int port) {
        this.address = address;
        this.port = port;
    }

    /*
     * When used for data
     */
    public DataPacket(byte[] dataBuffer, int off, int len) {
        this.data= dataBuffer;
        this.offset=off;
        this.length=len;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public long getAddress() {
        return address;
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public void setAddress(long address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
