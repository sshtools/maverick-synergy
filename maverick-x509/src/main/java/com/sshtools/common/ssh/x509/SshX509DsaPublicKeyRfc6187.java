package com.sshtools.common.ssh.x509;

/*-
 * #%L
 * X509 Certificate Support
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.interfaces.DSAPublicKey;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.SshPublicKeyFactory;
import com.sshtools.common.ssh.components.SshX509PublicKey;
import com.sshtools.common.ssh.components.jce.Ssh2DsaPublicKey;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;

public class SshX509DsaPublicKeyRfc6187 extends Ssh2DsaPublicKey implements SshX509PublicKey {

	 public static final String X509V3_SSH_DSS = "x509v3-ssh-dss";
	 
	public static class SshX509DsaPublicKeyRfc6187Factory implements SshPublicKeyFactory<SshX509DsaPublicKeyRfc6187> {

		@Override
		public SshX509DsaPublicKeyRfc6187 create() throws NoSuchAlgorithmException, IOException {
			return new SshX509DsaPublicKeyRfc6187();
		}

		@Override
		public String[] getKeys() {
			return new String[] {  X509V3_SSH_DSS };
		}
	}
		
	Certificate[] certs;

	public SshX509DsaPublicKeyRfc6187() {
		
	}
	
	public SshX509DsaPublicKeyRfc6187(Certificate[] chain) {
		 super((DSAPublicKey)chain[0].getPublicKey());
		this.certs = chain;
	}
	
	public SshPublicKey init(byte[] blob, int start, int len) throws SshException {

		ByteArrayReader reader = new ByteArrayReader(blob, start, len);

		try {

			String alg = reader.readString();
			if (!alg.equals(getAlgorithm())) {
				throw new SshException("Public key blob is not a "
						+ getAlgorithm() + " formatted key [" + alg + "]",
						SshException.BAD_API_USAGE);
			}

			int certificateCount = (int) reader.readInt();

			if(Log.isDebugEnabled()) {
				Log.debug("Expecting chain of " + certificateCount);
			}
			
			if(certificateCount <= 0) {
				throw new SshException( 
						"There are no certificats present in the public key blob",
						SshException.POSSIBLE_CORRUPT_FILE);
			}
			
			this.certs = new Certificate[certificateCount];
			
			for(int i=0;i<certificateCount;i++) {
				byte[] certBlob = reader.readBinaryString();
				CertificateFactory certFactory = CertificateFactory
						.getInstance("X.509");
				certs[i] = certFactory.generateCertificate(new ByteArrayInputStream(certBlob));
			}
			
			this.pubkey = (DSAPublicKey)certs[0].getPublicKey();
		} catch (CertificateException ex) {
			throw new SshException(
					"Failed to generate or read certificate from public key blob",
					SshException.INTERNAL_ERROR, ex);
		} catch (IOException ex) {
			throw new SshException(
					"Failed to read public key blob; expected format "
							+ getAlgorithm(), SshException.INTERNAL_ERROR, ex);
		} finally {
			reader.close();
		}

		return this;
	}
	
    public Certificate getCertificate() {
        return certs[0];
    }
    
    public Certificate[] getCertificateChain() {
    	return certs;
    }

	public String getAlgorithm() {
		return X509V3_SSH_DSS;
	}

	public byte[] getEncoded() throws SshException {
		
		ByteArrayWriter writer = new ByteArrayWriter();
		
		try {
			writer.writeString(getAlgorithm());
			writer.writeInt(certs.length);

			for(Certificate c : certs) {
				writer.writeBinaryString(c.getEncoded());
			}
			
			// No OCSP responses
			writer.writeInt(0);
			
			return writer.toByteArray();
		} catch (CertificateEncodingException e) {
			throw new SshException("Failed to encode certificate chain", SshException.INTERNAL_ERROR, e);
		} catch (IOException e) {
			throw new SshException("Failed to write certificate chain", SshException.INTERNAL_ERROR, e);
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
			}
		}
		
	}
}
