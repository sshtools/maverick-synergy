/* HEADER */
package com.sshtools.common.publickey;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.sshtools.common.logger.Log;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshIOException;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.util.ByteArrayReader;

/**
 * Public key format factory used to decode different formats of public keys.
 * The following types of public keys are currently supported:
 * 
 * <pre>
 * OpenSSH
 * <blockquote>
 * ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAAAQQC8OZmB4d+SSMtVgsvdsCqRovgwcL/SYZunIBlR
 * mCO6LhY/8PqefhygKfIZcxyGCKcrVAO4THGbqZ/ilv8NWXJT This is a comment
 * </blockquote>
 * SECSH standard format
 * <blockquote>
 * ---- BEGIN SSH2 PUBLIC KEY ----
 * Comment: "This is a comment"
 * AAAAB3NzaC1yc2EAAAADAQABAAAAgQC9mPcvyCnWpuvN7u4cjwUkBbTqgYm5kR92XNbo7/ElAJY+
 * 7HwoTtiUsQ6Q2Ma6hUg29LlDifpX5Ujwwm5PRK+7dXWL5bbznNGxJXY5P1E/5cr/+cJueaqZuA90
 * 2x6oFweQZPK4en+nJyXFwYY/Pbf86F3EJFD3lh9RWSN7r2RbRw==
 * ---- END SSH2 PUBLIC KEY ----
 * </blockquote>
 * SSH1
 * <blockquote>
 * 1024 65537 12203618663441486180278392644721081332612879088348276482061792
 * 3981996764870633915934678786242627941442492506374351346273236223683187153
 * 1433842142721049328324552410746419300820752745317401639942167156433029893
 * 3759921689255688343334770869709776055449427739142029076904194522024626419
 * 9127925140284440450097198129
 * </blockquote>
 * </pre>
 * 
 * @author Lee David Painter
 */
public class SshPublicKeyFileFactory {

	
	
	public static final int OPENSSH_FORMAT = 0;
	public static final int SECSH_FORMAT = 1;
	public static final int SSH1_FORMAT = 2;

	/**
	 * Decode an SSH2 encoded public key as specified in the SSH2 transport
	 * protocol. This consists of a String identifier specifying the algorithm
	 * of the public key and the remaining data is formatted depending upon the
	 * public key type. The supported key types are as follows:
	 * 
	 * <pre>
	 * ssh-rsa is encoded as
	 * String        "ssh-rsa"
	 * BigInteger    e
	 * BigInteger    n
	 * 
	 * ssh-dsa is encoded as
	 * String        "ssh-dsa"
	 * BigInteger    p
	 * BigInteger    q
	 * BigItneger    g
	 * BigInteger    y
	 * </pre>
	 * 
	 * @param encoded
	 * @return SshPublicKey
	 * @throws IOException
	 */
	public static SshPublicKey decodeSSH2PublicKey(byte[] encoded)
			throws IOException {

		ByteArrayReader bar = new ByteArrayReader(encoded);
		try {

			String algorithm = bar.readString();

			try {
				SshPublicKey publickey = (SshPublicKey) ComponentManager
						.getInstance().supportedPublicKeys()
						.getInstance(algorithm);
				publickey.init(encoded, 0, encoded.length);
				return publickey;
			} catch (SshException ex) {
				throw new SshIOException(ex);
			}
		} catch (OutOfMemoryError ex2) {
			throw new IOException(
					"An error occurred parsing a public key file! Is the file corrupt?");
		} finally {
			bar.close();
		}
	}

	public static SshPublicKey decodeSSH2PublicKey(String algorithm,
			byte[] encoded) throws IOException {
		try {
			SshPublicKey publickey = (SshPublicKey) ComponentManager
					.getInstance().supportedPublicKeys().getInstance(algorithm);
			publickey.init(encoded, 0, encoded.length);
			return publickey;
		} catch (SshException ex) {
			throw new SshIOException(ex);
		}
	}

	/**
	 * Parse a formatted public key and return a file representation.
	 * 
	 * @param formattedkey
	 * @return SshPublicKeyFile
	 * @throws IOException
	 */
	public static SshPublicKeyFile parse(byte[] formattedkey)
			throws IOException {
		
		try {
			if(SECSHPublicKeyFile.isFormatted(formattedkey, SECSHPublicKeyFile.BEGIN, SECSHPublicKeyFile.END)) {
				return new SECSHPublicKeyFile(formattedkey);
			} else if(OpenSSHPublicKeyFile.isFormatted(formattedkey)) {
				return new OpenSSHPublicKeyFile(formattedkey);
			} else if(Ssh1RsaPublicKeyFile.isFormatted(formattedkey)) {
				return new Ssh1RsaPublicKeyFile(formattedkey);
			} else {
				throw new IOException("Unable to parse key, format could not be identified");
			}
		} catch (Throwable e) {
			if(e instanceof IOException) {
				throw e;
			}
			Log.error("Cannot parse public key", e);
			throw new IOException("An error occurred parsing a public key file! Is the file corrupt?");
		}
	}

	/**
	 * Parse a formatted key from an InputStream and return a file
	 * representation.
	 * 
	 * @param in
	 * @return SshPublicKeyFile
	 * @throws IOException
	 */
	public static SshPublicKeyFile parse(InputStream in) throws IOException {

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

	/**
	 * Create a file representation from an existing public key. To generate new
	 * keys see <a href="SshKeyPairGenerator.html>SshKeyPairGenerator</a>.
	 * 
	 * @param key
	 *            the public key
	 * @param comment
	 *            the comment to apply to the formatted key
	 * @param format
	 *            the format type
	 * @return SshPublicKeyFile
	 * @throws IOException
	 */
	public static SshPublicKeyFile create(SshPublicKey key, String comment,
			int format) throws IOException {
		return create(key, null, comment, format);
	}

	/**
	 * Create a file representation from an existing public key. To generate new
	 * keys see <a href="SshKeyPairGenerator.html>SshKeyPairGenerator</a>.
	 * 
	 * @param key
	 *            the public key
	 * @param options options (if supported)
	 * @param comment
	 *            the comment to apply to the formatted key
	 * @param format
	 *            the format type
	 * @return SshPublicKeyFile
	 * @throws IOException
	 */
	public static SshPublicKeyFile create(SshPublicKey key, String options, String comment,
			int format) throws IOException {
		switch (format) {
		case OPENSSH_FORMAT:
			return new OpenSSHPublicKeyFile(key, comment, options);
		case SECSH_FORMAT:
			return new SECSHPublicKeyFile(key, comment);
		case SSH1_FORMAT:
			return new Ssh1RsaPublicKeyFile(key);
		default:
			throw new IOException("Invalid format type specified!");
		}
	}

	/**
	 * Take a <a href="SshPublicKey.html">SshPublicKey</a> and write it to a
	 * file
	 * 
	 * @param key
	 * @param comment
	 * @param format
	 * @param toFile
	 * @throws IOException
	 */
	public static void createFile(SshPublicKey key, String comment, int format,
			File toFile) throws IOException {

		SshPublicKeyFile pub = create(key, comment, format);

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
	 */
	public static void convertFile(File keyFile, int toFormat, File toFile)
			throws IOException {

		SshPublicKeyFile pub = parse(new FileInputStream(keyFile));
		createFile(pub.toPublicKey(), pub.getComment(), toFormat, toFile);
	}
}
