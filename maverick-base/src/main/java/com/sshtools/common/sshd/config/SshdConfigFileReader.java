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
import com.sshtools.common.util.Utils;


public class SshdConfigFileReader {
	
	private InputStream stream;
	
	static Set<String> DIRECTIVES = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
	
	static {
		DIRECTIVES.add("AcceptEnv");
		DIRECTIVES.add("AddressFamily");
		DIRECTIVES.add("AllowAgentForwarding");
		DIRECTIVES.add("AllowGroups");
		DIRECTIVES.add("AllowTcpForwarding");
		DIRECTIVES.add("AllowUsers");
		DIRECTIVES.add("AuthorizedKeysFile");
		DIRECTIVES.add("Banner");
		DIRECTIVES.add("ChallengeResponseAuthentication");
		DIRECTIVES.add("ChrootDirectory");
		DIRECTIVES.add("Ciphers");
		DIRECTIVES.add("ClientAliveCountMax");
		DIRECTIVES.add("ClientAliveInterval");
		DIRECTIVES.add("Compression");
		DIRECTIVES.add("DenyGroups");
		DIRECTIVES.add("DenyUsers");
		DIRECTIVES.add("ForceCommand");
		DIRECTIVES.add("GatewayPorts");
		DIRECTIVES.add("GSSAPIAuthentication");
		DIRECTIVES.add("GSSAPIKeyExchange");
		DIRECTIVES.add("GSSAPICleanupCredentials");
		DIRECTIVES.add("GSSAPIStrictAcceptorCheck");
		DIRECTIVES.add("GSSAPIStoreCredentialsOnRekey");
		DIRECTIVES.add("HostbasedAuthentication");
		DIRECTIVES.add("HostbasedUsesNameFromPacketOnly");
		DIRECTIVES.add("HostKey");
		DIRECTIVES.add("IgnoreRhosts");
		DIRECTIVES.add("IgnoreUserKnownHosts");
		DIRECTIVES.add("KerberosAuthentication");
		DIRECTIVES.add("KerberosGetAFSToken");
		DIRECTIVES.add("KerberosOrLocalPasswd");
		DIRECTIVES.add("KerberosTicketCleanup");
		DIRECTIVES.add("KerberosUseKuserok");
		DIRECTIVES.add("KeyRegenerationInterval");
		DIRECTIVES.add("ListenAddress");
		DIRECTIVES.add("LoginGraceTime");
		DIRECTIVES.add("LogLevel");
		DIRECTIVES.add("MACs");
		DIRECTIVES.add("Match");
		DIRECTIVES.add("MaxAuthTries");
		DIRECTIVES.add("MaxSessions");
		DIRECTIVES.add("MaxStartups");
		DIRECTIVES.add("PasswordAuthentication");
		DIRECTIVES.add("PermitEmptyPasswords");
		DIRECTIVES.add("PermitOpen");
		DIRECTIVES.add("PermitRootLogin");
		DIRECTIVES.add("PermitTTY");
		DIRECTIVES.add("PermitTunnel");
		DIRECTIVES.add("PermitUserEnvironment");
		DIRECTIVES.add("PidFile");
		DIRECTIVES.add("Port");
		DIRECTIVES.add("PrintLastLog");
		DIRECTIVES.add("PrintMotd");
		DIRECTIVES.add("Protocol");
		DIRECTIVES.add("PubkeyAuthentication");
		DIRECTIVES.add("AuthorizedKeysCommand");
		DIRECTIVES.add("AuthorizedKeysCommandRunAs");
		DIRECTIVES.add("RequiredAuthentications1");
		DIRECTIVES.add("RequiredAuthentications2");
		DIRECTIVES.add("RhostsRSAAuthentication");
		DIRECTIVES.add("RSAAuthentication");
		DIRECTIVES.add("ServerKeyBits");
		DIRECTIVES.add("ShowPatchLevel");
		DIRECTIVES.add("StrictModes");
		DIRECTIVES.add("Subsystem");
		DIRECTIVES.add("SyslogFacility");
		DIRECTIVES.add("TCPKeepAlive");
		DIRECTIVES.add("UseDNS");
		DIRECTIVES.add("UseLogin");
		DIRECTIVES.add("UsePAM");
		DIRECTIVES.add("UsePrivilegeSeparation");
		DIRECTIVES.add("VersionAddendum");
		DIRECTIVES.add("X11DisplayOffset");
		DIRECTIVES.add("X11Forwarding");
		DIRECTIVES.add("X11UseLocalhost");
		DIRECTIVES.add("XAuthLocation");
	}
	
	public SshdConfigFileReader(InputStream stream) {
		this.stream = stream;
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
