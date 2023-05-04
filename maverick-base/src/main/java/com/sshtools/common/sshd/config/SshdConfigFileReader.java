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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import com.sshtools.common.sshd.config.SshdConfigFile.SshdConfigFileBuilder;
import com.sshtools.common.util.IOUtils;
import com.sshtools.common.util.Utils;


public class SshdConfigFileReader {
	
	public static final String ACCEPT_ENV = "AcceptEnv";	
	public static final String ALLOW_AGENT_FORWARDING = "AllowAgentForwarding";
	public static final String ADDRESS_FAMILY = "AddressFamily";

	public static final String PASSWORD_AUTHENTICATION = "PasswordAuthentication";
	

	private InputStream stream;
	
	static Set<String> DIRECTIVES = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
	
	static {
		DIRECTIVES.add(SshdConfigFile.AcceptEnv);
		DIRECTIVES.add(SshdConfigFile.AddressFamily);
		DIRECTIVES.add(SshdConfigFile.AllowAgentForwarding);
		DIRECTIVES.add(SshdConfigFile.AllowGroups);
		DIRECTIVES.add(SshdConfigFile.AllowTcpForwarding);
		DIRECTIVES.add(SshdConfigFile.AllowUsers);
		DIRECTIVES.add(SshdConfigFile.AuthorizedKeysFile);
		DIRECTIVES.add(SshdConfigFile.Banner);
		DIRECTIVES.add(SshdConfigFile.ChallengeResponseAuthentication);
		DIRECTIVES.add(SshdConfigFile.ChrootDirectory);
		DIRECTIVES.add(SshdConfigFile.Ciphers);
		DIRECTIVES.add(SshdConfigFile.ClientAliveCountMax);
		DIRECTIVES.add(SshdConfigFile.ClientAliveInterval);
		DIRECTIVES.add(SshdConfigFile.Compression);
		DIRECTIVES.add(SshdConfigFile.DenyGroups);
		DIRECTIVES.add(SshdConfigFile.DenyUsers);
		DIRECTIVES.add(SshdConfigFile.ForceCommand);
		DIRECTIVES.add(SshdConfigFile.GatewayPorts);
		DIRECTIVES.add(SshdConfigFile.GSSAPIAuthentication);
		DIRECTIVES.add(SshdConfigFile.GSSAPIKeyExchange);
		DIRECTIVES.add(SshdConfigFile.GSSAPICleanupCredentials);
		DIRECTIVES.add(SshdConfigFile.GSSAPIStrictAcceptorCheck);
		DIRECTIVES.add(SshdConfigFile.GSSAPIStoreCredentialsOnRekey);
		DIRECTIVES.add(SshdConfigFile.HostbasedAuthentication);
		DIRECTIVES.add(SshdConfigFile.HostbasedUsesNameFromPacketOnly);
		DIRECTIVES.add(SshdConfigFile.HostKey);
		DIRECTIVES.add(SshdConfigFile.IgnoreRhosts);
		DIRECTIVES.add(SshdConfigFile.IgnoreUserKnownHosts);
		DIRECTIVES.add(SshdConfigFile.KerberosAuthentication);
		DIRECTIVES.add(SshdConfigFile.KerberosGetAFSToken);
		DIRECTIVES.add(SshdConfigFile.KerberosOrLocalPasswd);
		DIRECTIVES.add(SshdConfigFile.KerberosTicketCleanup);
		DIRECTIVES.add(SshdConfigFile.KerberosUseKuserok);
		DIRECTIVES.add(SshdConfigFile.KeyRegenerationInterval);
		DIRECTIVES.add(SshdConfigFile.ListenAddress);
		DIRECTIVES.add(SshdConfigFile.LoginGraceTime);
		DIRECTIVES.add(SshdConfigFile.LogLevel);
		DIRECTIVES.add(SshdConfigFile.MACs);
		DIRECTIVES.add(SshdConfigFile.Match);
		DIRECTIVES.add(SshdConfigFile.MaxAuthTries);
		DIRECTIVES.add(SshdConfigFile.MaxSessions);
		DIRECTIVES.add(SshdConfigFile.MaxStartups);
		DIRECTIVES.add(SshdConfigFile.PasswordAuthentication);
		DIRECTIVES.add(SshdConfigFile.PermitEmptyPasswords);
		DIRECTIVES.add(SshdConfigFile.PermitOpen);
		DIRECTIVES.add(SshdConfigFile.PermitRootLogin);
		DIRECTIVES.add(SshdConfigFile.PermitTTY);
		DIRECTIVES.add(SshdConfigFile.PermitTunnel);
		DIRECTIVES.add(SshdConfigFile.PermitUserEnvironment);
		DIRECTIVES.add(SshdConfigFile.PidFile);
		DIRECTIVES.add(SshdConfigFile.Port);
		DIRECTIVES.add(SshdConfigFile.PrintLastLog);
		DIRECTIVES.add(SshdConfigFile.PrintMotd);
		DIRECTIVES.add(SshdConfigFile.Protocol);
		DIRECTIVES.add(SshdConfigFile.PubkeyAuthentication);
		DIRECTIVES.add(SshdConfigFile.AuthorizedKeysCommand);
		DIRECTIVES.add(SshdConfigFile.AuthorizedKeysCommandUser);
		DIRECTIVES.add(SshdConfigFile.AuthorizedKeysCommandRunAs);
		DIRECTIVES.add(SshdConfigFile.RequiredAuthentications1);
		DIRECTIVES.add(SshdConfigFile.RequiredAuthentications2);
		DIRECTIVES.add(SshdConfigFile.RhostsRSAAuthentication);
		DIRECTIVES.add(SshdConfigFile.RSAAuthentication);
		DIRECTIVES.add(SshdConfigFile.ServerKeyBits);
		DIRECTIVES.add(SshdConfigFile.ShowPatchLevel);
		DIRECTIVES.add(SshdConfigFile.StrictModes);
		DIRECTIVES.add(SshdConfigFile.Subsystem);
		DIRECTIVES.add(SshdConfigFile.SyslogFacility);
		DIRECTIVES.add(SshdConfigFile.TCPKeepAlive);
		DIRECTIVES.add(SshdConfigFile.UseDNS);
		DIRECTIVES.add(SshdConfigFile.UseLogin);
		DIRECTIVES.add(SshdConfigFile.UsePAM);
		DIRECTIVES.add(SshdConfigFile.UsePrivilegeSeparation);
		DIRECTIVES.add(SshdConfigFile.VersionAddendum);
		DIRECTIVES.add(SshdConfigFile.X11DisplayOffset);
		DIRECTIVES.add(SshdConfigFile.X11Forwarding);
		DIRECTIVES.add(SshdConfigFile.X11UseLocalhost);
		DIRECTIVES.add(SshdConfigFile.XAuthLocation);
	}
	
