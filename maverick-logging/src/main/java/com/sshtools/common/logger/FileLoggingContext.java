
package com.sshtools.common.logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;

import com.sshtools.common.logger.Log.Level;
import com.sshtools.common.util.IOUtils;
import com.sshtools.common.util.RandomAccessOutputStream;

public class FileLoggingContext extends AbstractLoggingContext {


	BufferedWriter currentWriter = null;
	OutputStream currentOut = null;
	RandomAccessFile currentFile = null;
	
	long maxSize;
	int maxFiles;
	File logFile;
	boolean logging = true;

	public FileLoggingContext(Level level, File logFile) throws IOException {
		this(level, logFile, 10, 1024 * 1024 * 20L);
	}
	
	public FileLoggingContext(Level level, File logFile, int maxFiles, long maxSize) throws IOException {
		super(level);
		this.logFile = logFile;
		if(!logFile.exists()) {
			logFile.getAbsoluteFile().getParentFile().mkdirs();
		}
		this.maxFiles = maxFiles;
		this.maxSize = maxSize;
		createLogFile();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				closeLog();
			}
		});
	}

	private void createLogFile() throws IOException {
		currentFile = new RandomAccessFile(logFile, "rw");
		currentFile.seek(currentFile.length());
		currentWriter = new BufferedWriter(new OutputStreamWriter(new RandomAccessOutputStream(currentFile)), 65536);
		log(Level.INFO, String.format("Logging file %s", logFile.getAbsolutePath()), null);
	}


	@Override
	public boolean isLogging(Level level) {
		return logging && super.isLogging(level);
	}
	
	@Override
	public void log(Level level, String msg, Throwable e, Object... args) {
		logToFile(DefaultLoggerContext.prepareLog(level, msg, e, args), true);
	}

	private synchronized void logToFile(String msg, boolean flush) {
		try {
			checkRollingLog();
			if(currentFile.getChannel().isOpen()) {
				currentWriter.write(msg);
				if(flush) {
					currentWriter.flush();
				}
			}
		} catch (IOException e) {
			System.err.println(String.format("Failed to log to %s", logFile.getName()));
			e.printStackTrace();
			logging = false;
		}
	}

	private void closeLog() {
		IOUtils.closeStream(currentWriter);
		IOUtils.closeStream(currentOut);
	}
	
	private synchronized void checkRollingLog() throws IOException {

		if(currentFile.getChannel().isOpen()) {
			if(currentFile.length() > maxSize) {
				closeLog();
				IOUtils.rollover(logFile, maxFiles);
				createLogFile();
			}
		}
	}

	public synchronized void close() {
		closeLog();
	}

	@Override
	public void raw(Level level, String msg) {
		logToFile(DefaultLoggerContext.prepareLog(level, "", null), false);
		logToFile(msg, true);
	}

	@Override
	public void newline() {
		logToFile(System.lineSeparator(), true);
	}
}
