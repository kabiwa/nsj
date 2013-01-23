package agentj.examples.udprouting.util;




public abstract class Tools {
	
	/**
	 * Returns a string representing the given byte array 
	 * (e.g. "0A 1C 55 DD")
	 * @param bytes
	 * @return string
	 */
	public static String toHexString(byte[] bytes) {
		if (bytes == null)
			return "(warning: parameter is NULL)";
		
		return toHexString(bytes, bytes.length);
	}
	
	public static String toHexString(byte[] bytes, int length) {
		return toHexString(bytes, 0, length, true);
	}
	
	public static String toHexString(byte[] bytes, int length, boolean lineBreak) {
		return toHexString(bytes, 0, length, lineBreak);
	}
	
	public static String toHexString(byte[] bytes, int offset, int length, boolean lineBreak) {
		if (bytes == null)
			return "";
		
		byte currentByte = 0;
		StringBuffer result = new StringBuffer();

		for (int i = offset + 1; i <= length; i++) {
			currentByte = bytes[i - 1];
			result.append(Integer.toString((currentByte & 0xff) + 0x100, 16).substring(1));
			if (lineBreak && i % 4 == 0) // four per line
				result.append("\n");
			else if (i < bytes.length)
				result.append("  ");
		}
		return result.toString();
	}
	
	
	
	/**
	 * The sequence number S1 is said to be "greater than" the sequence
     * number S2 if:
     *
     * <ul>
     *   <li>S1 > S2 AND S1 - S2 < MAXVALUE/2 OR</li>
     *  
     *   <li>S2 > S1 AND S2 - S1 > MAXVALUE/2</li>
     * </ul>
     * This includes wrap-around
     * 
	 * @param S1
	 * @param S2
	 * @param MAXVALUE designates in the following one more than the
     * largest possible value for a sequence number.
	 * @return true if S1 is greater than S2
	 */
	public static boolean isSequenceNumberGreater(int S1, int S2, int MAXVALUE){
		return (S1 > S2 && S1 - S2 < MAXVALUE/2) ||
		   (S2 > S1 && S2 - S1 > MAXVALUE/2);

	}
	
	/**
	 * Increases sequencenumber using wraparound
	 * @param sequenceNumber
	 * @param MAXVALUE The maximum value of the sequence number, e.g. 65535
	 * @return sequenceNumber++
	 */
	public static int increaseSequenceNumber(int sequenceNumber, int MAXVALUE){		
		return increaseSequenceNumberByValue(sequenceNumber, 1, MAXVALUE);
	}
	
	/**
	 * Increases sequencenumber by the given number using wraparound
	 * @param sequenceNumber
	 * @param increase The number to add
	 * @param MAXVALUE
	 * @return sequenceNumber+increase
	 */
	public static int increaseSequenceNumberByValue(int sequenceNumber, int increase, int MAXVALUE){
		return (sequenceNumber + increase) % (MAXVALUE + 1);
	}
	
	/**
	 * Decreases sequencenumber using wraparound
	 * @param S
	 * @param MAXVALUE
	 * @return S++
	 */
	public static int decreaseSequenceNumber(int S, int MAXVALUE){
		if (S == 0)
			return MAXVALUE;
		
		return S - 1;
	}
}