	public SshdConfigFileReader(InputStream stream) {
		this.stream = stream;
	}
	
	public SshdConfigFileReader(String config) throws IOException {
		this.stream = IOUtils.toInputStream(config, "UTF-8");
	}

	public SshdConfigFile read() throws IOException {
		return readToBuilder().build();
	}
	
	public SshdConfigFileBuilder readToBuilder() throws IOException {
		final SshdConfigFileBuilder sshdConfigFileBuilder = SshdConfigFile.builder();
		
		return sshdConfigFileBuilder.executeWrite(new Callable<SshdConfigFileBuilder>() {

			@Override
			public SshdConfigFileBuilder call() throws Exception {
				EntryBuilder<?, ?> currentBuilder = sshdConfigFileBuilder;
				
				if (SshdConfigFileReader.this.stream == null) {
					throw new IllegalStateException("Stream not initiallized.");
				}
				
				try (BufferedReader br = new BufferedReader(new InputStreamReader(SshdConfigFileReader.this.stream))) {
					String line = null;
					boolean indented = false;
					
					while((line = br.readLine()) != null) {
						
						line = line.trim();
						if (line.equals("")) {
							currentBuilder.cursor().get().appendEntry(new BlankEntry());
							continue;
						}
						
						boolean commentedDirective = false;
						if(line.startsWith("#")) {
							line = line.substring(1).trim();
							if(!isKnownDirective(line)) {
								currentBuilder.cursor().get().appendEntry(new CommentEntry(line));
								continue;
							}
							commentedDirective = true;
							
						}
						
						
						String[] result = line.split("\\s");

						if (result.length == 0 || result.length == 1) {
							onInvalidEntry(line);
							continue;
						}
						
						String key = result[0];
						String value = Utils.csv(" ", Arrays.copyOfRange(result, 1, result.length));
						if (key.equalsIgnoreCase("match")) {
							
							if (currentBuilder.cursor().get() instanceof MatchEntry) {
								indented = false;
								currentBuilder.end();
							}
							
							String[] matchValueSplit = value.split("\\s");
							
							currentBuilder = sshdConfigFileBuilder
								.matchEntry(commentedDirective)
								.parse(matchValueSplit);
							
							indented = true;
							continue;
						}
						
						currentBuilder.cursor().get().appendEntry(new SshdConfigKeyValueEntry(key, value, commentedDirective, indented));
					}
				}
				
				return sshdConfigFileBuilder;
			}

			private boolean isKnownDirective(String comment) {
				String[] result = comment.split("\\s");
				return DIRECTIVES.contains(result[0]);
			}
		});
		
		
	}
	
	void onInvalidEntry(String entry)  {
		// Do nothing
	}
}
