package com.sshtools.common.logger;

import java.io.File;

import com.sshtools.common.logger.Log.Level;

public interface RootLoggerContext extends LoggerContext {

	void enableConsole(Level level);

	String getProperty(String key, String defaultValue);

	void shutdown();

	void enableFile(Level level, String logFile);

	void enableFile(Level level, File logFile);

	void enableFile(Level level, File logFile, int maxFiles, long maxSize);

	void reset();

}
