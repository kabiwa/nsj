package agentj.examples.udprouting.repository;

import java.net.InetAddress;

public final class TopologyTuple {
	private final InetAddress _from;
	private final InetAddress _to;
	private long _expireTime;

	public TopologyTuple(InetAddress from, InetAddress to) {
		_from = from;
		_to = to;
	}

	public InetAddress getFrom() {
		return _from;
	}

	public InetAddress getTo() {
		return _to;
	}

	public long getExpireTime() {
		return _expireTime;
	}
	
	public void setExpireTime(long expireTime) {
		_expireTime = expireTime;
	}
}
