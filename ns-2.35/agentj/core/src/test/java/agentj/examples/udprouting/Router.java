package agentj.examples.udprouting;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import agentj.examples.udprouting.comm.Receiver;
import agentj.examples.udprouting.comm.Sender;
import agentj.examples.udprouting.repository.LinkTuple;
import agentj.examples.udprouting.repository.ReceivedTuple;
import agentj.examples.udprouting.repository.RoutingSet;
import agentj.examples.udprouting.repository.RoutingTuple;
import agentj.examples.udprouting.repository.TopologyTuple;

/**
 * This is the main class of the routing protocol, including a "main" method.
 * @author Ulrich Herberg
 *
 */
public final class Router {
	
	/**
	 * This thread periodically creates a Hello message and sends it to the socket.
	 */
	private class HelloTask extends Thread {
		@Override
		public void run() {
			try {
				Thread.sleep(_random.nextInt(Constants.MAX_JITTER)); // wait for a random "jitter" time to reduce collisions of packets
			} catch (InterruptedException e1) {
				e1.printStackTrace(); // should never happen in AgentJ
			}
			
			
			while (true){
				Message message = createHelloMessage();
				StringBuffer txt = new StringBuffer("Sending ");
				txt.append(message.toString());
				_logger.info(txt.toString());
				byte[] bytes = message.getBytes();
				_sender.send(bytes);
				try {
					Thread.sleep(Constants.HELLO_INTERVAL - _random.nextInt(Constants.MAX_JITTER)); // wait HELLO_INTERVAL, reduced by jitter.
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * This thread periodically creates a TC message and sends it to the socket.
	 */
	private class TcTask extends Thread{
		@Override
		public void run() {
			try {
				Thread.sleep(500 + _random.nextInt(500)); // wait a little to have some HELLOs exchanged first
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			
			
			while (true){
				Message message = createTcMessage();
				StringBuffer txt = new StringBuffer("Sending ");
				txt.append(message.toString());
				_logger.info(txt.toString());
				byte[] bytes = message.getBytes();
				//_logger.fine(Tools.toHexString(bytes));
				_sender.send(bytes);
				
				try {
					Thread.sleep(Constants.TC_INTERVAL - _random.nextInt(Constants.MAX_JITTER));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Start the link state routing protocol (not used in AgentJ)
	 * @param args nothing needed
	 */
	public static void main(String[] args){
		Router router = new Router();
		router.initialize();
	}
	
	private final Sender _sender = new Sender();
	private final Receiver _receiver = new Receiver(this);
	private final List<LinkTuple> _linkSet = Collections.synchronizedList(new ArrayList<LinkTuple>());
	private final List<TopologyTuple> _topologySet = Collections.synchronizedList(new ArrayList<TopologyTuple>());
	private final RoutingSet _routingSet = new RoutingSet();
	private final Timer _timer = new Timer();
	private final Random _random = new Random();
	private short _seqno = (short) _random.nextInt(30000);
	private InetAddress _origAddress;
	private HelloTask _helloTask = null;
	private TcTask _tcTask = null;
	private static Logger _logger =
		 Logger.getLogger(Router.class.getName());

	
	public Router(){		
	}
	
	/**
	 * Calculate shortest paths to all destinations in the topology set
	 */
	private void calculateDijkstra(){
		_routingSet.clear();
		for (LinkTuple linkTuple : _linkSet){
			RoutingTuple newTuple = new RoutingTuple(linkTuple.getAddress(), linkTuple.getAddress(), 1);
			_routingSet.add(newTuple);
		}
		
		int h = 1;
			
		while (true){
			boolean tupleAdded = false;
			for (TopologyTuple topologyTuple : _topologySet){
				if (_routingSet.containsDestination(topologyTuple.getTo()) || topologyTuple.getTo().equals(_origAddress))
					continue;
				
				RoutingTuple previous = _routingSet.getDestination(topologyTuple.getFrom(), h);
				if (previous != null){
					RoutingTuple newTuple = new RoutingTuple(topologyTuple.getTo(), previous.getNexthop(), h+1);
					_routingSet.add(newTuple);
					tupleAdded = true;
				}
			}
			h++;
			if (tupleAdded == false)
				break;
		}
	}
	
	private Message createHelloMessage(){
		Message msg = new Message(Message.HELLO);
		msg.setSeqno(_seqno++);
		msg.setOrigAddress(_origAddress);
		return msg;
	}

	private Message createTcMessage(){
		Message msg = new Message(Message.TC);
		msg.setSeqno(_seqno++);
		msg.setOrigAddress(_origAddress);
		// add all neighbors in the TC
		for (LinkTuple tuple : _linkSet){
			msg.addAddress(tuple.getAddress());
		}
		
		return msg;
	}
	
	/**
	 * Return the originator address of this router
	 * @return originator address
	 */
	public InetAddress getOrigAddress() {
		return _origAddress;
	}
	
	/**
	 * Returns the routing set of this router
	 * @return routing set
	 */
	public RoutingSet getRoutingSet() {
		return _routingSet;
	}
	
	/**
	 * This method must be called after the router object has been created
	 */
	public void initialize(){
		_receiver.start(); // start receiving messages from a socket
		
		try {
			_origAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e2) {
			e2.printStackTrace();
			System.exit(1);
		}
		
		// this task will periodically clean all expired tuples
		TimerTask expireTask = new TimerTask(){
			@Override
			public void run() {
				long now = System.currentTimeMillis();
				boolean changed = false;
				for (Iterator<LinkTuple> iter = _linkSet.iterator(); iter.hasNext(); ){
					LinkTuple tuple = iter.next();
					if (tuple.getExpireTime() <= now){
						iter.remove();
						changed = true;
					}
				}
				
				for (Iterator<TopologyTuple> iter = _topologySet.iterator(); iter.hasNext(); ){
					TopologyTuple tuple = iter.next();
					if (tuple.getExpireTime() <= now){
						iter.remove();
						changed = true;
					}
				}
				
				for (Iterator<ReceivedTuple> iter = _receiver.getReceivedSet().iterator(); iter.hasNext(); ){
					ReceivedTuple tuple = iter.next();
					if (tuple.getExpireTime() <= now)
						iter.remove();
				}
				
				if (changed)
					calculateDijkstra();
			}
		};
		_timer.schedule(expireTask, 50, 50);
		
		
		_helloTask = new HelloTask(); // this thread will periodically send HELLOs
		_tcTask = new TcTask(); // same for TCs
		
		_helloTask.start(); // start the threads
		_tcTask.start();	
	}
	
	/**
	 * Process an incoming HELLO message
	 * @param msg Message to be processed (should not be {@code null})
	 */
	public void processHello(Message msg){
		long now = System.currentTimeMillis();
		LinkTuple existingTuple = null;
		// try to find whether a link tuple has already been added for that neighbor
		for (LinkTuple tuple : _linkSet){
			if (tuple.getAddress().equals(msg.getOrigAddress())){
				existingTuple = tuple;
				break;
			}
		}
		
		// if not, add a tuple
		if (existingTuple == null){
			existingTuple = new LinkTuple(msg.getOrigAddress());
			_linkSet.add(existingTuple);
			calculateDijkstra();
		} 
		
		// Update expiration time of the tuple
		existingTuple.setExpireTime(now + Constants.HELLO_HOLD_TIME);
	}
	
	
	/**
	 * Process an incoming TC message
	 * @param msg Message to be processed 
	 */
	public void processTc(final Message msg){
		long now = System.currentTimeMillis();
		boolean changed = false;
		for (InetAddress address : msg.getAddresses()){
			TopologyTuple existingTuple = null;
			for (TopologyTuple tuple : _topologySet){
				if (tuple.getFrom().equals(msg.getOrigAddress()) && 
						tuple.getTo().equals(address)){
					existingTuple = tuple;
					break;
				}
			}
			
			if (existingTuple == null){
				existingTuple = new TopologyTuple(msg.getOrigAddress(), address);
				_topologySet.add(existingTuple);
				changed = true;
			}	
			// Update expiration time of the tuple
			existingTuple.setExpireTime(now + Constants.TC_HOLD_TIME);
		}
		
		if (changed)
			calculateDijkstra();
		

		_sender.send(_random.nextInt(Constants.MAX_JITTER), msg.getBytes()); // forward message	
	}
}
