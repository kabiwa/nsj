package agentj.examples.udprouting.repository;

import java.net.InetAddress;
import java.util.ArrayList;

/**
 * The received set lists messages that have been recently heard and will not be processed again
 * @author Ulrich Herberg
 *
 */
public final class ReceivedSet extends ArrayList<ReceivedTuple> {
	static final long serialVersionUID = 1L;
	
	public ReceivedSet(){
	}

	/**
	 * Returns {@code true} when a message from the given originator address with
	 * the given sequence number has been recently heard.
	 * @param address
	 * @param sequenceNumber
	 * @return {@code true} if the message has been heard
	 */
	public boolean contains(InetAddress address, int sequenceNumber) {
		synchronized (this) {
			for (ReceivedTuple tuple : this) {
				if (tuple.getOrigAddress().equals(address)
						&& tuple.getSequenceNumber() == sequenceNumber)
					return true;
			}
		}
		return false;
	}
	

	public String toString() {
		StringBuffer txt = new StringBuffer();
		txt.append("ReceivedSet: ");
		synchronized (this) {
			for (ReceivedTuple tuple : this) {
				txt.append(tuple.toString());
				txt.append("\n");
			}
		}
		return txt.toString();
	}
}
