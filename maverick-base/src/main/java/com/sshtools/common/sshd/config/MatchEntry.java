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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import com.sshtools.common.publickey.authorized.Patterns;
import com.sshtools.common.sshd.config.SshdConfigFile.SshdConfigFileBuilder;
import com.sshtools.common.util.Utils;


/**
 * User, Group, Host, LocalAddress, LocalPort, and Address
 */
public class MatchEntry extends GlobalConfiguration {
	
	private static final String MATCH_STRING_TEMPLATE = "%s %s";
	public static final String MATCH_ENTRY_CRITERIA_USER = "User";
	public static final String MATCH_ENTRY_CRITERIA_GROUP = "Group";
	public static final String MATCH_ENTRY_CRITERIA_HOST = "Host";
	public static final String MATCH_ENTRY_CRITERIA_LOCAL_ADDRESS = "LocalAddress";
	public static final String MATCH_ENTRY_CRITERIA_LOCAL_PORT = "LocalPort";
	public static final String MATCH_ENTRY_CRITERIA_ADDRESS = "Address";
	public static final String MATCH_ENTRY_CRITERIA_RDOMAIN = "RDomain";
	
	private Map<String, Set<String>> matchCriteriaMap = new LinkedHashMap<>();
	private List<CommentEntry> matchCriteriaCommentEntries = new ArrayList<>();
	private boolean commentedOut;
	
	public MatchEntry(SshdConfigFile sshdConfigFile) {
		super(sshdConfigFile);
	}
	
	public void addComment(String comment) {
		matchCriteriaCommentEntries.add(new CommentEntry(comment));
	}

