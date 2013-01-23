package agentj.examples.udprouting.repository;

import java.net.InetAddress;

/**
 * A link tuple is a single entry in a table listing all neighbors
 * @author Ulrich Herberg
 *
 */
public final class LinkTuple {
	private final InetAddress _address;
	private long _expireTime;

	public LinkTuple(InetAddress address) {
		_address = address;
	}

	public InetAddress getAddress() {
		return _address;
	}

	/**
	 * At this time, the entry should be removed from the table
	 * @return
	 */
	public long getExpireTime() {
		return _expireTime;
	}
	
	public void setExpireTime(long expireTime) {
		_expireTime = expireTime;
	}
	
	public String toString(){
		StringBuffer txt = new StringBuffer();
		txt.append("LinkTuple: ");
		txt.append(_address);
		return txt.toString();
	}
}
