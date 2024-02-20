package com.sshtools.common.logger;

/*-
 * #%L
 * Logging API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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
			System.out.flush();
		}
	}

	@Override
	public void raw(Level level, String msg) {
		if(isLogging(level)) {
			System.out.print(DefaultLoggerContext.prepareLog(level, "", null));
			System.out.println(msg);
			System.out.flush();
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
		System.out.flush();
	}


}
