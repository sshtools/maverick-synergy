/* HEADER */
package com.sshtools.common.publickey;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.sshtools.common.logger.Log;

import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshRsaPrivateCrtKey;
import com.sshtools.common.ssh.components.jce.JCEProvider;

/**
 * Private key format factory used to decode private key files. This factory
 * currently supports SSHTools, OpenSSH and SSH1 encrypted private keys.
 * 
 * @author Lee David Painter
 */
public class SshPrivateKeyFileFactory {

	
	
	public static final int OPENSSH_FORMAT = 0;
	public static final int SSHTOOLS_FORMAT = 1;
	public static final int SSH1_FORMAT = 3;

	/**
	 * Parse formatted data and return a suitable <a
	 * href="SshPrivateKeyFile.html">SshPrivateKeyFile</a> implementation.
	 * 
	 * @param formattedkey
	 * @return SshPrivateKeyFile
	 * @throws IOException
	 */
	public static SshPrivateKeyFile parse(byte[] formattedkey)
			throws IOException {

		if(JCEProvider.hasBCProvider() && JCEProvider.isBCEnabled()) {
			try {
					@SuppressWarnings("unchecked")
					Class<SshPrivateKeyFile> clz = (Class<SshPrivateKeyFile>) Class.forName("com.sshtools.publickey.OpenSSHPrivateKeyFile" + JCEProvider.getBCProvider().getName());
					
					Method is = clz.getMethod("isFormatted", byte[].class);
					
					Boolean result = (Boolean) is.invoke(null, formattedkey);
					
					if(result) {
						Constructor<SshPrivateKeyFile> c = clz.getDeclaredConstructor(byte[].class);
						c.setAccessible(true);
						return c.newInstance(formattedkey);
					}
	
			} catch(InvocationTargetException e) { 
				if(Boolean.getBoolean("maverick.verbose")) {
					Log.warn("OpenSSHPrivateKeyFile could not load using Bouncycastle PKIX", e.getTargetException());
				} 
			} catch(Throwable t) {
				if(Boolean.getBoolean("maverick.verbose")) {
					Log.warn("Bouncycastle PKIX not in classpath so falling back to older implementation of OpenSSHPrivateKeyFile.", t);
				} 
			}
		}	
		try {			
			if (OpenSSHPrivateKeyFile.isFormatted(formattedkey)) {
				return new OpenSSHPrivateKeyFile(formattedkey);
			} else if (Base64EncodedFileFormat.isFormatted(formattedkey,
					SshtoolsPrivateKeyFile.BEGIN, SshtoolsPrivateKeyFile.END)) {
				return new SshtoolsPrivateKeyFile(formattedkey);
			} else if (Ssh1RsaPrivateKeyFile.isFormatted(formattedkey)) {
				return new Ssh1RsaPrivateKeyFile(formattedkey);
			} else if (PuTTYPrivateKeyFile.isFormatted(formattedkey)) {
				return new PuTTYPrivateKeyFile(formattedkey);
			} else if (SSHCOMPrivateKeyFile.isFormatted(formattedkey)) {
				return new SSHCOMPrivateKeyFile(formattedkey);
			} else {
				throw new IOException(
						"A suitable key format could not be found!");
			}
		} catch (OutOfMemoryError ex) {
			throw new IOException(
					"An error occurred parsing a private key file! Is the file corrupt?");
		}

	}

