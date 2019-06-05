package com.sshtools.common.logger;

import com.sshtools.common.logger.Log.Level;

public class ConsoleLoggingContext extends AbstractLoggingContext {

	public ConsoleLoggingContext(Level level) {
		super(level);
	}
	
	public ConsoleLoggingContext() {
		
	}
	
	@Override
	public void log(Level level, String msg, Throwable e, Object... args) {
		if(isLogging(level)) {
			System.out.print(DefaultLoggerContext.prepareLog(level, msg, e, args));
		}
	}

	@Override
	public void raw(Level level, String msg) {
		if(isLogging(level)) {
			System.out.print(DefaultLoggerContext.prepareLog(level, "", null));
			System.out.println(msg);
		}
	}		

	@Override
	public void close() {
		/**
		 * Does nothing because we don't want to close the console. 
		 */
	}

	@Override
	public void newline() {
		System.out.println();
	}


}
