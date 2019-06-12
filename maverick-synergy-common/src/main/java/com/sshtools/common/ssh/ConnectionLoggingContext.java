/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.common.ssh;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.events.EventListener;
import com.sshtools.common.logger.FileLoggingContext;
import com.sshtools.common.logger.Log;
import com.sshtools.common.logger.Log.Level;
import com.sshtools.common.logger.LoggerContext;
import com.sshtools.common.util.IOUtil;

public class ConnectionLoggingContext implements LoggerContext, EventListener {

	Level defaultLevel;
	Map<SshConnection, FileLoggingContext> activeLoggers = new HashMap<>();
	ConnectionManager<?> cm;
	
	ConnectionLoggingContext(Level level, ConnectionManager<?> cm) {
		this.defaultLevel = level;
		this.cm = cm;
	}
	
	@Override
	public boolean isLogging(Level level) {
		SshConnection currentConnection = cm.getCurrentConnection();
		if(activeLoggers.containsKey(currentConnection)) {
			return this.defaultLevel != Level.NONE && this.defaultLevel.ordinal() >= level.ordinal();
		} 
		return false;
	}

	@Override
	public void log(Level level, String msg, Throwable e, Object... args) {

		SshConnection currentConnection = cm.getCurrentConnection();
		if(!Objects.isNull(currentConnection)) {
			FileLoggingContext ctx = activeLoggers.get(currentConnection);
			if(!Objects.isNull(ctx)) {
				ctx.log(level, msg, e, args);
			}
		}
	}

	private boolean isLoggingRemoteAddress(SshConnection con) {
		return lookup(".remoteAddr", con.getRemoteAddress().getHostAddress(), con);
	}
	
	private boolean isLoggingLocalAddress(SshConnection con) {
		return lookup(".localAddr", con.getLocalAddress().getHostAddress(), con);
	}
	
	private boolean isLoggingRemotePort(SshConnection con) {
		return lookup(".remotePort", String.valueOf(con.getRemotePort()), con);
	}
	
	private boolean isLoggingLocalPort(SshConnection con) {
		return lookup(".localPort", String.valueOf(con.getLocalPort()), con);
	}
	
	private boolean lookup(String key, String value, SshConnection con) {
		String v = getProperty(key, "");
		if("".equals(v)) {
			return true;
		}
		Set<String> addr = new HashSet<String>(Arrays.asList(v.split(",")));
		return addr.isEmpty() || addr.contains(value);
	}

	
	public void open(Connection<?> con) throws IOException {
		con.addEventListener(this);
		if(isLoggingConnection(con)) {
			createLog(con);
		}
	}
	
	private void createLog(Connection<?> con) throws IOException {
		
		Level level = Level.valueOf(getProperty(".defaultLevel", defaultLevel.name()));
		String filenameFormat = getProperty(".filenameFormat", "${timestamp}__${uuid}.log");
		Integer maxFiles = Integer.parseInt(getProperty(".maxFiles", "10"));
		Long maxSize = IOUtil.fromByteSize(getProperty(".maxSize", "20MB"));
		String defaultTimestamp = getProperty(".timestampFormat", "yyyy-MM-dd-HH-mm-ss-SSS");
		
		String filename = filenameFormat
				.replace("${timestamp}", LocalDateTime.now().format(
						DateTimeFormatter.ofPattern(
								getProperty(getPropertyKey(".timestampPattern"), 
										defaultTimestamp))))
				.replace("${uuid}", con.getUUID())
				.replace("${remotePort}", String.valueOf(con.getRemotePort()))
				.replace("${remoteAddr}", con.getRemoteAddress().getHostAddress())
				.replace("${localPort}", String.valueOf(con.getLocalPort()))
				.replace("${localAddr}", con.getLocalAddress().getHostAddress());
		
		activeLoggers.put(con, new FileLoggingContext(level, new File(filename), maxFiles, maxSize));
	}

	private boolean isLoggingConnection(Connection<?> con) {
		
		Properties loggingProperties = Log.getDefaultContext().getLoggingProperties();
		
		/**
		 * Get maverick.log.connection.<name> property to determine if logging is enabled.
		 * Default to the default log level
		 */
		if(!"true".equalsIgnoreCase(loggingProperties.getProperty(getPropertyKey(""), 
				String.valueOf(!this.defaultLevel.equals(Level.NONE))))) {
			return false;
		}
		
		return (isLoggingRemoteAddress(con) && isLoggingRemotePort(con)
				&& isLoggingLocalAddress(con) && isLoggingLocalPort(con))
				|| isLoggingIdentifier(con) || isLoggingUser(con);
	}

	private boolean isLoggingUser(Connection<?> con) {
		if(!Objects.isNull(con.getUsername())) {		
			String v = getProperty(".user", "");
			if("".equals(v)) {
				return false;
			}
			Set<String> users = new HashSet<String>(Arrays.asList(v.split(",")));
			return users.contains(con.getUsername());
		}
		return false;
	}

	private boolean isLoggingIdentifier(Connection<?> con) {
		if(con.getRemoteIdentification().length() > 0) {
			String v = getProperty(".ident", "");
			if("".equals(v)) {
				return false;
			}
			Set<String> identifications = new HashSet<String>(Arrays.asList(v.split(",")));
			return identifications.contains(con.getRemoteIdentification());
		}
		return false;
		
	}

	private String getProperty(String key, String defaultValue) {
		Properties loggingProperties = Log.getDefaultContext().getLoggingProperties();
		defaultValue = loggingProperties.getProperty(String.format("maverick.log.connection%s", key), defaultValue);
		return loggingProperties.getProperty(getProperty(key, defaultValue));
	}
	
	private String getPropertyKey(String key) {
		return String.format("maverick.log.connection.%s%s", cm.getName(), key);
	}
	
	public void close(Connection<?> con) {
		FileLoggingContext ctx = activeLoggers.remove(con);
		if(!Objects.isNull(ctx)) {
			ctx.close();
		}
	}

	@Override
	public void raw(Level level, String msg) {
		SshConnection currentConnection = cm.getCurrentConnection();
		if(!Objects.isNull(currentConnection)) {
			FileLoggingContext ctx = activeLoggers.get(currentConnection);
			if(!Objects.isNull(ctx)) {
				ctx.raw(level, msg);
			}
		}
	}

	@Override
	public void close() {
		/**
		 * We don't need to close anything because logs are connection specific. If this
		 * is the result of a change in the log file, the new settings will be applied
		 * to new connections after the changes are applied in the global context.
		 */
	}

	@Override
	public void newline() {
		SshConnection currentConnection = cm.getCurrentConnection();
		if(!Objects.isNull(currentConnection)) {
			FileLoggingContext ctx = activeLoggers.get(currentConnection);
			if(!Objects.isNull(ctx)) {
				ctx.newline();
			}
		}
	}

	@Override
	public void processEvent(Event evt) {
		
		Connection<?> con = (Connection<?>) evt.getAttribute(EventCodes.ATTRIBUTE_CONNECTION);
		switch(evt.getId()) {
		case EventCodes.EVENT_NEGOTIATED_PROTOCOL:
		case EventCodes.EVENT_USERAUTH_STARTED:
			if(!activeLoggers.containsKey(con)) {
				if(isLoggingConnection(con)) {
					try {
						createLog(con);
					} catch (IOException e) {
					}
				}
			}
			break;
		default:
			break;
		}
		
	}
}
