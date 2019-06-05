package com.sshtools.common.ssh.components;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.interfaces.ECPublicKey;

import com.sshtools.common.logger.Log;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshX509PublicKey;
import com.sshtools.common.ssh.components.jce.Ssh2EcdsaSha2NistPublicKey;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;

public abstract class SshX509EcdsaSha2NistPublicKeyRfc6187 extends Ssh2EcdsaSha2NistPublicKey implements SshX509PublicKey {

	Certificate[] certs;

	public SshX509EcdsaSha2NistPublicKeyRfc6187(String name, String spec, String curve, String nistpCurve) {
		super(name, spec, curve, nistpCurve);
	}
	
	public SshX509EcdsaSha2NistPublicKeyRfc6187(ECPublicKey pk, String curve) throws IOException {
		super(pk, curve);
	}
	
	public SshX509EcdsaSha2NistPublicKeyRfc6187(Certificate[] chain, String curve) throws IOException {
		 super((ECPublicKey)chain[0].getPublicKey(), curve);
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
			
			this.pub = (ECPublicKey)certs[0].getPublicKey();
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
	
    public Certificate getCertificate() {
        return certs[0];
    }
    
    public Certificate[] getCertificateChain() {
    	return certs;
    }

	public abstract String getAlgorithm();

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
