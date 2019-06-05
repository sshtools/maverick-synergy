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

import com.sshtools.common.logger.Log.Level;

public class DefaultLoggerContext implements LoggerContext {

	Collection<LoggerContext> contexts = new ArrayList<>();
	static DateFormat df = new SimpleDateFormat("dd MMM yyyy HH:mm:ss,SSS");
	Properties loggingProperties; 
	File propertiesFile;
	FileWatcher watcher;
	
	public DefaultLoggerContext() {
		propertiesFile = new File(System.getProperty("maverick.log.config", "logging.properties"));
		loadFile();
		watcher = new FileWatcher(propertiesFile);
		watcher.start();
	}
	
	private synchronized void loadFile() {

		for(LoggerContext ctx : contexts) {
			ctx.close();
		}
		
		contexts.clear();

		if(propertiesFile.exists()) {
			loggingProperties = new Properties();
			try(InputStream in  = new FileInputStream(propertiesFile)) {
				loggingProperties.load(in);
			} catch(IOException e) {
				e.printStackTrace();
			}
		} else {
			loggingProperties = System.getProperties();
		}
		
		if("true".equalsIgnoreCase(loggingProperties.getProperty("maverick.log.console"))) {
			enableConsole(Level.valueOf(loggingProperties.getProperty("maverick.log.console.level", "INFO")));
		}
		
		if("true".equalsIgnoreCase(loggingProperties.getProperty("maverick.log.file"))) {
			enableFile(Level.valueOf(loggingProperties.getProperty("maverick.log.file.level", "INFO")),
					new File(loggingProperties.getProperty("maverick.log.file.path", "application.log")),
					Integer.parseInt(loggingProperties.getProperty("maverick.log.file.maxFiles", "10")),
					Long.parseLong(loggingProperties.getProperty("maverick.log.file.maxSize", String.valueOf(1024 * 1024 * 20))));
		}
		
		log(Level.INFO, "Reloaded logging configuration %s", null, propertiesFile.getName());
	}

	public synchronized Properties getLoggingProperties() {
		return loggingProperties;
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
				String.format(msg, args),
				System.lineSeparator());
		
		if(Objects.isNull(e)) {
			return log;
		}
		
		StringWriter s = new StringWriter();
		PrintWriter w = new PrintWriter(s);
		e.printStackTrace(w);
		
		return log + System.lineSeparator() + s.toString() + System.lineSeparator();
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
	        setDaemon(true);
	    }

	    public boolean isStopped() { return stop.get(); }
	    public void stopThread() { stop.set(true); }

	    public void doOnChange() {
	        loadFile();
	    }

	    @Override
	    public void run() {
	        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
	            Path path = file.getAbsoluteFile().toPath().getParent();
	            path.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
	            while (!isStopped()) {
	                WatchKey key;
	                try { key = watcher.poll(25, TimeUnit.MILLISECONDS); }
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
