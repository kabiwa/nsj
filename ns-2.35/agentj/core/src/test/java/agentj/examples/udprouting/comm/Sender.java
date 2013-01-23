package agentj.examples.udprouting.comm;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import agentj.examples.udprouting.Constants;
import agentj.examples.udprouting.Message;

/**
 * This class sends messages, possibly with a delay
 * @author Ulrich Herberg
 *
 */
public final class Sender {
	private MulticastSocket _socket;
	private InetAddress _broadcast;
	private Timer _timer = new Timer();
	private static Logger _logger =
		 Logger.getLogger(Sender.class.getName());
	
	public Sender(){
		try {
			_socket = new MulticastSocket();
			_broadcast = InetAddress.getByName("255.255.255.255");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Send a given byte array immediately
	 * @param buf (must not be {@code null})
	 */
	public void send(byte[] buf){
		DatagramPacket packet = new DatagramPacket(buf, buf.length, _broadcast, Constants.PORT);
		
		
		try {
			_socket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Send data with delay
	 * @param delay in ms
	 * @param buf
	 */
	public void send(long delay, final byte[] buf){
		TimerTask task = new TimerTask(){
			public void run(){
				DatagramPacket packet = new DatagramPacket(buf, buf.length, _broadcast, Constants.PORT);
				
				
				try {
					Message msg = new Message(buf, buf.length);
					StringBuffer txt = new StringBuffer("Forwarding ");
					txt.append(msg.toString());
					_logger.info(txt.toString());
					_socket.send(packet);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		_timer.schedule(task, delay);
	}
}
