package util;

import java.io.PrintStream;

public class Log {
	enum Level {
		DEBUG,
		INFO,
		WARN,
		FATAL,
		ERROR
	}
	
	private static PrintStream activePrintStream = System.out;
	private static PrintStream errorPrintStream = System.err;
	private static Level logLevel = Level.INFO;
	
	public static void setActivePrintStream(PrintStream printStream) {
		activePrintStream = printStream;
	}
	
	public static void setErrorPrintStream(PrintStream printStream) {
	    errorPrintStream = printStream;
	}
	
	
	public static void setLogLevel(String logLevel) {
		Log.logLevel = Level.valueOf(logLevel);
	}
	
	public static void warn(String message) {
		log(Level.WARN, message);
	}
	
	public static void info(String message) {
		log(Level.INFO, message);
	}
	
	public static void fatal(String message) {
		log(Level.FATAL, message);
	}

	public static void err(String message) {
		log(Level.ERROR, message);
	}
	
	private static void log(Level level, String message) {
		switch (level)
		{
		case ERROR: log(level, message, errorPrintStream);
		break;
		default: log(level, message, activePrintStream);
		break;
		}
	}
	
	/** Logs message prepending location of log invocation. To retrieve the location from which the log was called, 
	 * it navigates through the stack until it gets outside of the Log/Exception classes. */ 
	private static void log(Level level, String message, PrintStream writer) {
		if(level == null || level.compareTo(logLevel) < 0) {
			return;
		}
		StackTraceElement relevantStackTraceElement = getLocation();
		writer.println(level.name()+" (" + relevantStackTraceElement.getClassName() + ";" + relevantStackTraceElement.getMethodName() + ";" + relevantStackTraceElement.getLineNumber() + "): "+ message);
		writer.flush();
	}
	
	private static StackTraceElement getLocation() {
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		StackTraceElement relevantStackTraceElement = null;
		for(StackTraceElement element : stackTrace) {
			// nifty hardcoded way to get the correct stacktrace
			if(!element.getMethodName().equals("getStackTrace") && !element.getClassName().contains("Log") && !element.getClassName().contains("Err")) {
				relevantStackTraceElement = element;
				break;
			}
		}
		return relevantStackTraceElement;
	}
	
	public static PrintStream getPrintStream() {
		return activePrintStream;
	}
}
