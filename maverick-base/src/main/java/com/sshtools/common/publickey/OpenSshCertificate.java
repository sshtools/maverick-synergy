/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.common.publickey;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshKeyFingerprint;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.common.util.UnsignedInteger64;

/**
 * To generate a key that supports this use
 * 
 * ssh-keygen -s ca_key -I 2 -n lee,kelly -z 12345 -O force-command=ls -O
 * source-address=192.168.82.0/24 -O no-port-forwarding user_key.pub
 * 
 * @author lee
 * 
 */
public abstract class OpenSshCertificate implements SshPublicKey {
	
	public static final int SSH_CERT_TYPE_USER = 1;
	public static final int SSH_CERT_TYPE_HOST = 2;

	public static final String PERMIT_X11_FORWARDING = "permit-x11-forwarding";
	public static final String PERMIT_PORT_FORWARDING = "permit-port-forwarding";
	public static final String PERMIT_AGENT_FORWARDING = "permit-agent-forwarding";
	public static final String PERMIT_USER_PTY = "permit-pty";
	public static final String PERMIT_USER_RC = "permit-user-rc";

	public static final String OPTION_FORCE_COMMAND = "force-command";
	public static final String OPTION_SOURCE_ADDRESS = "source-address";

	protected SshPublicKey publicKey;
	byte[] nonce;
	UnsignedInteger64 serial;
	int type;
	String keyId;
	List<String> validPrincipals = new ArrayList<String>();
	UnsignedInteger64 validAfter;
	UnsignedInteger64 validBefore;
	List<CriticalOption> criticalOptions = new ArrayList<>();
	List<CertificateExtension> extensions =new ArrayList<>();
	List<String> customExtensionsOrder = new ArrayList<>();
	
	String reserved;
	SshPublicKey signedBy;
	byte[] signature;
	
	public String getEncodingAlgorithm() {
		return getAlgorithm();
	}
	
	public boolean isUserCertificate() {
		return type == SSH_CERT_TYPE_USER;
	}
	
	public boolean isHostCertificate() {
		return type == SSH_CERT_TYPE_HOST;
	}
	
	public SshPublicKey getSignedKey() {
		return publicKey;
	}
	
	public final String getFingerprint() throws SshException {
		return SshKeyFingerprint.getFingerprint(getSignedKey().getEncoded());
	}
	
	public void init(byte[] blob, int start, int len) throws SshException {

		ByteArrayReader bar = new ByteArrayReader(blob, start, len);

		try {
			// Extract the key information
			String header = bar.readString();

			if (!header.equals(getAlgorithm())) {
				throw new SshException("The encoded key is not DSA",
						SshException.INTERNAL_ERROR);
			}

			nonce = bar.readBinaryString();

			decodePublicKey(bar);
			
			decodeCertificate(bar);
			
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new SshException(
					"Failed to obtain certificate key instance from JCE",
					SshException.INTERNAL_ERROR, ex);

		} finally {
			bar.close();
		}
	}
	
	public byte[] getEncoded() throws SshException {

		ByteArrayWriter blob = new ByteArrayWriter();

		try {

			blob.writeString(getEncodingAlgorithm());
			blob.writeBinaryString(nonce);
			
			ByteArrayReader reader = new ByteArrayReader(getSignedKey().getEncoded());
			reader.readString();
			
			blob.write(reader.array(), reader.getPosition(), reader.available());
			reader.close();
			
			encodeCertificate(blob);
			
			encodeSignature(blob);
			
			return blob.toByteArray();
		} catch (Throwable t) {
			t.printStackTrace();
			throw new SshException("Failed to encode public key",
					SshException.INTERNAL_ERROR);
		} finally {
			try {
				blob.close();
			} catch (IOException e) {
			}
		}
	}
	
	private void encodeSignature(ByteArrayWriter writer) throws IOException{
		writer.writeBinaryString(signature);
	}
	
	protected abstract void decodePublicKey(ByteArrayReader reader) throws IOException, SshException;

