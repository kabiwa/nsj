package agentj.examples.udprouting.repository;

import java.net.InetAddress;

import agentj.examples.udprouting.Constants;


/**
 * A single entry in the ReceivedSet
 * @author Ulrich Herberg
 *
 */
public class ReceivedTuple {
	static final long serialVersionUID = 1L;

	protected InetAddress _origAddress;
	protected int _sequenceNumber;
	protected long _time;


	public ReceivedTuple(InetAddress origAddress,
			int sequenceNumber) {
		_sequenceNumber = sequenceNumber;
		_origAddress = origAddress;
		_time = System.currentTimeMillis() + Constants.R_HOLD_TIME;
	}

	public InetAddress getOrigAddress() {
		return _origAddress;
	}

	public int getSequenceNumber() {
		return _sequenceNumber;
	}

	public long getExpireTime(){
		return _time;
	}
	
	/**
	 * @param origAddress
	 *            the origAddress to set
	 */
	public void setOrigAddress(InetAddress origAddress) {
		_origAddress = origAddress;
	}

	/**
	 * @param sequenceNumber
	 *            the sequenceNumber to set
	 */
	public void setSequenceNumber(int sequenceNumber) {
		_sequenceNumber = sequenceNumber;
	}

	

	public String toString() {
		StringBuffer txt = new StringBuffer();
		txt.append("  * Tuple: ");
		txt.append("originatorAddress: ");
		txt.append(_origAddress);
		txt.append("\n    sequenceNumber: ");
		txt.append(_sequenceNumber);
		txt.append("\n    time: ");
		txt.append(_time);
		txt.append("\n");

		return txt.toString();
	}

}