	public void addMatchCriteriaComment(final CommentEntry commentEntry) {
		executeWrite(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				MatchEntry.this.matchCriteriaCommentEntries.add(commentEntry);
				return null;
			}
		});
		
	}
	
	public void disable() {
		commentedOut = true;
		for(String key : getKeyEntries().keySet()) {
			disable(key);
		}
	}
	
	public boolean isCommentedOut() {
		return commentedOut;
	}
	
	public void remove() {
		executeWrite(new Callable<Void>() {
			public Void call() throws Exception {
				sshdConfigFile.removeMatchEntry(MatchEntry.this);
				return null;
			}
		});
	}
	
	public String matchEntryCriteriaAsString() {
		return executeRead(new Callable<String>() {

			@Override
			public String call() throws Exception {
				StringBuilder string = new StringBuilder(256);
				Set<String> keySet = matchCriteriaMap.keySet();
				Iterator<String> keySetIterator = keySet.iterator();
				
				String first = keySetIterator.next();
				string.append(String.format(MATCH_STRING_TEMPLATE, first, Utils.csv(matchCriteriaMap.get(first))));
				if (!keySetIterator.hasNext()) {
					return string.toString();
				}
				
				while (keySetIterator.hasNext()) {
		            string.append(" ");
		            String key = keySetIterator.next();
		            string.append(String.format(MATCH_STRING_TEMPLATE, key, Utils.csv(matchCriteriaMap.get(key))));
		        }
				
				return string.toString();
			}
		});
		
	}
	
	// ========================================= GENERIC =================================================
	private void addCriteria(final String criteria, final String[] values) {
		executeWrite(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				Set<String> valuesSet = new LinkedHashSet<>(Arrays.asList(values));
				MatchEntry.this.matchCriteriaMap.put(criteria, valuesSet);
				return null;
			}
		});
		
	}
	
	public void addCriteria(String criteria, String value) {
		addCriteria(criteria, value.split(","));
	}
	
	
	public boolean matchValueAgainstPattern(final String paramKey, final Collection<String> patterns) {
		return executeRead(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				Set<String> values = MatchEntry.this.matchCriteriaMap.get(paramKey);
				
				for (String value : values) {
					if (Patterns.matchesWithCIDR(patterns, value)) {
						return true;
					}
				}
				
				return false;
			}
		});
		
	}
	
	public boolean matchValueExact(final String paramKey, final Map<String, String> params) {
		return executeRead(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				Set<String> values = MatchEntry.this.matchCriteriaMap.get(paramKey);
				return values.contains(params.get(paramKey));
			}
		});
		
	}
	
	public Iterator<CommentEntry> getMatchCriteriaCommentEntriesIterator() {
		return this.matchCriteriaCommentEntries.iterator();
	}
	
	private void pushCriteria(final String criteria, final String value) {
		executeWrite(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				if (!matchCriteriaMap.containsKey(criteria)) {
					addCriteria(criteria, value);
					return null;
				}
				MatchEntry.this.matchCriteriaMap.get(criteria).add(value);
				return null;
			}
		});
		
	}
	
	private void updateCriteria(final String criteria, final String value) {
		executeWrite(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				if (!matchCriteriaMap.containsKey(criteria)) {
					addCriteria(criteria, value);
					return null;
				}
				MatchEntry.this.matchCriteriaMap.get(criteria).clear();
				MatchEntry.this.matchCriteriaMap.get(criteria).addAll(Arrays.asList(value.split(",")));
				return null;
			}
		});
		
	}
	
	private void deleteCriteria(final String criteria) {
		executeWrite(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				MatchEntry.this.matchCriteriaMap.get(criteria).clear();
				MatchEntry.this.matchCriteriaMap.remove(criteria);
				return null;
			}
		});
		
	}
	// ========================================= GENERIC =================================================
	
	
	// ========================================= USER =================================================
	public void addUserCriteria(String[] values) {
		addCriteria(MATCH_ENTRY_CRITERIA_USER, values);
	}
	
	public void addUserCriteria(String value) {
		addCriteria(MATCH_ENTRY_CRITERIA_USER, value.split(","));
	}
	
	public void pushUserCriteria(String value) {
		pushCriteria(MATCH_ENTRY_CRITERIA_USER, value);
	}
	
	public void updateUserCriteria(String value) {
		updateCriteria(MATCH_ENTRY_CRITERIA_USER, value);
	}
	
	public void deleteUserCriteria() {
		deleteCriteria(MATCH_ENTRY_CRITERIA_USER);
	}
	// ========================================= USER =================================================
	
	
	// ========================================= GROUP =================================================
	public void addGroupCriteria(String[] values) {
		addCriteria(MATCH_ENTRY_CRITERIA_GROUP, values);
	}
	
	public void addGroupCriteria(String value) {
		addCriteria(MATCH_ENTRY_CRITERIA_GROUP, value.split(","));
	}
	
	public void pushGroupCriteria(String value) {
		pushCriteria(MATCH_ENTRY_CRITERIA_GROUP, value);
	}
	
	public void updateGroupCriteria(String value) {
		updateCriteria(MATCH_ENTRY_CRITERIA_GROUP, value);
	}
	
	public void deleteGroupCriteria() {
		deleteCriteria(MATCH_ENTRY_CRITERIA_GROUP);
	}
	// ========================================= GROUP =================================================
	
	
	// ========================================= HOST =================================================
	public void addHostCriteria(String[] values) {
		addCriteria(MATCH_ENTRY_CRITERIA_HOST, values);
	}
	
	public void addHostCriteria(String value) {
		addCriteria(MATCH_ENTRY_CRITERIA_HOST, value.split(","));
	}
	
	public void pushHostCriteria(String value) {
		pushCriteria(MATCH_ENTRY_CRITERIA_HOST, value);
	}
	
	public void updateHostCriteria(String value) {
		updateCriteria(MATCH_ENTRY_CRITERIA_HOST, value);
	}
	
	public void deleteHostCriteria() {
		deleteCriteria(MATCH_ENTRY_CRITERIA_HOST);
	}
	// ========================================= HOST =================================================
	
	
	// ========================================= LOCAL_ADDRESS =================================================
	public void addLocalAddressCriteria(String[] values) {
		addCriteria(MATCH_ENTRY_CRITERIA_LOCAL_ADDRESS, values);
	}
	
	public void addLocalAddressCriteria(String value) {
		addCriteria(MATCH_ENTRY_CRITERIA_LOCAL_ADDRESS, value.split(","));
	}
	
	public void pushLocalAddressCriteria(String value) {
		pushCriteria(MATCH_ENTRY_CRITERIA_LOCAL_ADDRESS, value);
	}
	
	public void updateLocalAddressCriteria(String value) {
		updateCriteria(MATCH_ENTRY_CRITERIA_LOCAL_ADDRESS, value);
	}
	
	public void deleteLocalAddressCriteria() {
		deleteCriteria(MATCH_ENTRY_CRITERIA_LOCAL_ADDRESS);
	}
	// ========================================= LOCAL_ADDRESS =================================================
	
	
	// ========================================= LOCAL_PORT =================================================
	public void addLocalPortCriteria(String[] values) {
		addCriteria(MATCH_ENTRY_CRITERIA_LOCAL_PORT, values);
	}
	
	public void addLocalPortCriteria(String value) {
		addCriteria(MATCH_ENTRY_CRITERIA_LOCAL_PORT, value.split(","));
	}
	
	public void pushLocalPortCriteria(String value) {
		pushCriteria(MATCH_ENTRY_CRITERIA_LOCAL_PORT, value);
	}
	
	public void updateLocalPortCriteria(String value) {
		updateCriteria(MATCH_ENTRY_CRITERIA_LOCAL_PORT, value);
	}
	
	public void deleteLocalPortCriteria() {
		deleteCriteria(MATCH_ENTRY_CRITERIA_LOCAL_PORT);
	}
	// ========================================= LOCAL_PORT =================================================
	
	
	// ========================================= ADDRESS =================================================
	public void addAddressCriteria(String[] values) {
		addCriteria(MATCH_ENTRY_CRITERIA_ADDRESS, values);
	}
	
	public void addAddressCriteria(String value) {
		addCriteria(MATCH_ENTRY_CRITERIA_ADDRESS, value.split(","));
	}
	
	public void pushAddressCriteria(String value) {
		pushCriteria(MATCH_ENTRY_CRITERIA_ADDRESS, value);
	}
	
	public void updateAddressCriteria(String value) {
		updateCriteria(MATCH_ENTRY_CRITERIA_ADDRESS, value);
	}
	
	public void deleteAddressCriteria() {
		deleteCriteria(MATCH_ENTRY_CRITERIA_ADDRESS);
	}
	// ========================================= ADDRESS =================================================
	
	// ========================================= RDOMAIN =================================================
	public void addRDomainCriteria(String[] values) {
		addCriteria(MATCH_ENTRY_CRITERIA_RDOMAIN, values);
	}
	
	public void addRDomainCriteria(String value) {
		addCriteria(MATCH_ENTRY_CRITERIA_RDOMAIN, value.split(","));
	}
	
	public void pushRDomainCriteria(String value) {
		pushCriteria(MATCH_ENTRY_CRITERIA_RDOMAIN, value);
	}
	
	public void updateRDomainCriteria(String value) {
		updateCriteria(MATCH_ENTRY_CRITERIA_RDOMAIN, value);
	}
	
	public void deleteRDomainCriteria() {
		deleteCriteria(MATCH_ENTRY_CRITERIA_RDOMAIN);
	}
	// ========================================= RDOMAIN =================================================
	
	
	public void parse(final String[] matchValueSplit) {
		executeWrite(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				for (int i = 0; i < matchValueSplit.length - 1; i+=2) {
					String mkey = matchValueSplit[i];
					String mvalue = matchValueSplit[i+1];
					if (MatchEntry.isNotAllowedKey(mkey)) {
						throw new IllegalStateException(String.format("Key %s not recognized for Match entry", mkey));
					}
					MatchEntry.this.matchCriteriaMap.put(mkey, new LinkedHashSet<>(Arrays.asList(mvalue.split(","))));
				}
				return null;
			}
		});
		
	}
	
	public boolean hasKey(final String key) {
		return executeRead(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				return matchCriteriaMap.containsKey(key);
			}
		});
		
	}
	
	public boolean hasUserEntry() {
		return executeRead(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				return matchCriteriaMap.containsKey(MATCH_ENTRY_CRITERIA_USER);
			}
		});
		
	}
	
	public boolean hasGroupEntry() {
		return executeRead(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				return matchCriteriaMap.containsKey(MATCH_ENTRY_CRITERIA_GROUP);
			}
		});
		
	}
	
	public boolean hasHostEntry() {
		return executeRead(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				return matchCriteriaMap.containsKey(MATCH_ENTRY_CRITERIA_HOST);
			}
		});
		
	}
	
	public boolean hasLocalAddressEntry() {
		return executeRead(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				return matchCriteriaMap.containsKey(MATCH_ENTRY_CRITERIA_LOCAL_ADDRESS);
			}
		});
		
	}
	
	public boolean hasLocalPortEntry() {
		return executeRead(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				return matchCriteriaMap.containsKey(MATCH_ENTRY_CRITERIA_LOCAL_PORT);
			}
		});
		
	}
	
	public boolean hasAddressEntry() {
		return executeRead(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				return matchCriteriaMap.containsKey(MATCH_ENTRY_CRITERIA_ADDRESS);
			}
		});
		
	}
	
	public boolean hasRDomainEntry() {
		return executeRead(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				return matchCriteriaMap.containsKey(MATCH_ENTRY_CRITERIA_RDOMAIN);
			}
		});
		
	}
	
	public static boolean isAllowedKey(String key) {
		return MATCH_ENTRY_CRITERIA_USER.equals(key) || MATCH_ENTRY_CRITERIA_GROUP.equals(key)
				|| MATCH_ENTRY_CRITERIA_HOST.equals(key) || MATCH_ENTRY_CRITERIA_LOCAL_ADDRESS.equals(key)
				|| MATCH_ENTRY_CRITERIA_LOCAL_PORT.equals(key) || MATCH_ENTRY_CRITERIA_ADDRESS.equals(key)
				|| MATCH_ENTRY_CRITERIA_RDOMAIN.equals(key);
	}
	
	public static boolean isNotAllowedKey(String key) {
		return !isAllowedKey(key);
	}

	@Override
	public String toString() {
		return "MatchEntry [matchCriteriaMap=" + matchCriteriaMap + ", keyEntries=" + keyEntries + "]";
	}
	
	public static class MatchEntryBuilder extends AbstractEntryBuilder<MatchEntryBuilder> implements EntryBuilder<MatchEntryBuilder, SshdConfigFileBuilder>{
		
		private MatchEntry managedInstance;
		private SshdConfigFileBuilder parentBuilder;
		
		public MatchEntryBuilder(SshdConfigFileBuilder parentBuilder, SshdConfigFile file, SshdConfigFileCursor cursor, boolean commentedOut) {
			this.parentBuilder = parentBuilder;
			
			this.file = file;
			this.managedInstance = this.file.addMatchEntry();
			this.managedInstance.commentedOut = commentedOut;
			this.cursor = cursor;
			this.cursor.set(this.managedInstance);
		}
		
		public MatchEntryBuilder(SshdConfigFileBuilder parentBuilder, SshdConfigFile file, SshdConfigFileCursor cursor, MatchEntry managedInstance) {
			this.managedInstance = managedInstance;
			this.parentBuilder = parentBuilder;
			
			this.file = file;
			
			this.cursor = cursor;
			this.cursor.set(this.managedInstance);
			
		}
		
		/**
		 * To add comment to a Match Criteria.
		 * <br/>
		 * <b>Note:</b> This will only add comments to file on write.
		 * On read there will be no such list of comments available.
		 * There is no definitive way to distinguish between end of Global section and start of Match section comments.
		 * 
		 * <pre>
		 * NoneEnabled no
		 * # Comment (X) HPNBufferSize 2048
		 * #Comment 1
		 * #Comment 2
		 * Match .......
		 * </pre>
		 * 
		 * There is no way to tell Comment 1 and 2 belong to Match or the Global Section.
		 * Comment could be eligible entry as X above.
		 * <br>
		 * On write the comments added would be written right before match criteria entry.
		 * 
		 * @param commentEntry
		 * @return
		 */
		public MatchEntryBuilder addMatchCriteriaComment(CommentEntry commentEntry) {
			this.managedInstance.addMatchCriteriaComment(commentEntry);
			return this;
		}
		
		public MatchEntryBuilder addUserCriteria(String[] values) {
			this.managedInstance.addUserCriteria(values);
			return this;
		}
		
		public MatchEntryBuilder addUserCriteria(String value) {
			return addUserCriteria(value.split(","));
		}
		
		public MatchEntryBuilder pushUserCriteria(String value) {
			this.managedInstance.pushUserCriteria(value);
			return this;
		}
		
		public MatchEntryBuilder updateUserCriteria(String value) {
			this.managedInstance.updateUserCriteria(value);
			return this;
		}
		
		public MatchEntryBuilder deleteUserCriteria() {
			this.managedInstance.deleteUserCriteria();
			return this;
		}
		
		public MatchEntryBuilder addGroupCriteria(String[] values) {
			this.managedInstance.addGroupCriteria(values);
			return this;
		}
		
		public MatchEntryBuilder addGroupCriteria(String value) {
			return addGroupCriteria(value.split(","));
		}
		
		public MatchEntryBuilder pushGroupCriteria(String value) {
			this.managedInstance.pushGroupCriteria(value);
			return this;
		}
		
		public MatchEntryBuilder updateGroupCriteria(String value) {
			this.managedInstance.updateGroupCriteria(value);
			return this;
		}
		
		public MatchEntryBuilder deleteGroupCriteria() {
			this.managedInstance.deleteGroupCriteria();
			return this;
		}
		
		public MatchEntryBuilder addHostCriteria(String[] values) {
			this.managedInstance.addHostCriteria(values);
			return this;
		}
		
		public MatchEntryBuilder addHostCriteria(String value) {
			return addHostCriteria(value.split(","));
		}
		
		public MatchEntryBuilder pushHostCriteria(String value) {
			this.managedInstance.pushHostCriteria(value);
			return this;
		}
		
		public MatchEntryBuilder updateHostCriteria(String value) {
			this.managedInstance.updateHostCriteria(value);
			return this;
		}
		
		public MatchEntryBuilder deleteHostCriteria() {
			this.managedInstance.deleteHostCriteria();
			return this;
		}
		
		public MatchEntryBuilder addLocalPortCriteria(String[] values) {
			this.managedInstance.addLocalPortCriteria(values);
			return this;
		}
		
		public MatchEntryBuilder addLocalPortCriteria(String value) {
			return addLocalPortCriteria(value.split(","));
		}
		
		public MatchEntryBuilder pushLocalPortCriteria(String value) {
			this.managedInstance.pushLocalPortCriteria(value);
			return this;
		}
		
		public MatchEntryBuilder updateLocalPortCriteria(String value) {
			this.managedInstance.updateLocalPortCriteria(value);
			return this;
		}
		
		public MatchEntryBuilder deleteLocalPortCriteria() {
			this.managedInstance.deleteLocalPortCriteria();
			return this;
		}
		
		public MatchEntryBuilder addLocalAddressCriteria(String[] values) {
			this.managedInstance.addLocalAddressCriteria(values);
			return this;
		}
		
		public MatchEntryBuilder addLocalAddressCriteria(String value) {
			return addLocalAddressCriteria(value.split(","));
		}
		
		public MatchEntryBuilder pushLocalAddressCriteria(String value) {
			this.managedInstance.pushLocalAddressCriteria(value);
			return this;
		}
		
		public MatchEntryBuilder updateLocalAddressCriteria(String value) {
			this.managedInstance.updateLocalAddressCriteria(value);
			return this;
		}
		
		public MatchEntryBuilder deleteLocalAddressCriteria() {
			this.managedInstance.deleteLocalAddressCriteria();
			return this;
		}
		
		public MatchEntryBuilder addAddressCriteria(String[] values) {
			this.managedInstance.addAddressCriteria(values);
			return this;
		}
		
		public MatchEntryBuilder addAddressCriteria(String value) {
			return addAddressCriteria(value.split(","));
		}
		
		public MatchEntryBuilder pushAddressCriteria(String value) {
			this.managedInstance.pushAddressCriteria(value);
			return this;
		}
		
		public MatchEntryBuilder updateAddressCriteria(String value) {
			this.managedInstance.updateAddressCriteria(value);
			return this;
		}
		
		public MatchEntryBuilder deleteAddressCriteria() {
			this.managedInstance.deleteAddressCriteria();
			return this;
		}
		
		public MatchEntryBuilder addRDomainCriteria(String[] values) {
			this.managedInstance.addRDomainCriteria(values);
			return this;
		}
		
		public MatchEntryBuilder addRDomainCriteria(String value) {
			return addRDomainCriteria(value.split(","));
		}
		
		public MatchEntryBuilder pushRDomainCriteria(String value) {
			this.managedInstance.pushRDomainCriteria(value);
			return this;
		}
		
		public MatchEntryBuilder updateRDomainCriteria(String value) {
			this.managedInstance.updateRDomainCriteria(value);
			return this;
		}
		
		public MatchEntryBuilder deleteRDomainCriteria() {
			this.managedInstance.deleteRDomainCriteria();
			return this;
		}
		
		public MatchEntryBuilder parse(String[] matchValueSplit) {
			this.managedInstance.parse(matchValueSplit);
			return this;
		}
		
		public SshdConfigFileBuilder end() {
			return this.file.executeWrite(new Callable<SshdConfigFileBuilder>() {

				@Override
				public SshdConfigFileBuilder call() throws Exception {
					MatchEntryBuilder.this.cursor.remove();
					return parentBuilder;
				}
			});
		}

		@Override
		protected Entry getManagedInstance() {
			return this.managedInstance;
		}
	}

	public String getFormattedLine() {
		if(commentedOut) {
			return String.format("#Match %s", matchEntryCriteriaAsString());
		}
		return String.format("Match %s", matchEntryCriteriaAsString());
	}
	
}