	/**
	 * Parse an InputStream and return a suitable <a
	 * href="SshPrivateKeyFile.html">SshPrivateKeyFile</a> implementation.
	 * 
	 * @param in
	 * @return SshPrivateKeyFile
	 * @throws IOException
	 */
	public static SshPrivateKeyFile parse(InputStream in) throws IOException {

		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			int read;
			while ((read = in.read()) > -1) {
				out.write(read);
			}
			return parse(out.toByteArray());
		} finally {
			try {
				in.close();
			} catch (IOException ex) {
			}
		}

	}

	public static SshPrivateKeyFile create(SshKeyPair pair, String passphrase,
			String comment) throws IOException {
		return create(pair, passphrase, comment, SshPrivateKeyFileFactory.OPENSSH_FORMAT);
	}
	
	public static SshPrivateKeyFile create(SshKeyPair pair, String passphrase,
			String comment, int format) throws IOException {

		if (!(pair.getPrivateKey() instanceof SshRsaPrivateCrtKey)
				&& format == SSH1_FORMAT) {
			throw new IOException("SSH1 format requires rsa key pair!");
		}

		switch (format) {
		case OPENSSH_FORMAT:
			if(JCEProvider.isBCEnabled()) {
				try {
					/**
					 * Try BouncyCastle based PEM / OpenSSH else failover to
					 * previous implementation
					 */
					@SuppressWarnings("unchecked")
					Class<SshPrivateKeyFile> clz = (Class<SshPrivateKeyFile>) Class.forName("com.sshtools.publickey.OpenSSHPrivateKeyFile" + JCEProvider.getBCProvider().getName());
					
					Constructor<SshPrivateKeyFile> c = clz.getDeclaredConstructor(SshKeyPair.class, String.class);
					c.setAccessible(true);
					SshPrivateKeyFile f = c.newInstance(pair, passphrase);
					f.toKeyPair(passphrase);
					return f;
				} catch(Throwable t) {
				}
			}
			return new OpenSSHPrivateKeyFile(pair, passphrase);
		case SSHTOOLS_FORMAT:
			return new SshtoolsPrivateKeyFile(pair, passphrase, comment);
		case SSH1_FORMAT:
			return new Ssh1RsaPrivateKeyFile(pair, passphrase, comment);
		default:
			throw new IOException("Invalid key format!");
		}

	}

	public static void createFile(SshKeyPair key, String passphrase,
			String comment, File toFile) throws IOException {
		createFile(key, passphrase, comment, OPENSSH_FORMAT, toFile);
	}
	
	/**
	 * Take a <a href="SshPrivateKey.html">SshPrivateKey</a> and write it to a
	 * file.
	 * 
	 * @param key
	 * @param comment
	 * @param format
	 * @param toFile
	 * @throws IOException
	 */
	public static void createFile(SshKeyPair key, String passphrase,
			String comment, int format, File toFile) throws IOException {

		SshPrivateKeyFile pub = create(key, passphrase, comment, format);

		FileOutputStream out = new FileOutputStream(toFile);

		try {
			out.write(pub.getFormattedKey());
			out.flush();
		} finally {
			out.close();
		}
	}

	/**
	 * Take a file in any of the supported public key formats and convert to the
	 * requested format.
	 * 
	 * @param keyFile
	 * @param toFormat
	 * @param toFile
	 * @throws IOException
	 * @throws InvalidPassphraseException
	 */
	public static void convertFile(File keyFile, String passphrase,
			String comment, int toFormat, File toFile) throws IOException,
			InvalidPassphraseException {

		SshPrivateKeyFile pub = parse(new FileInputStream(keyFile));

		createFile(pub.toKeyPair(passphrase), passphrase, comment, toFormat,
				toFile);
	}
	
	/**
	 * Take a file in any of the supported public key formats and convert to the
	 * requested format.
	 * 
	 * @param keyFile
	 * @param toFormat
	 * @param toFile
	 * @throws IOException
	 * @throws InvalidPassphraseException
	 */
	public static void changePassphrase(File keyFile, String passphrase,
			String newPassphrase) throws IOException,
			InvalidPassphraseException {

		SshPrivateKeyFile pub = parse(new FileInputStream(keyFile));

		pub.changePassphrase(passphrase, newPassphrase);
		
		FileOutputStream out = new FileOutputStream(keyFile);

		try {
			out.write(pub.getFormattedKey());
			out.flush();
		} finally {
			out.close();
		}
	}
}
