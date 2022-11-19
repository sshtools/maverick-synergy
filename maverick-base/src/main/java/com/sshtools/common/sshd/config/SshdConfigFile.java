package com.sshtools.common.sshd.config;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import com.sshtools.common.sshd.config.Entry.AbstractEntryBuilder;
import com.sshtools.common.sshd.config.MatchEntry.MatchEntryBuilder;


/**
 * SshdConfigFile
 * 		-- GlobalEntry (1..1)
 * 			-- Map <String, SshdConfigFileEntry>
 * 		-- MatchEntries (1..N) *
 * 			-- Map <String, SshdConfigFileEntry>
 * 
 * @author gnode
 *
 */
public class SshdConfigFile {
	
	private static final int TIME_OUT_SECONDS = 20;
	private GlobalEntry globalEntry;
	private List<MatchEntry> matchEntries = new LinkedList<>();
	private ReadWriteLock lock = new ReentrantReadWriteLock();
	private ReadLock readLock = (ReadLock) lock.readLock();
	private WriteLock writeLock = (WriteLock) lock.writeLock();
	
	
	private SshdConfigFile() {
		this.globalEntry = new GlobalEntry(this);
	}
	
	public MatchEntry findMatchEntry(final Map<String, String> params) {
		return executeRead(new Callable<MatchEntry>() {

			@Override
			public MatchEntry call() throws Exception {
				outter: for (MatchEntry matchEntry : matchEntries) {
					for (String paramKey : params.keySet()) {
						if (!matchEntry.hasKey(paramKey)) {
							continue outter;
						}
						
						if (!matchEntry.matchValueExact(paramKey, params)) {
							continue outter;
						}
					}
					
					return matchEntry;
					
				}
				return null;
			}
		});
	}
	
	public MatchEntry findMatchEntryWithMatch(final Map<String, Collection<String>> params) {
		return executeRead(new Callable<MatchEntry>() {

			@Override
			public MatchEntry call() throws Exception {
				outter: for (MatchEntry matchEntry : matchEntries) {
					for (String paramKey : params.keySet()) {
						if (!matchEntry.hasKey(paramKey)) {
							continue outter;
						}
						
						if (!matchEntry.matchValueAgainstPattern(paramKey, params.get(paramKey))) {
							continue outter;
						}
					}
					
					return matchEntry;
					
				}
				return null;
			}
		});
	}
	
	public GlobalEntry getGlobalEntry() {
		return this.globalEntry;
	}
	
	public Iterator<MatchEntry> getMatchEntriesIterator() {
		return this.matchEntries.iterator();
	}
	

	public void removeMatchEntry(final MatchEntry entry) {
		executeWrite(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				SshdConfigFile.this.matchEntries.remove(entry);
				return null;
			}
		});
	}
	
	public MatchEntry addMatchEntry() {
		return executeWrite(new Callable<MatchEntry>() {

			@Override
			public MatchEntry call() throws Exception {
				MatchEntry matchEntry = new MatchEntry(SshdConfigFile.this);
				SshdConfigFile.this.matchEntries.add(matchEntry);
				return matchEntry;
			}
		});
		
	}
	
	
	public <T> T executeRead(Callable<T> callable) {
		try {
			this.readLock.tryLock(TIME_OUT_SECONDS, TimeUnit.SECONDS);
			return callable.call();
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		} finally {
			this.readLock.unlock();
		}
	}
	
	public <T> T executeWrite(Callable<T> callable) {
		try {
			this.writeLock.tryLock(TIME_OUT_SECONDS, TimeUnit.SECONDS);
			return callable.call();
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		} finally {
			this.writeLock.unlock();
		}
	}
	
	public static SshdConfigFileBuilder builder() {
		return new SshdConfigFileBuilder();
	}
	
	public static class SshdConfigFileBuilder extends AbstractEntryBuilder<SshdConfigFileBuilder> implements EntryBuilder<SshdConfigFileBuilder, SshdConfigFileBuilder> {
		private GlobalEntry managedInstance;
		
		public SshdConfigFileBuilder() {
			this.file = new SshdConfigFile();
			this.managedInstance = this.file.globalEntry;
			this.cursor.set(this.managedInstance);
		} 
		
		public MatchEntryBuilder matchEntry(boolean commentedOut) {
			return new MatchEntryBuilder(this, this.file, this.cursor, commentedOut);
		}
		
		public MatchEntryBuilder findMatchEntry(Map<String, String> params) {
			MatchEntry matchEntry = this.file.findMatchEntry(params);
			if (matchEntry == null) {
				throw new IllegalArgumentException("Match entry not found, is null.");
			}
			return new MatchEntryBuilder(this, this.file, this.cursor, matchEntry);
		}
		
		public MatchEntryBuilder findMatchEntryWithMatch(Map<String, Collection<String>> params) {
			MatchEntry matchEntry = this.file.findMatchEntryWithMatch(params);
			if (matchEntry == null) {
				throw new IllegalArgumentException("Match entry not found, is null.");
			}
			return new MatchEntryBuilder(this, this.file, this.cursor, matchEntry);
		}
		
		public SshdConfigFile build() {
			return this.file;
		}
		
		public <T> T executeRead(Callable<T> callable) {
			return this.file.executeRead(callable);
		}
		
		public <T> T executeWrite(Callable<T> callable) {
			return this.file.executeWrite(callable);
		}
		
		public SshdConfigFileCursor cursor() {
			return this.cursor;
		}

		@Override
		public SshdConfigFileBuilder end() {
			return this;
		}

		@Override
		protected Entry getManagedInstance() {
			return this.managedInstance;
		}
	}
}


