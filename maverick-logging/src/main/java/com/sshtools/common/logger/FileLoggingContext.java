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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;

import com.sshtools.common.logger.Log.Level;
import com.sshtools.common.util.IOUtils;
import com.sshtools.common.util.RandomAccessOutputStream;

public class FileLoggingContext extends AbstractLoggingContext {


	BufferedWriter currentWriter = null;
	OutputStream currentOut = null;
	RandomAccessFile currentFile = null;
	Object currentFileKey = null;
	long lastWriteAt = 0L;
	long suppressedUntil = 0L;
	long idleCloseMillis;
	
	long maxSize;
	int maxFiles;
	File logFile;
	boolean logging = true;

	public FileLoggingContext(Level level, File logFile) throws IOException {
		this(level, logFile, 10, 1024 * 1024 * 20L, 30000L);
	}
	
	public FileLoggingContext(Level level, File logFile, int maxFiles, long maxSize) throws IOException {
		this(level, logFile, maxFiles, maxSize, 30000L);
	}
	
	public FileLoggingContext(Level level, File logFile, int maxFiles, long maxSize, long idleCloseMillis)
			throws IOException {
		super(level);
		this.logFile = logFile;
		if(!logFile.exists()) {
			logFile.getAbsoluteFile().getParentFile().mkdirs();
		}
		this.maxFiles = maxFiles;
		this.maxSize = maxSize;
		this.idleCloseMillis = idleCloseMillis;
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
		currentFileKey = readFileKey();
	}

	private Object readFileKey() {
		try {
			BasicFileAttributes attrs = Files.readAttributes(logFile.toPath(), BasicFileAttributes.class);
			return attrs.fileKey();
		} catch (IOException e) {
			return null;
		}
	}


	@Override
	public boolean isLogging(Level level) {
		return super.isLogging(level) && (logging || System.currentTimeMillis() >= suppressedUntil);
	}
	
	@Override
	public void log(Level level, String msg, Throwable e, Object... args) {
		logToFile(DefaultLoggerContext.prepareLog(level, msg, e, args), true);
	}

	private synchronized void logToFile(String msg, boolean flush) {
		long now = System.currentTimeMillis();
		if(!logging && now < suppressedUntil) {
			return;
		}
		try {
			maybeCloseIfIdle(now);
			ensureOpen();
			checkRollingLog();
			if(currentFile.getChannel().isOpen()) {
				currentWriter.write(msg);
				if(flush) {
					currentWriter.flush();
				}
				lastWriteAt = now;
				logging = true;
			}
		} catch (IOException e) {
			System.err.println(String.format("Failed to log to %s", logFile.getName()));
			e.printStackTrace();
			suppressedUntil = now + 5000L;
			logging = false;
			tryRecover(msg, flush);
		}
	}

	private void tryRecover(String msg, boolean flush) {
		try {
			closeLog();
			ensureOpen();
			if(currentFile != null && currentFile.getChannel().isOpen()) {
				currentWriter.write(msg);
				if(flush) {
					currentWriter.flush();
				}
				lastWriteAt = System.currentTimeMillis();
				logging = true;
			}
		} catch (IOException ignored) {
			// Suppress repeated failures; next attempt will retry after backoff.
		}
	}

	private void maybeCloseIfIdle(long now) {
		if(idleCloseMillis > 0 && lastWriteAt > 0 && (now - lastWriteAt) >= idleCloseMillis) {
			closeLog();
		}
	}

	private void ensureOpen() throws IOException {
		if(currentFile == null || currentWriter == null || !currentFile.getChannel().isOpen()) {
			createLogFile();
			logging = true;
		}
	}

	private void closeLog() {
		IOUtils.closeStream(currentWriter);
		IOUtils.closeStream(currentOut);
		IOUtils.closeStream(currentFile);
		currentWriter = null;
		currentOut = null;
		currentFile = null;
		currentFileKey = null;
	}
	
	private synchronized void checkRollingLog() throws IOException {

		if(currentFile == null || !currentFile.getChannel().isOpen()) {
			return;
		}

		if(!logFile.exists()) {
			closeLog();
			createLogFile();
			return;
		}

		BasicFileAttributes attrs = Files.readAttributes(logFile.toPath(), BasicFileAttributes.class);
		Object fileKey = attrs.fileKey();
		if(fileKey != null && currentFileKey != null && !fileKey.equals(currentFileKey)) {
			closeLog();
			createLogFile();
			return;
		}

		long currentLength = currentFile.length();
		long pathLength = attrs.size();
		if(pathLength < currentLength) {
			if(fileKey == null || currentFileKey == null) {
				closeLog();
				createLogFile();
				return;
			}
			currentFile.seek(pathLength);
		}

		if(currentLength > maxSize) {
			closeLog();
			IOUtils.rollover(logFile, maxFiles);
			createLogFile();
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

	public File getFile() {
		return logFile;
	}
}
