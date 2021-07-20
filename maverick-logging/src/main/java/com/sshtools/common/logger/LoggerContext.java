
package com.sshtools.common.logger;

import com.sshtools.common.logger.Log.Level;

public interface LoggerContext {

	boolean isLogging(Level level);

	void log(Level level, String msg, Throwable e, Object... args);

	void raw(Level level, String msg);

	void close();

	void newline();

}
