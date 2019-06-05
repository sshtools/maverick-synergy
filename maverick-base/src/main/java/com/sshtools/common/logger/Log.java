package com.sshtools.common.logger;

import java.util.Objects;

public class Log {

	
	static DefaultLoggerContext defaultContext = new DefaultLoggerContext();
	static ThreadLocal<LoggerContext> currentContext = new ThreadLocal<LoggerContext>();
	
	public static DefaultLoggerContext getDefaultContext() {
		return defaultContext;
	}
	
	public enum Level {
		NONE,
		WARN,
		ERROR,
		INFO,
		DEBUG,
		TRACE
	}
	
	public static void setupCurrentContext(LoggerContext context) {
		currentContext.set(context);
	}
	
	public static void clearCurrentContext() {
		currentContext.remove();
	}
	
	public static boolean isWarnEnabled() {
		return isLevelEnabled(Level.WARN);
	}

	public static boolean isErrorEnabled() {
		return isLevelEnabled(Level.ERROR);
	}

	public static boolean isInfoEnabled() {
		return isLevelEnabled(Level.INFO);
	}

	public static boolean isDebugEnabled() {
		return isLevelEnabled(Level.DEBUG);
	}

	public static boolean isTraceEnabled() {
		return isLevelEnabled(Level.TRACE);
	}

	public static boolean isLevelEnabled(Level level) {
		LoggerContext ctx = currentContext.get();
		if((!Objects.isNull(ctx) && ctx.isLogging(level))) {
			return ctx.isLogging(level);
		} else {
			return Log.getDefaultContext().isLogging(level);
		}
	}
	
	public static void info(String msg, Object... args) {
		log(Level.INFO, msg, null, args);
	}
	
	public static void info(String msg, Throwable e, Object... args) {
		log(Level.INFO, msg, e, args);
	}

	public static void debug(String msg, Object... args) {
		log(Level.DEBUG, msg, null, args);
	}
	
	public static void debug(String msg, Throwable e, Object... args) {
		log(Level.DEBUG, msg, e, args);
	}

	public static void trace(String msg, Object... args) {
		log(Level.TRACE, msg, null, args);
	}
	
	public static void trace(String msg, Throwable e, Object... args) {
		log(Level.TRACE, msg, e, args);
	}

	public static void error(String msg, Throwable e, Object... args) {
		log(Level.ERROR, msg, e, args);
	}

	public static void warn(String msg, Throwable e, Object... args) {
		log(Level.WARN, msg, e, args);
	}

	public static void warn(String msg, Object... args) {
		log(Level.WARN, msg, null, args);
	}

	public static void error(String msg, Object... args) {
		log(Level.ERROR, msg, null, args);
	}

	protected static void log(Level level, String msg, Throwable e, Object... args) {
		
		LoggerContext ctx = currentContext.get();
		if(!Objects.isNull(ctx) && ctx.isLogging(level)) {
			contextLog(ctx, level, msg, e, args);
		} else {
			contextLog(Log.getDefaultContext(), level, msg, e, args);
		}
	}
	
	private static void contextLog(LoggerContext ctx, Level level, String msg, Throwable e, Object... args) {
		ctx.log(level, msg, e, args);
	}

	public static void raw(Level level, String msg, boolean newline) {
		LoggerContext ctx = currentContext.get();
		if(!Objects.isNull(ctx) && ctx.isLogging(level)) {
			ctx.raw(level, msg);
			if(newline) {
				ctx.newline();
			}
		} else {
			Log.getDefaultContext().raw(level, msg);
			if(newline) {
				Log.getDefaultContext().newline();
			}
		}
	}
}
