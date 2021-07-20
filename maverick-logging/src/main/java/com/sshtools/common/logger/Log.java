/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */

package com.sshtools.common.logger;

import java.util.Objects;

public class Log {

	
	static RootLoggerContext defaultContext = null;
	static ThreadLocal<LoggerContext> currentContext = new ThreadLocal<LoggerContext>();
	
	public static RootLoggerContext getDefaultContext() {
		synchronized(Log.class) {
			if(defaultContext==null) {
				defaultContext = new DefaultLoggerContext();
			}
			return defaultContext;
		}
	}
	
	public void shutdown() {
		defaultContext.shutdown();
	}
	
	public static void setDefaultContext(RootLoggerContext loggerContext) {
		synchronized(Log.class) {
			defaultContext = loggerContext;
		}
	}
	
	public static void enableConsole(Level level) {
		getDefaultContext().enableConsole(level);
	}
	
	public enum Level {
		NONE,
		ERROR,
		WARN,
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
