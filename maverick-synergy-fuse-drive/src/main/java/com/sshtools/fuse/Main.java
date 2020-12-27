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
package com.sshtools.fuse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.sshtools.client.PasswordAuthenticator;
import com.sshtools.client.PublicKeyAuthenticator;
import com.sshtools.client.SshClient;
import com.sshtools.client.sftp.SftpClientTask;
import com.sshtools.common.logger.Log;
import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.publickey.RsaUtils;
import com.sshtools.common.publickey.SshKeyPairGenerator;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshRsaPrivateKey;
import com.sshtools.fuse.fs.FuseSFTP;
import com.sshtools.synergy.nio.SshEngine;

public class Main {
	
	FuseSFTP fuseFs = null;
	
	public static void main(String[] args) {
		new Main().run(args);
	}
	
	public void run(String[] args) {

		System.setProperty("maverick.log.config", "conf/logging.properties");

    	
		try {
			
			if(Log.isInfoEnabled()) {
				Log.info("Parsing options");
			}
			
			Properties properties = new Properties();
			File confFile = new File("conf/drive.properties");
			if(confFile.exists()) {
				loadConfiguration(properties, confFile);
			}
			
			CommandLine cli = new DefaultParser().parse(getOptions(), args, properties);
			
			if(cli.hasOption("s")) {
				saveConfiguration(cli, properties, confFile);
			}
			
			String driveName = properties.getProperty("drive.name", "SynergyDrive");
			File mountDir = new File(properties.getProperty("drive.path", "${home}/.synergy")
					.replace("${home}", System.getProperty("user.home")));
			
			if(Log.isInfoEnabled()) {
				Log.info("Mounting {} on {}", driveName, mountDir.getAbsolutePath());
			}
			
			List<SshKeyPair> keys = new ArrayList<>();
			if(properties.containsKey("privateKey")) {
				try {
					File keyfile = new File(properties.getProperty("privateKey"));
					keys.add(SshKeyUtils.getPrivateKey(keyfile, 
							properties.getProperty("passphrase")));
				} catch (Exception e) {
					Log.error("Failed to load private key", e);
				}
			}
			
			long timeout = Long.parseLong(properties.getProperty("authentication.timeout", "30000"));
			long retryInterval = Long.parseLong(properties.getProperty("retry.interval", "5000"));
			
			boolean authenticated = false;
			
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					synchronized(Main.this) {
						umount();
					}
				}
			});
			
			do {
				try(SshClient ssh = new SshClient(properties.getProperty("hostname", "localhost"),
						Integer.parseInt(properties.getProperty("port", "22")),
								properties.getProperty("username", System.getProperty("user.name")))) {
					
					if(Log.isInfoEnabled()) {
						Log.info("Authenticating");
					}
					
//					if(ssh.getAuthenticationMethods().contains("keyboard-interactive")) {
//						ssh.authenticate(new KeyboardInteractiveAuthenticator(
//								new PasswordOverKeyboardInteractiveCallback(
//										new PasswordAuthenticator(properties.getProperty("password")))), timeout);
//					} else
						if(!ssh.isAuthenticated() && properties.containsKey("password")) {
						if(!ssh.isAuthenticated() && ssh.getAuthenticationMethods().contains("password")) {
							ssh.authenticate(new PasswordAuthenticator(properties.getProperty("password")), timeout);
						}
					}
					
					if(!ssh.isAuthenticated() && !keys.isEmpty()) {
						ssh.authenticate(new PublicKeyAuthenticator(keys.toArray(new SshKeyPair[0])), timeout);
					}

					if(Log.isInfoEnabled()) {
						Log.info("Completed Authentication");
					}
					
					if(authenticated = ssh.isAuthenticated()) {

						if(Log.isInfoEnabled()) {
							Log.info("Authenticated");
						}
						
						ssh.runTask(new SftpClientTask(ssh) {
							
							@Override
							protected void doSftp() {
								
								try(FuseSFTP fuseFs = new FuseSFTP(this)) {
									
									
									if(Log.isInfoEnabled()) {
										Log.info("Mounting virtual file system");
									}
									
									beforeMount(fuseFs);
									
									fuseFs.mount(mountDir.toPath(), 
											true, 
											Boolean.parseBoolean(properties.getProperty("debug.fuse", "false")),
											new String[] { "-o", "volname=" + driveName});
		
								} catch (SftpStatusException | IOException | SshException e) {
									Log.error("SFTP error", e);
								} finally {
									afterMount();
									ssh.disconnect();
								}
							}
						});
						
						if(ssh.isConnected()) {
							ssh.disconnect();
						}
						
						Thread.sleep(retryInterval);
					}
					
					if(Log.isInfoEnabled()) {
						Log.info("Disconnecting client");
					}
				} catch (Throwable e) {
					Log.error("Operation error", e);
				} 
			} while(authenticated);

		} catch (Throwable e) {
			Log.error("Unexpected error", e);
		} finally {
			try {
				SshEngine.getDefaultInstance().shutdownNow(false, 0L);
			} catch (IOException e) {
			}
		}
	}
	
	protected synchronized void beforeMount(FuseSFTP fuseFs) {
		this.fuseFs = fuseFs;
	}

	protected synchronized void afterMount() {
		fuseFs = null;
	}

	protected synchronized void umount() {
		if(!Objects.isNull(fuseFs)) {
			fuseFs.umount();
		}
	}

	private void loadConfiguration(Properties properties, File confFile) throws IOException {

		if(Log.isInfoEnabled()) {
			Log.info("Loading properties file");
		}

		try(InputStream in = new FileInputStream(confFile)) {
			properties.load(in);
		} 
	}

	private void saveConfiguration(CommandLine cli, Properties properties, File confFile) throws Exception {
		properties.setProperty("hostname", cli.getOptionValue("host"));
		properties.setProperty("port", cli.getOptionValue("pport", "443"));
		properties.setProperty("username", cli.getOptionValue("username"));
		
		String password = cli.getOptionValue("password");
		if(password.startsWith("!ENC!")) {
			properties.setProperty("password", password);
		} else {
			SshRsaPrivateKey privateKey = getSecretKey(confFile);
			properties.setProperty("password", "!ENC!" +
					RsaUtils.encrypt(privateKey, password));
		}

		try(FileOutputStream out = new FileOutputStream(confFile);) {
			properties.store(out, "Saved by Synergy Drive");
		} 
	}

	private SshRsaPrivateKey getSecretKey(File confFile) throws IOException, SshException, InvalidPassphraseException {
		
		File secretsKey = new File(confFile.getParent(), ".secrets");
		if(!secretsKey.exists()) {
			SshKeyUtils.createPrivateKeyFile(SshKeyPairGenerator.generateKeyPair(SshKeyPairGenerator.SSH2_RSA, 4096), null, secretsKey);
			secretsKey.setWritable(true, true);
			secretsKey.setReadable(true, true);
		}
		
		return (SshRsaPrivateKey) SshKeyUtils.getPrivateKey(secretsKey, null).getPrivateKey();
	}

	private Options getOptions() {
		Options options = new Options();
		options.addOption(Option.builder("h")
				.argName("Hostname")
				.required()
				.longOpt("hostname")
				.desc("The hostname of the server.")
				.hasArg().build());
		options.addOption(Option.builder("p")
				.argName("Port")
				.required(false)
				.longOpt("port")
				.type(Integer.class)
				.desc("The port of the server.")
				.hasArg().build());
		options.addOption(Option.builder("u")
				.argName("Username")
				.required()
				.longOpt("username")
				.desc("The username of the account onthe server.")
				.hasArg().build());
		options.addOption(Option.builder("P")
				.argName("Password")
				.required()
				.longOpt("password")
				.desc("The password of the account on the server.")
				.hasArg().build());
		options.addOption(Option.builder("s")
				.argName("Save Configuration")
				.longOpt("save")
				.desc("Save the configuration to drive.properties").build());
		return options;
	}

}