	protected void encodeCertificate(ByteArrayWriter writer) throws IOException, SshException {

		writer.writeUINT64(serial);
		
		writer.writeInt(type);
		
		writer.writeString(keyId);
		
		ByteArrayWriter users = new ByteArrayWriter();
		for(String user : validPrincipals) {
			users.writeString(user);
		}
		
		writer.writeBinaryString(users.toByteArray());
		users.close();
		
		writer.writeUINT64(validAfter);
		writer.writeUINT64(validBefore);
		
		ByteArrayWriter options = new ByteArrayWriter();
		List<CriticalOption> exts = filterExtensions(criticalOptions, true);
		
		for(CriticalOption e : exts) {

			options.writeString(e.getName());
			options.writeBinaryString(e.getStoredValue());
		}
		
		exts = filterExtensions(criticalOptions, false);
		
		for(CriticalOption e : exts) {

			options.writeString(e.getName());
			options.writeBinaryString(e.getStoredValue());
		}
		
		writer.writeBinaryString(options.toByteArray());
		options.close();
		
		ByteArrayWriter ext = new ByteArrayWriter();
		
		List<CertificateExtension> exts2 = filterExtensions(extensions, true);
		
		for(CertificateExtension e : exts2) {
			ext.writeString(e.getName());
			ext.writeBinaryString(e.getStoredValue());
		}
		
		if(customExtensionsOrder.size() > 0) {
			for(String key : customExtensionsOrder) {
				CertificateExtension e = getExtension(key);
				ext.writeString(e.getName());
				ext.writeBinaryString(e.getStoredValue());
			}
		} else {
			exts2 = filterExtensions(extensions, false);
			for(CertificateExtension e : exts2) {
				ext.writeString(e.getName());
				ext.writeBinaryString(e.getStoredValue());
			}
		}

		writer.writeBinaryString(ext.toByteArray());
		ext.close();
		
		writer.writeString(reserved);
		
		writer.writeBinaryString(signedBy.getEncoded());
		
	}
	
	public CertificateExtension getExtension(String key) {
		for(CertificateExtension ext : extensions) {
			if(ext.getName().equals(key)) {
				return ext;
			}
		}
		return null;
	}

	private <T extends EncodedExtension> List<T> filterExtensions(List<T> exts, boolean requireKnown) {
		
		List<T> certs = new ArrayList<>();
		for(T ext : exts) {
			if(ext.isKnown()==requireKnown) {
				certs.add(ext);
			}
		}
		Collections.sort(certs, new Comparator<T>() {

			@Override
			public int compare(T o1, T o2) {
				return o1.getName().compareTo(o2.getName());
			}
			
		});
		return certs;
	}

	protected void decodeCertificate(ByteArrayReader reader) throws IOException,
			SshException {
	
		serial = reader.readUINT64();

 		type = (int) reader.readInt();

		keyId = reader.readString();

		byte[] buf = reader.readBinaryString();

		ByteArrayReader tmp = new ByteArrayReader(buf);
		validPrincipals = new ArrayList<String>();
		while (tmp.available() > 0) {
			validPrincipals.add(tmp.readString());
		}
		tmp.close();

		validAfter = reader.readUINT64();

		validBefore = reader.readUINT64();
		tmp = new ByteArrayReader(reader.readBinaryString());

		criticalOptions.clear();
		extensions.clear();
		customExtensionsOrder.clear();
		
		while (tmp.available() > 0) {
			String name = tmp.readString();
			criticalOptions.add(CriticalOption.createKnownOption(name, tmp.readBinaryString()));
		}
		
		tmp.close();
		tmp = new ByteArrayReader(reader.readBinaryString());

		while (tmp.available() > 0) {
			String name = tmp.readString().trim();
			CertificateExtension ext = CertificateExtension.createKnownExtension(name, tmp.readBinaryString());
			if(!ext.isKnown()) {
				customExtensionsOrder.add(ext.getName());
			}
			extensions.add(ext);
		}
		tmp.close();
		reserved = reader.readString();

		signedBy = SshPublicKeyFileFactory.decodeSSH2PublicKey(reader
				.readBinaryString());

		signature = reader.readBinaryString();

		byte[] data = new byte[reader.array().length - (signature.length + 4)];
		System.arraycopy(reader.array(), 0, data, 0, data.length);

		if(!signedBy.verifySignature(signature, data)) {
			throw new SshException("Certificate file could not validate the signature supplied by the CA", SshException.JCE_ERROR);
		}
	}
	
