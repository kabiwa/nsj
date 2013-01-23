package agentj.examples.udprouting;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a message (both HELLO and TC/LSA messages)
 * @author Ulrich Herberg
 *
 */
public final class Message {
	public final static byte HELLO = 0;
	public final static byte TC = 1;
	
	
	private short _seqno = 0;
	private final byte _msgType;
	private InetAddress _origAddress;
	private List<InetAddress> _addresses= null;
	private byte[] _bytes = null;
	

	/**
	 * Create a new message with the given type (either HELLO or TC)
	 * @param msgType should be {@link #HELLO} or {@link #HELLO}
	 */
	public Message(byte msgType) {
		_msgType = msgType;
		if (msgType == TC){
			_addresses = new ArrayList<InetAddress>();
		}
	}
	
	/**
	 * Parse incoming message
	 * @param bytes
	 * @param length
	 * @throws Exception
	 */
	public Message(byte[] bytes, int length) throws Exception{
		ByteBuffer buf = ByteBuffer.wrap(bytes, 0, length);
		buf.limit(length);
		_msgType = buf.get();
		_seqno = buf.getShort();
		if (_msgType == TC){
			_addresses = new ArrayList<InetAddress>();
			byte[] origAddressBytes = new byte[4];
			buf.get(origAddressBytes, 0, 4);
			_origAddress = InetAddress.getByAddress(origAddressBytes);
			
			
			while (buf.hasRemaining()){
				byte[] addressBytes = new byte[4];
				buf.get(addressBytes, 0, 4);
				_addresses.add(InetAddress.getByAddress(addressBytes));
			}
			
			// store the byte array for TCs for forwarding. But only copy the necessary part
			_bytes = new byte[length];
			for (int i = 0; i < length; i++)
				_bytes[i] = bytes[i];
		}
	}
	
	/**
	 * Add neighbor address (only for TCs)
	 * @param address
	 */
	public void addAddress(InetAddress address) {
		if (_msgType != TC)
			throw new IllegalStateException("this message is no TC message!");
		
		if (_addresses == null)
			throw new IllegalAccessError("_addresses is null");
		
		_addresses.add(address);
	}
	
	/**
	 * Get addresses (if message is a TC)
	 * @return messages (may be {@code null})
	 */
	public List<InetAddress> getAddresses() {
		return _addresses;
	}
	
	/**
	 * Returns the byte array that can be sent to the socket
	 * @return byte array
	 */
	public byte[] getBytes(){
		if (_bytes != null)
			return _bytes; // for forwarded TCs, no need to recreate the byte array
			
		int size = 3; // seqno and msgType
		if (_msgType == TC){
			if (_addresses == null)
				throw new IllegalAccessError("_addresses is null");
			
			size += 4 * (_addresses.size() + 1); // originator address and addresses
			
			if (_origAddress == null)
				throw new IllegalAccessError("_origAddress is null");
		}
			
		byte[] bytes = new byte[size];
		ByteBuffer buf = ByteBuffer.wrap(bytes);
		buf.put(_msgType);
		buf.putShort(_seqno);
		
		if (_msgType == TC){
			buf.put(_origAddress.getAddress());
			for (InetAddress address : _addresses)
				buf.put(address.getAddress());
		}
		
		return bytes;
	}
	
	/**
	 * Returns the message type (either {@link #HELLO} or {@link #TC})
	 * @return message type
	 */
	public byte getMsgType() {
		return _msgType;
	}
	
	/**
	 * Returns the message originator
	 * @return originator
	 */
	public InetAddress getOrigAddress() {
		return _origAddress;
	}
	
	/**
	 * Returns the sequence number
	 * @return seqno
	 */
	public short getSeqno() {
		return _seqno;
	}
	
	public void setOrigAddress(InetAddress origAddress) {
		_origAddress = origAddress;
	}
	
	public void setSeqno(short seqno) {
		_seqno = seqno;
	}
	
	public String toString(){
		StringBuffer txt = new StringBuffer();
		txt.append((_msgType == HELLO) ? "HELLO " : "TC ");
		txt.append(_seqno);
		txt.append(" orig: ");
		txt.append(_origAddress.getHostAddress());
		
		return txt.toString();
	}
}
