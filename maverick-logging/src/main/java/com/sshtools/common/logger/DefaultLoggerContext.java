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
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.common.logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sshtools.common.logger.Log.Level;
import com.sshtools.common.util.IOUtils;

public class DefaultLoggerContext implements RootLoggerContext {

	Collection<LoggerContext> contexts = new ArrayList<>();
	static DateFormat df = new SimpleDateFormat("dd MMM yyyy HH:mm:ss,SSS");
	Properties props; 
	File propertiesFile;
	FileWatcher watcher;
	
	public DefaultLoggerContext() {
		propertiesFile = new File(System.getProperty("maverick.log.config", "logging.properties"));
		loadFile();
		watcher = new FileWatcher(propertiesFile);
		watcher.start();
	}
	
	public void shutdown() {
		watcher.stopThread();
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
		
		log(Level.INFO, "Reloaded logging configuration %s [%s]", null, propertiesFile.getName(), propertiesFile.getAbsolutePath());
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

	public void enableFile(Level level, String logFile) {
		enableFile(level, new File(logFile));
	}
	
	public synchronized void enableFile(Level level, File logFile) {
		try {
			contexts.add(new FileLoggingContext(level, logFile));
		} catch (IOException e) {
			System.err.println("Error logging to file");
			e.printStackTrace();
		}
	}
	
	public synchronized void enableFile(Level level, File logFile, int maxFiles, long maxSize) {
		try {
			contexts.add(new FileLoggingContext(level, logFile, maxFiles, maxSize));
		} catch (IOException e) {
			System.err.println("Error logging to file");
			e.printStackTrace();
		}
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
		String log = String.format("%s [%20s] %6s - %s%s", 
				df.format(new Date()), 
				Thread.currentThread().getName(),
				level.name(), 
				String.format(processArgs(msg, args), args),
				System.lineSeparator());
		
		if(Objects.isNull(e)) {
			return log;
		}
		
		StringWriter s = new StringWriter();
		PrintWriter w = new PrintWriter(s);
		e.printStackTrace(w);
		
		return log + System.lineSeparator() + s.toString() + System.lineSeparator();
	}
	
	private static String processArgs(String msg, Object[] args) {
		
		int idx = 0;
		while(msg.indexOf("{}") > -1 && idx < args.length) {
			msg = msg.replace("{}", args[idx++].toString());
		}
		return msg;
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

	/**
	 * From https://stackoverflow.com/questions/16251273/can-i-watch-for-single-file-change-with-watchservice-not-the-whole-directory
	 */
	public class FileWatcher extends Thread {
	    private final File file;
	    private AtomicBoolean stop = new AtomicBoolean(false);

	    public FileWatcher(File file) {
	        this.file = file;
	        setName("MaverickLoggerWatcher");
	        setDaemon(true);
	        Runtime.getRuntime().addShutdownHook(new Thread() {
	        	public void run() {
	        		stopThread();
	        	}
	        });
	    }

	    public boolean isStopped() { return stop.get(); }
	    public void stopThread() { 
	    	stop.set(true); 
	    }

	    public void doOnChange() {
	        loadFile();
	    }

	    @Override
	    public void run() {
	        try (WatchService service = FileSystems.getDefault().newWatchService()) {
	            Path path = file.getAbsoluteFile().toPath().getParent();
	            path.register(service, StandardWatchEventKinds.ENTRY_MODIFY);
	            while (!isStopped()) {
	                WatchKey key;
	                try { key = service.poll(25, TimeUnit.MILLISECONDS); }
	                catch (InterruptedException e) { return; }
	                if (key == null) { Thread.yield(); continue; }

	                for (WatchEvent<?> event : key.pollEvents()) {
	                    WatchEvent.Kind<?> kind = event.kind();

	                    @SuppressWarnings("unchecked")
	                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
	                    Path filename = ev.context();

	                    if (kind == StandardWatchEventKinds.OVERFLOW) {
	                        Thread.yield();
	                        continue;
	                    } else if (kind == java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
	                            && filename.toString().equals(file.getName())) {
	                        doOnChange();
	                    }
	                    boolean valid = key.reset();
	                    if (!valid) { break; }
	                }
	                Thread.yield();
	            }
	        } catch (Throwable e) {
	            // Log or rethrow the error
	        }
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
