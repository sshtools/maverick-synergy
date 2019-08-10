package com.sshtools.fuse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.sshtools.client.SshClient;
import com.sshtools.client.sftp.SftpClientTask;
import com.sshtools.common.logger.Log;
import com.sshtools.common.nio.SshEngine;
import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.publickey.RsaUtils;
import com.sshtools.common.publickey.SshKeyPairGenerator;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshRsaPrivateKey;
import com.sshtools.fuse.fs.FuseSFTP;

public class Main {

	public static void main(String[] args) {
		new Main().run(args);
	}
	
	public void run(String[] args) {

		System.setProperty("maverick.log.config", "conf/logging.properties");
		
    	File mountDir = new File(System.getProperty("user.home"), ".synergy");
    	
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
			
			if(Log.isInfoEnabled()) {
				Log.info("Creating virtual file system");
			}
			
			try(SshClient ssh = new SshClient(properties.getProperty("hostname", "localhost"),
					Integer.parseInt(properties.getProperty("port", "22")),
							properties.getProperty("username", System.getProperty("user.name")),
							properties.getProperty("password", "").toCharArray())) {
				
				ssh.runTask(new SftpClientTask(ssh) {
					
					@Override
					protected void doSftp() {
						
						try(FuseSFTP memfs = new FuseSFTP(this)) {
							
							
							if(Log.isInfoEnabled()) {
								Log.info("Mounting virtual file system");
							}
							
							Runtime.getRuntime().addShutdownHook(new Thread() {
								public void run() {
									synchronized(Main.this) {
										memfs.umount();
									}
								}
							});
							
							memfs.mount(mountDir.toPath(), 
									true, 
									Boolean.parseBoolean(properties.getProperty("debugFuse", "false")),
									new String[] { "-o", "volname=SynergyDrive"});

						} catch (SftpStatusException | IOException | SshException e) {
							Log.error("SFTP error", e);
						} finally {
							ssh.disconnect();
						}
					}
				});
			}

		} catch (Throwable e) {
			Log.error("Unexpected error", e);
		} finally {
			try {
				SshEngine.getDefaultInstance().shutdownNow(false, 0L);
			} catch (IOException e) {
			}
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
