package com.sshtools.common.sshd.config;

/*-
 * #%L
 * Base API
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

import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import com.sshtools.common.publickey.authorized.Patterns;


public class Entry {
	
	AtomicInteger commentKey = new AtomicInteger(1);
	AtomicInteger blankKey = new AtomicInteger(1);
	protected SshdConfigFile sshdConfigFile;
	
	public Entry(SshdConfigFile sshdConfigFile) {
		this.sshdConfigFile = sshdConfigFile;
	}

	protected Map<String, SshdConfigFileEntry> keyEntries = new ListOrderedMap<>();
	
	protected ListOrderedMap<String, SshdConfigFileEntry> getKeyEntriesOrderedMap() {
		return (ListOrderedMap<String, SshdConfigFileEntry>) this.keyEntries;
	}
	
	public String getValue(String key) {
		SshdConfigFileEntry e = getEntry(key);
		if(Objects.isNull(e)) {
			throw new NoSuchElementException(String.format("No value exists for key %s", key));
		}
		return e.getValue();
	}
	
	public SshdConfigFileEntry getEntry(String key) {
		return getKeyEntries().get(key);
	}
	
	public Map<String, SshdConfigFileEntry> getKeyEntries() {
		return executeRead(new Callable<Map<String, SshdConfigFileEntry>>() {

			@Override
			public Map<String, SshdConfigFileEntry> call() throws Exception {
				return Entry.this.keyEntries;
			}
			
		}); 
	}
	
	public SshdConfigFileEntry find(final String key) {
		return executeRead(new Callable<SshdConfigFileEntry>() {

			@Override
			public SshdConfigFileEntry call() throws Exception {
				return Entry.this.keyEntries.get(key);
			}
		});
	}
	
	public void enable(String key, String value) {
		SshdConfigFileEntry e = find(key);
		if(Objects.nonNull(e)) {
    		e.setValue(value);
    		e.setCommentedOut(false);
    	} else {
    		appendEntry(new SshdConfigKeyValueEntry(key, value));
    	}
	}
	
	public void disable(String key) {
		SshdConfigFileEntry e = find(key);
		if(Objects.nonNull(e)) {
    		e.setCommentedOut(true);
    	} 
	}
	
	public SshdConfigKeyValueEntry findKeyValueEntry(final String key) {
		return executeRead(new Callable<SshdConfigKeyValueEntry>() {

			@Override
			public SshdConfigKeyValueEntry call() throws Exception {
				SshdConfigFileEntry keyValueEntry = Entry.this.keyEntries.get(key);
				if (!(keyValueEntry instanceof SshdConfigKeyValueEntry)) {
					throw new IllegalArgumentException(String.format("Value with key `%s` is not Key Value entry of type SshdConfigKeyValueEntry", key));
				}
				return (SshdConfigKeyValueEntry) keyValueEntry;
			}
		});
	}
	
	public int findEntryIndex(final String key) {
		return executeRead(new Callable<Integer>() {

			@Override
			public Integer call() throws Exception {
				return Entry.this.getKeyEntriesOrderedMap().indexOf(key);
			}
		});
	}
	
	public SshdConfigFileEntry findEntryAtIndex(final int index) {
		return executeRead(new Callable<SshdConfigFileEntry>() {

			@Override
			public SshdConfigFileEntry call() throws Exception {
				return (SshdConfigFileEntry) getKeyEntriesOrderedMap().getValue(index);
			}
		});
		
	}
	
	public void addCommentForEntry(final String key, final CommentEntry commentEntry) {
		executeWrite(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				int index = Entry.this.findEntryIndex(key);
				if (index == -1) {
					throw new IllegalArgumentException(String.format("Entry with key `%s` not found.", key));
				}
				Entry.this.getKeyEntriesOrderedMap().put(index, getCommentEntryKey(), commentEntry);
				return null;
			}
		});
		
	}
	
	/**
	 * Will add a comment entry at the beginning of the section.
	 * @param commentEntry
	 */
	public void addBeginingComment(final CommentEntry commentEntry) {
		executeWrite(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				Entry.this.getKeyEntriesOrderedMap().put(0, getCommentEntryKey(), commentEntry);
				return null;
			}
		});
		
	}
	
	/**
	 * Will update the value for the given key with new entry
	 * 
	 * @param key
	 * @param entry
	 */
	public void updateEntry(final String key, final String value) {
		executeWrite(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				SshdConfigFileEntry entry = (SshdConfigFileEntry) Entry.this.getKeyEntriesOrderedMap().get(key);
				if (entry == null) {
					throw new IllegalArgumentException(String.format("Entry with key `%s` not found.", key));
				}
				//unlikely case but still adding, comments and blank lines have implicit keys not exposed.
				if (entry instanceof CommentEntry || entry instanceof BlankEntry) {
					throw new IllegalArgumentException("Entry is not a valid entry is Comment or Blank");
				}
				
				SshdConfigKeyValueEntry keyValueEntry = (SshdConfigKeyValueEntry) entry;
				keyValueEntry.setValue(value);
				return null;
			}
		});
		
	}
	
	/**
	 * Will delete the entry for the given key.
	 * 
	 * @param key
	 */
	public void deleteEntry(final String key) {
		executeWrite(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				int index = findEntryIndex(key);
				if (index == -1) {
					throw new IllegalArgumentException(String.format("Entry with key `%s` not found.", key));
				}
				Entry.this.getKeyEntriesOrderedMap().removeIndex(index);
				return null;
			}
		});
		
	}
	
	/**
	 * Will delete the entry for the given key and value (when multiple values exist).
	 * 
	 * @param key
	 */
	public void deleteEntry(final String key, final String value) {
		executeWrite(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				int index = findEntryIndex(key);
				if (index == -1) {
					throw new IllegalArgumentException(String.format("Entry with key `%s` not found.", key));
				}
				
				SshdConfigFileEntry e = Entry.this.getKeyEntriesOrderedMap().getValue(index);
				if(e.getValue().equals(value)) {
					Entry.this.getKeyEntriesOrderedMap().removeIndex(index);
					
					if(e.hasNext()) {
						Entry.this.getKeyEntriesOrderedMap().put(index, key, e.getNext());
					}
				} else {
					SshdConfigKeyValueEntry kv = (SshdConfigKeyValueEntry) e;
					while(e.hasNext()) {
						e = e.getNext();
						if(e.getValue().equals(value)) {
							if(e.hasNext()) {
								kv.setNext(e.getNext());
							} else {
								kv.setNext(null);
							}
							break;
						}
						kv = (SshdConfigKeyValueEntry) e;
					}
				}
				return null;
			}
		});
		
	}
	
	/**
	 * This method would add entry after the past valid entry.
	 * If the section ends with a series of blank and comment lines.
	 * It will walk backwards from the end of the section, find the valid entry index
	 * and add the entry post that.
	 * 
	 * <br />
	 * Use this when editing an already existing file.
	 * 
	 * @param index
	 * @param sshdConfigFileEntry
	 */
	public int addEntry(final int index, final SshdConfigFileEntry sshdConfigFileEntry) {
		return executeWrite(new Callable<Integer>() {

			@Override
			public Integer call() throws Exception {
				String key = resolveKey(sshdConfigFileEntry);
				
				if (Entry.this.keyEntries.isEmpty()) {
					getKeyEntriesOrderedMap().put(key, sshdConfigFileEntry);
					return 0;
				}
				
				int indexToStartLookingFrom;
				if (index == -1) {
					// means we did not look for any entry we need to add to the end, before blank line or list of comments
					// any other positive value would indicate index to start from 
					indexToStartLookingFrom = Entry.this.keyEntries.size() - 1;
				} else {
					// this means we made call to findEntryToEdit
					indexToStartLookingFrom = index;
				}
				
				int indexToEnter = findLastValidEntry(indexToStartLookingFrom) + 1;
				getKeyEntriesOrderedMap().put(indexToEnter, key, sshdConfigFileEntry);

				return indexToEnter;
			}
			
		});
	}

	public void append(String key, String value) {
		appendEntry(new SshdConfigKeyValueEntry(key, value));
	}
	
	/**
	 * This will simply keep adding entries in a section, will not make any decision to check to 
	 * add new entry after a valid entry.
	 * <br />
	 * Use this to create a fresh file.
	 * 
	 * @param sshdConfigFileEntry
	 */
	public void appendEntry(final SshdConfigFileEntry sshdConfigFileEntry) {
		executeWrite(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				String key = resolveKey(sshdConfigFileEntry);
				if(Entry.this.keyEntries.containsKey(key)) {
					SshdConfigFileEntry entry = Entry.this.keyEntries.get(key);
					while(entry.hasNext()) {
						entry = entry.getNext();
					}
					entry.setNext(sshdConfigFileEntry);
				} else {
					Entry.this.keyEntries.put(key, sshdConfigFileEntry);
				}
				return null;
			}
		});
		
	}

	
	public Boolean entryMatches(final String key, final Collection<String> patterns) {
		return executeRead(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				SshdConfigKeyValueEntry sshdConfigFileEntry = (SshdConfigKeyValueEntry) Entry.this.getKeyEntries().get(key);
				
				if (sshdConfigFileEntry != null) {
					String[] parts = sshdConfigFileEntry.getValueParts();
					if (parts != null) {
						for (String part : parts) {
							if (Patterns.matchesWithCIDR(patterns, part)) {
								return true;
							}
						}
					}
				} 
				return false;
			}
		});
		
	}

	private String resolveKey(SshdConfigFileEntry sshdConfigFileEntry) {
		String key = null;
		
		if (sshdConfigFileEntry instanceof SshdConfigKeyValueEntry) {
			key = ((SshdConfigKeyValueEntry) sshdConfigFileEntry).getKey();
		} else if (sshdConfigFileEntry instanceof BlankEntry){
			key = getBlankEntryKey();
		} else if (sshdConfigFileEntry instanceof CommentEntry) {
			key = getCommentEntryKey();
		}
		return key;
	}
	
	/**
	 * There is no demarcation in the sshd file to distinguish between global section, match section
	 * also it is hard to tell comment is for which entry or some early entry is now a comment.
	 * 
	 * The basic idea for global or match section is to go the last entry in that section and walk back
	 * and find the last proper entry and add the new entry post that. There is catch to it, the comments 
	 * which are read from a file have a flag set which distinguish them from the one added by API/Program running.
	 * This is so as with API we add a comment and after that an entry. If we follow above logic without flag, the entry would
	 * walk up the comment added by API as the logic is supposed to work, but this would be wrong as we want the comment to be before
	 * the entry. This flag helps us ditinguish between API comments and one read from file. The code walks up the comments which are
	 * from file.
	 * 
	 * Note: The comment entry added fresh are not considered in logic, only the ones loaded from an existing
	 * file are considered. This is needed as when you are adding a comment via program, you need the entries to go after it,
	 * else any entry post addition of comment will end up above the comment. For this comments added via program are tagged loaded false
	 * and check is present in the logic below, the if condition.
	 * 
	 * <pre>
	 * 	# override default of no subsystems
	 *	#Subsystem	sftp	/usr/lib/misc/sftp-server
	 *	Subsystem	sftp	internal-sftp
	 *  ????????????? 
	 *	# the following are HPN related configuration options
	 *	# tcp receive buffer polling. disable in non autotuning kernels
	 *	#TcpRcvBufPoll yes
	 *  XXXXXXXXXXXXXXXXXX
	 * </pre>
	 * 
	 * In above scenario the entry should be made in the line containing ?????????????
	 * not XXXXXXXXXXXXXXXXXX
	 * 
	 * @param index
	 * @return
	 */
	private int findLastValidEntry(final int index) {
		return executeRead(new Callable<Integer>() {

			@Override
			public Integer call() throws Exception {
				int localIndex = index;// index is final, cannot manipulate, hence a copy
				SshdConfigFileEntry value = (SshdConfigFileEntry) getKeyEntriesOrderedMap().getValue(localIndex);
				while(localIndex > 0) {
					if (value instanceof SshdConfigKeyValueEntry || (value instanceof CommentEntry && ((CommentEntry)value).isNotLoaded())) {
						break;
					}
					--localIndex;
					value = (SshdConfigFileEntry) getKeyEntriesOrderedMap().getValue(localIndex);
				}
				
				return localIndex;
			}
		});
		
	}
	
	public String getBlankEntryKey() {
		return String.format("Blank%d", blankKey.getAndIncrement());
	}
	
	public String getCommentEntryKey() {
		return String.format("Comment%d", commentKey.getAndIncrement());
	}
	
	protected <T> T executeRead(Callable<T> callable) {
		return this.sshdConfigFile.executeRead(callable);
	}
	
	protected <T> T executeWrite(Callable<T> callable) {
		return this.sshdConfigFile.executeWrite(callable);
	}
	
	public static abstract class AbstractEntryBuilder<T> {
		
		protected SshdConfigFileCursor cursor = new SshdConfigFileCursor();
		protected int pointer = -1;// pointer for the section not complete file
		protected SshdConfigFile file;
		
		protected abstract Entry getManagedInstance();
		
		/**
		 * Will update the value for the given key with new entry.
		 * 
		 * @param key
		 * @param entry
		 * @return
		 */
		@SuppressWarnings("unchecked")
		public T updateEntry(String key, String value) {
			this.getManagedInstance().updateEntry(key, value);
			return (T) this;
		}
		
		/**
		 * Will delete the entry for the given key.
		 * 
		 * @param key
		 * @return
		 */
		@SuppressWarnings("unchecked")
		public T deleteEntry(String key) {
			this.getManagedInstance().deleteEntry(key);
			return (T) this;
		}
		
		/**
		 * Will add a comment entry at current location.
		 * 
		 * @param commentEntry
		 * @return
		 */
		@SuppressWarnings("unchecked")
		public T addComment(String comment) {
			this.addEntry(new CommentEntry(comment));
			return (T) this;
		}
		
		/**
		 * Will add a comment entry before the entry with the given key.
		 * Note: Multiple calls will add calls in reverse.
		 * <pre>
		 * 	addCommentForEntry(....1)
		 *  addCommentForEntry(....2)
		 *  addCommentForEntry(....3)
		 * </pre>
		 * Above will end up as first 3, then 2 finally 1.
		 * All entries are made at index at which the entry is found.
		 * @param comment
		 */
		@SuppressWarnings("unchecked")
		public T addCommentForEntry(String key, String comment) {
			this.getManagedInstance().addCommentForEntry(key, new CommentEntry(comment));
			return (T) this;
		}
		
		/**
		 * Will add a comment entry before the entry with the given key.
		 * Note: Multiple calls will add calls in reverse.
		 * <pre>
		 * 	addCommentForEntry(....1)
		 *  addCommentForEntry(....2)
		 *  addCommentForEntry(....3)
		 * </pre>
		 * Above will end up as first 3, then 2 finally 1.
		 * All entries are made at index at which the entry is found.
		 * @param commentEntry
		 */
		@SuppressWarnings("unchecked")
		public T addCommentForEntry(String key, CommentEntry commentEntry) {
			this.getManagedInstance().addCommentForEntry(key, commentEntry);
			return (T) this;
		}
		
		/**
		 * Will add a comment entry at the beginning of the section.
		 * Note: Multiple calls will add calls in reverse.
		 * <pre>
		 * 	addBeginingComment(....1)
		 *  addBeginingComment(....2)
		 *  addBeginingComment(....3)
		 * </pre>
		 * Above will end up as first 3, then 2 finally 1.
		 * All entries are made at index 0.
		 * @param commentEntry
		 */
		@SuppressWarnings("unchecked")
		public T addBeginingComment(CommentEntry commentEntry) {
			this.getManagedInstance().addBeginingComment(commentEntry);
			return (T) this;
		}
		
		/**
		 * Will add entry after last non blank, non comment entry in a section (Global, Match)
		 * Note: Use while operating on file after loading it.
		 * 
		 * @param sshdConfigFileEntry
		 * @return
		 */
		@SuppressWarnings("unchecked")
		public T addEntry(final SshdConfigFileEntry sshdConfigFileEntry) {
			return this.file.executeWrite(new Callable<T>() {

				@Override
				public T call() throws Exception {
					int indexToEnter = AbstractEntryBuilder.this.getManagedInstance().addEntry(AbstractEntryBuilder.this.pointer, sshdConfigFileEntry);
					if (AbstractEntryBuilder.this.pointer != -1) {
						AbstractEntryBuilder.this.pointer = indexToEnter;
					}
					return (T) AbstractEntryBuilder.this;
				}
			});
			
		}
		
		/**
		 * Will add entry to the end of section.
		 * Note: Use while writing a file.
		 * 
		 * @param sshdConfigFileEntry
		 * @return
		 */
		@SuppressWarnings("unchecked")
		public T appendEntry(SshdConfigFileEntry sshdConfigFileEntry) {
			this.getManagedInstance().appendEntry(sshdConfigFileEntry);
			return (T) this;
		}
		
		public SshdConfigFileEntry findEntry(String key) {
			return this.getManagedInstance().getKeyEntries().get(key);
		}
		
		@SuppressWarnings("unchecked")
		public T findEntry(String key, Result<SshdConfigKeyValueEntry> result) {
			SshdConfigKeyValueEntry sshdConfigFileEntry = (SshdConfigKeyValueEntry) this.getManagedInstance().getKeyEntries().get(key);
			result.set(sshdConfigFileEntry);
			return (T) this;
		}
		
		@SuppressWarnings("unchecked")
		public T entryMatches(String key, Collection<String> patterns, Result<Boolean> result) {
			result.set(entryMatches(key, patterns));
			return (T) this;
		}
		
		public Boolean entryMatches(String key, Collection<String> patterns) {
			return this.getManagedInstance().entryMatches(key, patterns);
		}
		
		public int findEntryIndex(String key) {
			return this.getManagedInstance().findEntryIndex(key);
		}
		
		public SshdConfigFileEntry findEntryAtIndex(int index) {
			return this.getManagedInstance().findEntryAtIndex(index);
		}
		
		@SuppressWarnings("unchecked")
		public T findEntryIndex(String key, Result<Integer> result) {
			int index = this.getManagedInstance().findEntryIndex(key);
			result.set(index);
			return (T) this;
		}
		
		@SuppressWarnings("unchecked")
		public T findEntryToEdit(final String key) {
			return this.file.executeWrite(new Callable<T>() {

				@Override
				public T call() throws Exception {
					int index = AbstractEntryBuilder.this.getManagedInstance().getKeyEntriesOrderedMap().indexOf(key);
					if (index == -1) {
						throw new IllegalArgumentException(String.format("Entry with key `%s` not found.", key));
					}
					
					AbstractEntryBuilder.this.pointer = index;
					return (T) AbstractEntryBuilder.this;
				}
			});
			
		}
		
		@SuppressWarnings("unchecked")
		public T resetPointer() {
			return this.file.executeWrite(new Callable<T>() {

				@Override
				public T call() throws Exception {
					AbstractEntryBuilder.this.pointer = -1;
					return (T) AbstractEntryBuilder.this;
				}
			});
			
		}
		
		public SshdConfigFileCursor cursor() {
			return this.cursor;
		}
		
		public interface Result<T> {
			T get();
			void set(T value);
		}
	}
}
