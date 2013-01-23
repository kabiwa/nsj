package agentj.instrument;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.TreeSet;
import proto.logging.api.*;

public class ProfileHashtable extends ConcurrentHashMap<String, ProfileObject>{
	private static ProfileHashtable me = new ProfileHashtable();
	public static ProfileHashtable instance(){
		return me;
	}

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				Logger logger = Log.getLogger();
        			ProfileHashtable table = ProfileHashtable.instance();
        			if (table != null)
                		logger.info(table.toString());
					//System.out.println(table.toString());
			}
		});

	}

	private ProfileHashtable(){
	}

	public String toString(){
		StringBuffer txt = new StringBuffer();
		txt.append("Profiling statistic (time in microseconds): \n");
		Set<ProfileObject> entrySet = new TreeSet<ProfileObject>(me.values());
		int i = 20;
		for (ProfileObject obj : entrySet){
			txt.append(obj.toString());
			txt.append("\n");
			if (i >= 30)
				break;
			i++;
		}
		return txt.toString();
	}


	public static void methodCalled(String longMethodName, long timeInMicroseconds){
		if (longMethodName == null)
			//longMethodName = "null";
			return;
		
		ProfileObject obj = me.get(longMethodName);
		if (obj == null){
			obj = new ProfileObject(longMethodName);
			me.put(longMethodName, obj);
		}

		obj.callCount++;
		obj.timeSpent += timeInMicroseconds;
	}
}

class ProfileObject implements Comparable<ProfileObject> {
	private String methodCall = "";
	public int callCount = 0;
	public double timeSpent = 0;

	public ProfileObject(String method){
		methodCall = method;
	}

	public String toString(){
		StringBuffer buf = new StringBuffer();
		buf.append(methodCall);
		buf.append("\t\t");
		buf.append("callCount:");
		buf.append(callCount);
		buf.append("\ttotalTimeSpent: ");
		buf.append(timeSpent);
		buf.append("\taverageTimeSpent: ");
		buf.append(String.format("%.2f", timeSpent / (double) callCount));
		return buf.toString();
	}
	public int compareTo(ProfileObject other){
		return (int) (other.timeSpent - timeSpent);
	}
}