	public void sign(SshPublicKey publicKey, UnsignedInteger64 serial, int type,
						String keyId, List<String> validPrincipals, 
						UnsignedInteger64 validAfter, UnsignedInteger64 validBefore,
						List<CriticalOption> criticalOptions,
						List<CertificateExtension> extensions,
						SshKeyPair signingKey) throws SshException {

		this.publicKey = publicKey;
		this.nonce = new byte[32];
		JCEComponentManager.getSecureRandom().nextBytes(nonce);
		this.serial = serial;
		this.type = type;
		this.keyId = keyId;
		this.validPrincipals = validPrincipals;
		this.validAfter = validAfter;
		this.validBefore = validBefore;
		this.criticalOptions = new ArrayList<>(criticalOptions);
		this.extensions = new ArrayList<>(extensions);
		this.reserved = "";
		this.signedBy = signingKey.getPublicKey();
			
		ByteArrayWriter blob = new ByteArrayWriter();

		try {

			blob.writeString(getEncodingAlgorithm());
			blob.writeBinaryString(nonce);
			
			ByteArrayReader reader = new ByteArrayReader(publicKey.getEncoded());
			try {
				reader.readString();
				
				blob.write(reader.array(), reader.getPosition(), reader.available());
			} finally {
				reader.close();
			}
			
			encodeCertificate(blob);
			byte[] encoded =  blob.toByteArray();
			
			ByteArrayWriter sig = new ByteArrayWriter();
			try {
				sig.writeString(signingKey.getPublicKey().getSigningAlgorithm());
				sig.writeBinaryString(signingKey.getPrivateKey().sign(encoded, signingKey.getPublicKey().getSigningAlgorithm()));
				this.signature = sig.toByteArray();
			} finally {
				sig.close();
			}
			
			reader = new ByteArrayReader(getEncoded());
			
			try {
				String algortihm = reader.readString();
				if(!algortihm.equals(getAlgorithm())) {
					throw new SshException(String.format(
							"Unexpected encoding error generating signed certificate [%s] [%s]", 
							algortihm, getAlgorithm()), SshException.INTERNAL_ERROR);
				}
				
				byte[] n = reader.readBinaryString();
				if(!Arrays.equals(nonce, n)) {
					throw new SshException("Unexpected encoding error generating signed certificate [nonce]", SshException.INTERNAL_ERROR);
				}
				
				decodePublicKey(reader);
				
				decodeCertificate(reader);
			} finally {
				reader.close();
			}
			
		} catch (Throwable t) {
			Log.error("Ssh certificate sign failed", t);
			t.printStackTrace();
			throw new SshException("Failed to encode public key",
					SshException.INTERNAL_ERROR);
		} finally {
			try {
				blob.close();
			} catch (IOException e) {
			}
		}
	}

	public SshPublicKey getSignedBy() {
		return signedBy;
	}

	public int getType() {
		return type;
	}

	public List<String> getPrincipals() {
		return Collections.unmodifiableList(validPrincipals);
	}

	/**
	 * 
	 * @return
	 * @deprecated Process CertificateExtension values directly.
	 */
	@Deprecated
	public List<String> getExtensions() {
		List<String> tmp = new ArrayList<>();
		for(CertificateExtension ext : extensions) {
			tmp.add(ext.getName());
		}
		return Collections.unmodifiableList(tmp);
	}
	
	public List<CriticalOption> getCriticalOptionsList() {
		return Collections.unmodifiableList(criticalOptions);
	}
	
	public List<CertificateExtension> getExtensionsList() {
		return Collections.unmodifiableList(extensions);
	}
	
	/**
	 * 
	 * @return
	 * @deprecated Process CertificateExtension values directly.
	 */
	public Map<String,String> getExtensionsMap() {
		Map<String,String> tmp = new HashMap<>();
		for(CertificateExtension ext : extensions) {
			tmp.put(ext.getName(), ext.getValue());
		}
		return Collections.unmodifiableMap(tmp);
	}

	public boolean isForceCommand() {
		return getForcedCommand()!=null;
	}

	public String getForcedCommand() {
		for(CriticalOption ext : criticalOptions) {
			if(ext.getName().equals(OPTION_FORCE_COMMAND)) {
				return ext.getStringValue();
			}
		}
		return null;
	}

	public Set<String> getSourceAddresses() {
		Set<String> tmp = new HashSet<String>();

		for(CriticalOption ext : criticalOptions) {
			if (ext.getName().equals(OPTION_SOURCE_ADDRESS)) {
				StringTokenizer t = new StringTokenizer(ext.getStringValue(), ",");
				while (t.hasMoreTokens()) {
					tmp.add(t.nextToken());
				}
			}
		}
		return Collections.unmodifiableSet(tmp);
	}
	
	public Date getValidBefore() {
		return new Date(validBefore.longValue() * 1000);
	}
	
	public Date getValidAfter() {
		return new Date(validAfter.longValue() * 1000);
	}
	
	public UnsignedInteger64 getSerial() {
		return serial;
	}
	
	public String getKeyId() {
		return keyId;
	}

	/**
	 * 
	 * @return
	 * @deprecated Process CertificateExtension values directly.
	 */
	public Map<String,String> getCriticalOptions() {
		Map<String,String> tmp = new HashMap<>();
		for(CriticalOption ext : criticalOptions) {
			tmp.put(ext.getName(), ext.getStringValue());
		}
		return Collections.unmodifiableMap(tmp);
	}
}
