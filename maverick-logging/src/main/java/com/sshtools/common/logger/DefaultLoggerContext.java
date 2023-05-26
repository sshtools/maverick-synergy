/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
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
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.common.logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sshtools.common.logger.Log.Level;
import com.sshtools.common.util.IOUtils;

public class DefaultLoggerContext implements RootLoggerContext {

	Collection<LoggerContext> contexts = new ArrayList<>();
	static DateFormat df = new SimpleDateFormat("dd MMM yyyy HH:mm:ss,SSS");
	Properties props; 
	File propertiesFile;
	
	public DefaultLoggerContext() throws IOException {
		propertiesFile = new File(System.getProperty("maverick.log.config", "logging.properties")).getAbsoluteFile();
		loadFile();
		if("true".equalsIgnoreCase(getProperty("maverick.log.nothread", "false"))) {
			return;
		}
		try {
			Path properiesPath = propertiesFile.getAbsoluteFile().toPath();
			FileWatchingService.getInstance().register(properiesPath.getParent(), (path)->{
				if(path.equals(properiesPath))
					loadFile();
			});
		} catch (IOException e) {
			System.err.println("Logging context could not be initialized!");
			e.printStackTrace();
		}
	}
	
	public void shutdown() {
		reset();
	}
	
	public String getProperty(String key, String defaultValue) {
		return processTokenReplacements(props.getProperty(key, defaultValue), System.getProperties());
	}
	
	public String processTokenReplacements(String value, Properties tokenResolver) {
		
		Pattern pattern = Pattern.compile("\\$\\{(.*?)\\}");
		Matcher matcher = pattern.matcher(value);

		StringBuilder builder = new StringBuilder();
		int i = 0;
		while (matcher.find()) {
			String attributeName = matcher.group(1);
			String attributeValue = tokenResolver.getProperty(attributeName);
			if(attributeValue==null) {
				continue;
			}
		    builder.append(value.substring(i, matcher.start()));
		    builder.append(attributeValue);
		    i = matcher.end();
		}
		
	    builder.append(value.substring(i, value.length()));
		
		return builder.toString();
	}

	private synchronized void loadFile() {

		for(LoggerContext ctx : contexts) {
			ctx.close();
		}
		
		contexts.clear();

		if(propertiesFile.exists()) {
			props = new Properties();
			try(InputStream in  = new FileInputStream(propertiesFile)) {
				props.load(in);
			} catch(IOException e) {
				e.printStackTrace();
			}
		} else {
			props = System.getProperties();
		}
		
		if("true".equalsIgnoreCase(getProperty("maverick.log.console", "false"))) {
			enableConsole(Level.valueOf(getProperty("maverick.log.console.level", "INFO")));
		}
		
		if("true".equalsIgnoreCase(getProperty("maverick.log.file", "false"))) {
			enableFile(Level.valueOf(getProperty("maverick.log.file.level", "INFO")),
					new File(getProperty("maverick.log.file.path", "synergy.log")),
					Integer.parseInt(getProperty("maverick.log.file.maxFiles", "10")),
					IOUtils.fromByteSize(getProperty("maverick.log.file.maxSize", "20MB")));
		}
		
		log(Level.INFO, "Reloaded logging configuration {} [{}]", null, propertiesFile.getName(), propertiesFile.getAbsolutePath());
	}
	
	public synchronized void enableConsole(Level level) {
		boolean enable = true;
		for(LoggerContext ctx : contexts) {
			if(ctx instanceof ConsoleLoggingContext) {
				enable = false;
			}
		}
		if(enable) {
			contexts.add(new ConsoleLoggingContext(level));
			log(Level.INFO, "Console logging enabled", null);
		}
	}

	@Override
	public void enableFile(Level level, String logFile) {
		enableFile(level, new File(logFile));
	}
	
	@Override
	public synchronized void enableFile(Level level, File logFile) {
		try {
			Iterator<LoggerContext> it = contexts.iterator();
			while(it.hasNext()) {
				LoggerContext ctx = it.next();
				if(ctx instanceof FileLoggingContext) {
					FileLoggingContext context = (FileLoggingContext) ctx;
					if(context.getFile().equals(logFile)) {
						context.close();
						it.remove();
					}
				}
			}
			contexts.add(new FileLoggingContext(level, logFile));
		} catch (IOException e) {
			System.err.println("Error logging to file");
			e.printStackTrace();
		}
	}
	
	@Override
	public synchronized void enableFile(Level level, File logFile, int maxFiles, long maxSize) {
		try {
			contexts.add(new FileLoggingContext(level, logFile, maxFiles, maxSize));
		} catch (IOException e) {
			System.err.println("Error logging to file");
			e.printStackTrace();
		}
	}
	
	public synchronized void reset() {
		for(LoggerContext ctx : contexts) {
			ctx.close();
		}
		
		contexts.clear();
	}
	
	@Override
	public synchronized boolean isLogging(Level level) {
		for(LoggerContext context : contexts) {
			if(context.isLogging(level)) {
				return true;
			}
		}
		return false;
	}

	public static String prepareLog(Level level, String msg, Throwable e, Object... args) {

		int i=0;
		int idx=0;
		int idx2=0;
		StringBuffer buffer = new StringBuffer();
		
		buffer.append(String.format("%s [%20s] %6s - ", 
				df.format(new Date()), 
				Thread.currentThread().getName(),
				level.name()));
		
		if(args.length > 0 && msg.indexOf("{}") > -1) {
			
			while(i < args.length && ((idx2 = msg.indexOf("{}", idx)) > -1)) {
				buffer.append(msg.substring(idx, idx2));
				buffer.append(args[i]);
				idx = idx2 + 2;
				i++;
			}
			
			if(msg.length() > idx+2) {
				buffer.append(msg.substring(idx2+2));
			}
		} else {
			buffer.append(msg);
		}
		
		buffer.append(System.lineSeparator());
		
		if(Objects.nonNull(e)) {
			StringWriter s = new StringWriter();
			PrintWriter w = new PrintWriter(s);
			e.printStackTrace(w);
			
			buffer.append(s.toString());
			buffer.append(System.lineSeparator());
		}
		
		return buffer.toString();
	}

	@Override
	public synchronized void log(Level level, String msg, Throwable e, Object... args) {
		for(LoggerContext context : contexts) {
			context.log(level, msg, e, args);
		}
	}

	@Override
	public synchronized void raw(Level level, String msg) {
		for(LoggerContext context : contexts) {
			context.raw(level, msg);
		}
	}

	@Override
	public void close() {
		/**
		 * This is the default context and this will never be called. 
		 */
	}

	@Override
	public synchronized void newline() {
		for(LoggerContext context : contexts) {
			context.newline();
		}
	}
}
