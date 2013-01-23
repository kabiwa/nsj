package agentj.examples.udprouting.comm;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.logging.Logger;

import agentj.examples.udprouting.Constants;
import agentj.examples.udprouting.Message;
import agentj.examples.udprouting.Router;
import agentj.examples.udprouting.repository.ReceivedSet;
import agentj.examples.udprouting.repository.ReceivedTuple;

/**
 * This class is a thread listening for new messages on a socket
 * @author Ulrich Herberg
 *
 */
public final class Receiver extends Thread {
	private MulticastSocket _socket;
	private final ReceivedSet _receivedSet = new ReceivedSet();
	private final Router _router;
	private static Logger _logger =
		 Logger.getLogger(Receiver.class.getName());
	private InetAddress _origAddress = null;
	
	/**
	 * Creates a new receiver for that router
	 * @param router
	 */
	public Receiver(Router router){
		try {
			_socket = new MulticastSocket(Constants.PORT);
			_origAddress = InetAddress.getLocalHost();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		_router = router;
	}
	
	/**
	 * Returns the received set (the set listing messages already received previously)
	 * @return received set
	 */
	public ReceivedSet getReceivedSet(){
		return _receivedSet;
	}
	
	/**
	 * {@link #start()} should be called instead
	 */
	public void run(){
		byte[] buf = new byte[1024];
		while (true){
			try {
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				_socket.receive(packet);
				
				if (packet.getAddress().equals(_router.getOrigAddress()))
					continue;
				
				Message message = new Message(packet.getData(), packet.getLength());
				Short seqno = message.getSeqno();
				if (message.getMsgType() == Message.HELLO)
					message.setOrigAddress(packet.getAddress());
				
				if (!_receivedSet.contains(message.getOrigAddress(), seqno) && !message.getOrigAddress().equals(_origAddress)){
					StringBuffer txt = new StringBuffer("Received ");
					txt.append(message.toString());
					_logger.info(txt.toString());
					_receivedSet.add(new ReceivedTuple(message.getOrigAddress(), seqno));
					
					if (message.getMsgType() == Message.HELLO)
						_router.processHello(message);
					else if (message.getMsgType() == Message.TC)
						_router.processTc(message);
				}
			} catch (Exception e) {
				_logger.warning("Error parsing packet");
				e.printStackTrace();
			} 
		}
	}
}
