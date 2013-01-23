package agentj.examples.udprouting;

public final class Constants {
	private Constants(){}
	
	public static final int PORT = 50000;
	
	public static final int HELLO_INTERVAL = 2000;
	public static final int HELLO_HOLD_TIME = 3 * HELLO_INTERVAL;
	
	public static final int TC_INTERVAL = 5000;
	public static final int TC_HOLD_TIME = 3 * TC_INTERVAL;
	
	public static final int R_HOLD_TIME = 5000;
	
	public static final int MAX_JITTER = 500;
}
