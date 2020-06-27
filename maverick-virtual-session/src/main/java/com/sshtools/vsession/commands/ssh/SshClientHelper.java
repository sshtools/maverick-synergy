package com.sshtools.vsession.commands.ssh;

import java.io.IOException;

import com.sshtools.client.ClientAuthenticator;
import com.sshtools.client.PasswordAuthenticator;
import com.sshtools.client.PublicKeyAuthenticator;
import com.sshtools.client.SshClient;
import com.sshtools.client.SshClientContext;
import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.publickey.SshPrivateKeyFile;
import com.sshtools.common.publickey.SshPrivateKeyFileFactory;
import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshContext;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.server.vsession.VirtualConsole;

public class SshClientHelper {

	public static SshClientContext getSshContext(SshClientArguments arguments) throws IOException, SshException {
		if(CommandUtil.isNotEmpty(arguments.getSecurityLevel()) && 
				(CommandUtil.isNotEmpty(arguments.getCiphers()) || CommandUtil.isNotEmpty(arguments.getHmacs()))) {
			throw new IllegalArgumentException("Security level cannot be specified together with cipher or hmac spec.");
		}
		
		if (CommandUtil.isNotEmpty(arguments.getSecurityLevel())) {
			SecurityLevel securityLevel = SecurityLevel.valueOf(arguments.getSecurityLevel());
			return new SshClientContext(securityLevel);
		}
		
		if (CommandUtil.isNotEmpty(arguments.getCiphers()) || CommandUtil.isNotEmpty(arguments.getHmacs())) {
			return new SshClientContext(SecurityLevel.NONE);
		}
				
		return new SshClientContext();
	}
	
	public static void setUpCipherSpecs(SshClientArguments arguments, SshClientContext ctx)
			throws IOException, SshException {
		if (CommandUtil.isNotEmpty(arguments.getCiphers())) {
			String[] cipherSpecs = arguments.getCiphers();
			
			for (int i = cipherSpecs.length - 1; i >= 0; --i) {
				ctx.setPreferredCipherCS(cipherSpecs[i]); 
				ctx.setPreferredCipherSC(cipherSpecs[i]);
			}
		}
	}
	
	
	public static void setUpMacSpecs(SshClientArguments arguments, SshClientContext ctx)
			throws IOException, SshException {
		if (CommandUtil.isNotEmpty(arguments.getHmacs())) {
			String[] macSpecs = arguments.getHmacs();
			
			for (int i = macSpecs.length - 1; i >= 0; --i) {
				ctx.setPreferredMacCS(macSpecs[i]); 
				ctx.setPreferredMacSC(macSpecs[i]);
			}
		}
	}
	
	public static void setUpCompression(SshClientArguments arguments, SshClientContext ctx) 
			throws IOException, SshException {
		if (arguments.isCompression()) {
			ctx.setPreferredCompressionCS(SshContext.COMPRESSION_ZLIB);
 			ctx.setPreferredCompressionSC(SshContext.COMPRESSION_ZLIB);
		}
	}

	public static SshClient connectClient(SshClientArguments arguments, VirtualConsole console) throws IOException, SshException, PermissionDeniedException {
		

		SshClientContext ctx = getSshContext(arguments);
		
		setUpCipherSpecs(arguments, ctx);
		setUpMacSpecs(arguments, ctx);
		setUpCompression(arguments, ctx);
		
		SshClient sshClient = new SshClient(arguments.getDestination(), arguments.getPort(), arguments.getLoginName(), ctx);
		ClientAuthenticator auth;

		if (CommandUtil.isNotEmpty(arguments.getIdentityFile())) {
			
			String identityFile = arguments.getIdentityFile();
			AbstractFile identityFileTarget = console.getCurrentDirectory().resolveFile(identityFile);
			SshPrivateKeyFile pkf = SshPrivateKeyFileFactory.parse(identityFileTarget.getInputStream());
			
			String passphrase = null;
			if (pkf.isPassphraseProtected()) {
				SshKeyPair pair = null;
				for(int i=0;i<3 && sshClient.isConnected();i++) {
					try {
						passphrase = console.getLineReader().readLine("Passphrase :", '\0');
						pair = pkf.toKeyPair(passphrase);
					} catch (InvalidPassphraseException e) {
						continue;
					}
					
					auth = new PublicKeyAuthenticator(pair);

					if(!sshClient.authenticate(auth, 30000)) {
						console.println("Public key authentication failed");
					} else {
						console.println("Public key authentication succeeded");
					}
				}
			}
		} 

		if(!sshClient.isAuthenticated()) {
			do {
				auth = new PasswordAuthenticator(console.getLineReader().readLine("Password :", '*'));
				if (sshClient.authenticate(auth, 30000)) {
					break;
				}
			} while (sshClient.isConnected());
		}
		
		if(!sshClient.isAuthenticated()) {
			throw new IOException(String.format("Could not authenticate with %s", arguments.getDestination()));
		}
		return sshClient;
	}
}
