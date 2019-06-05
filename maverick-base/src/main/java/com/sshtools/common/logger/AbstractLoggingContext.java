package com.sshtools.common.logger;

import com.sshtools.common.logger.Log.Level;

public abstract class AbstractLoggingContext implements LoggerContext {

	Level level = Level.INFO;
	
	public AbstractLoggingContext() {
		
	}
	
	public AbstractLoggingContext(Level level) {
		this.level = level;
	}

	@Override
	public boolean isLogging(Level level) {
		return this.level.ordinal() >= level.ordinal();
	}

}
