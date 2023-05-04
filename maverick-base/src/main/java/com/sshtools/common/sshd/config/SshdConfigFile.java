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
	private GlobalConfiguration globalConfiguration;
	private List<MatchEntry> matchEntries = new LinkedList<>();
	private ReadWriteLock lock = new ReentrantReadWriteLock();
	private ReadLock readLock = (ReadLock) lock.readLock();
	private WriteLock writeLock = (WriteLock) lock.writeLock();
	
	public static final String AcceptEnv = "AcceptEnv";
	public static final String AddressFamily = "AddresssFamily";
	public static final String AllowAgentForwarding = "AllowAgentForwarding";
	public static final String AllowGroups = "AllowGroups";
	public static final String AllowTcpForwarding = "AllowTcpForwarding";
	public static final String AllowUsers = "AllowUsers";
	public static final String AuthorizedKeysFile = "AuthorizedKeysFile";
	public static final String Banner = "Banner";
	public static final String ChallengeResponseAuthentication = "ChallengeResponseAuthentication";
	public static final String ChrootDirectory = "ChrootDirectory";
	public static final String Ciphers = "Ciphers";
	public static final String ClientAliveCountMax = "ClientAliveCountMax";
	public static final String ClientAliveInterval = "ClientAliveInterval";
	public static final String Compression = "Compression";
	public static final String DenyGroups = "DenyGroups";
	public static final String DenyUsers = "DenyUsers";
	public static final String ForceCommand = "ForceCommand";
	public static final String GatewayPorts = "GatewayPorts";
	public static final String GSSAPIAuthentication = "GSSAPIAuthentication";
	public static final String GSSAPIKeyExchange = "GSSAPIKeyExchange";
	public static final String GSSAPICleanupCredentials = "GSSAPICleanupCredentials";
	public static final String GSSAPIStrictAcceptorCheck = "GSSAPIStrictAcceptorCheck";
	public static final String GSSAPIStoreCredentialsOnRekey = "GSSAPIStoreCredentialsOnRekey";
	public static final String HostbasedAuthentication = "HostbasedAuthentication";
	public static final String HostbasedUsesNameFromPacketOnly = "HostbasedUsesNameFromPacketOnly";
	public static final String HostKey = "HostKey";
	public static final String IgnoreRhosts = "IgnoreRhosts";
	public static final String IgnoreUserKnownHosts = "IgnoreUserKnownHosts";
	public static final String KerberosAuthentication = "KerberosAuthentication";
	public static final String KerberosGetAFSToken = "KerberosGetAFSToken";
	public static final String KerberosOrLocalPasswd = "KerberosOrLocalPasswd";
	public static final String KerberosTicketCleanup = "KerberosTicketCleanup";
	public static final String KerberosUseKuserok = "KerberosUseKuserok";
	public static final String KeyRegenerationInterval = "KeyRegenerationInterval";
	public static final String ListenAddress = "ListenAddress";
	public static final String LoginGraceTime = "LoginGraceTime";
	public static final String LogLevel = "LogLevel";
	public static final String MACs = "MACs";
	public static final String Match = "Match";
	public static final String MaxAuthTries = "MaxAuthTries";
	public static final String MaxSessions = "MaxSessions";
	public static final String MaxStartups = "MaxStartups";
	public static final String PasswordAuthentication = "PasswordAuthentication";
	public static final String PermitEmptyPasswords = "PermitEmptyPasswords";
	public static final String PermitOpen = "PermitOpen";
	public static final String PermitRootLogin = "PermitRootLogin";
	public static final String PermitTTY = "PermitTTY";
	public static final String PermitTunnel = "PermitTunnel";
	public static final String PermitUserEnvironment = "PermitUserEnvironment";
	public static final String PidFile = "PidFile";
	public static final String Port = "Port";
	public static final String PrintLastLog = "PrintLastLog";
	public static final String PrintMotd = "PrintMotd";
	public static final String Protocol = "Protocol";
	public static final String PubkeyAuthentication = "PubkeyAuthentication";
	public static final String AuthorizedKeysCommand = "AuthorizedKeysCommand";
	public static final String AuthorizedKeysCommandUser = "AuthorizedKeysCommandUser";
	public static final String AuthorizedKeysCommandRunAs = "AuthorizedKeysCommandRunAs";
	public static final String RequiredAuthentications1 = "RequiredAuthentications1";
	public static final String RequiredAuthentications2 = "RequiredAuthentications2";
	public static final String RhostsRSAAuthentication = "RhostsRSAAuthentication";
	public static final String RSAAuthentication = "RSAAuthentication";
	public static final String ServerKeyBits = "ServerKeyBits";
	public static final String ShowPatchLevel = "ShowPatchLevel";
	public static final String StrictModes = "StrictModes";
	public static final String Subsystem = "Subsystem";
	public static final String SyslogFacility = "SyslogFacility";
	public static final String TCPKeepAlive = "TCPKeepAlive";
	public static final String UseDNS = "UseDNS";
	public static final String UseLogin = "UseLogin";
	public static final String UsePAM = "UsePAM";
	public static final String UsePrivilegeSeparation = "UsePrivilegeSeparation";
	public static final String VersionAddendum = "VersionAddendum";
	public static final String X11DisplayOffset = "X11DisplayOffset";
	public static final String X11Forwarding = "X11Forwarding";
	public static final String X11UseLocalhost = "X11UseLocalhost";
	public static final String XAuthLocation = "XAuthLocation";
	
	public SshdConfigFile() {
		this.globalConfiguration = new GlobalConfiguration(this);
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
	
	public GlobalConfiguration getGlobalConfiguration() {
		return this.globalConfiguration;
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
		private GlobalConfiguration managedInstance;
		
		public SshdConfigFileBuilder() {
			this.file = new SshdConfigFile();
			this.managedInstance = this.file.globalConfiguration;
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


