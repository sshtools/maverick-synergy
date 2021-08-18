/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */

package com.sshtools.common.ssh.x509;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPublicKey;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshX509PublicKey;
import com.sshtools.common.ssh.components.jce.Ssh2RsaPublicKey;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;

public class SshX509Rsa2048Sha256Rfc6187 extends Ssh2RsaPublicKey implements SshX509PublicKey {

	 public static final String X509V3_SSH_RSA = "x509v3-rsa2048-sha256";
	
	Certificate[] certs;

	public SshX509Rsa2048Sha256Rfc6187() {
		
	}
	
	public SshX509Rsa2048Sha256Rfc6187(Certificate[] chain) {
		 super((RSAPublicKey)chain[0].getPublicKey());
		this.certs = chain;
	}
	
	public void init(byte[] blob, int start, int len) throws SshException {

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
			
			this.pubKey = (RSAPublicKey)certs[0].getPublicKey();
		} catch (CertificateException ex) {
			throw new SshException(
					"Failed to generate or read certificate from public key blob: " + ex.getMessage(),
					SshException.INTERNAL_ERROR, ex);
		} catch (IOException ex) {
			throw new SshException(
					"Failed to read public key blob; expected format "
							+ getAlgorithm(), SshException.INTERNAL_ERROR, ex);
		} finally {
			reader.close();
		}
	}
	
	public SecurityLevel getSecurityLevel() {
		return SecurityLevel.STRONG;
	}
	
    public Certificate getCertificate() {
        return certs[0];
    }
    
    public Certificate[] getCertificateChain() {
    	return certs;
    }

	public String getAlgorithm() {
		return X509V3_SSH_RSA;
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
