package agentj.examples.udprouting.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import agentj.AgentJVirtualMachine;


public class VerySimpleFormatter extends Formatter {

	/**
     * Format the given LogRecord.
     * 
     * @param record
     *            the log record to be formatted.
     * @return a formatted log record
     */
    public synchronized String format(LogRecord record) {
            StringBuffer sb = new StringBuffer();
            // Minimize memory allocations here.

            String message = formatMessage(record);
            // sb.append(record.getLevel().getLocalizedName());
            sb.append(javm.lang.System.currentTimeMillis() + " ms");
            sb.append(": ");
            sb.append("Node ");
            sb.append(AgentJVirtualMachine.getCurrentNS2NodeController().getLocalHost());
            sb.append(": ");
            sb.append(message);
            sb.append("\n");
            if (record.getThrown() != null) {
                    try {
                            StringWriter sw = new StringWriter();
                            PrintWriter pw = new PrintWriter(sw);
                            record.getThrown().printStackTrace(pw);
                            pw.close();
                            sb.append(sw.toString());
                    }
                    catch (Exception ex) {
                            java.lang.System.out.println(ex);
                    }
            }
            return sb.toString();
    }
	
	
}
