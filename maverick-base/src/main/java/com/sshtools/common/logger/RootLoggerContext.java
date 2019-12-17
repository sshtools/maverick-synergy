package com.sshtools.common.logger;

import com.sshtools.common.logger.Log.Level;

public interface RootLoggerContext extends LoggerContext {

	void enableConsole(Level level);

	String getProperty(String key, String defaultValue);

}
