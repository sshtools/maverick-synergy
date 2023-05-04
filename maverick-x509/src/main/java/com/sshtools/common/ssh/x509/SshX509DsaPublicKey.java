package com.sshtools.common.ssh.x509;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.interfaces.DSAPublicKey;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKeyFactory;
import com.sshtools.common.ssh.components.SshX509PublicKey;
import com.sshtools.common.ssh.components.jce.JCEAlgorithms;
import com.sshtools.common.ssh.components.jce.JCEProvider;
import com.sshtools.common.ssh.components.jce.Ssh2DsaPublicKey;

/**
 * Basic implementation of X509 certificate support.
 *
 * @author not attributable
 */
public class SshX509DsaPublicKey extends Ssh2DsaPublicKey implements SshX509PublicKey {

    public static final String X509V3_SIGN_DSA = "x509v3-sign-dss";
    Certificate cert;
	
	public static class SshX509DsaPublicKeyFactory implements SshPublicKeyFactory<SshX509DsaPublicKey> {

		@Override
		public SshX509DsaPublicKey create() throws NoSuchAlgorithmException, IOException {
			return new SshX509DsaPublicKey();
		}

		@Override
		public String[] getKeys() {
			return new String[] {  X509V3_SIGN_DSA };
		}
	}

    public SshX509DsaPublicKey() {
    }

    public SshX509DsaPublicKey(Certificate cert) {
            super((DSAPublicKey)cert.getPublicKey());
            this.cert = cert;
    }

    /**
     * Get the algorithm name for the public key.
     *
     * @return the algorithm name, for example "ssh-dss"
     * @todo Implement this com.maverick.ssh.SshPublicKey method
     */
    public String getAlgorithm() {
        return X509V3_SIGN_DSA;
    }
    
    
    public String getSigningAlgorithm() {
    	return getAlgorithm();
    }

    /**
     * Encode the public key into a blob of binary data, the encoded result
     * will be passed into init to recreate the key.
     *
     * @return an encoded byte array
     * @throws SshException
     * @todo Implement this com.maverick.ssh.SshPublicKey method
     */
    public byte[] getEncoded() throws SshException {
        
    	try {
			return cert.getEncoded();
		} catch (Throwable ex) {
			throw new SshException("Failed to encoded key data",
					SshException.INTERNAL_ERROR, ex);
		}
    }

    /**
     * Initialize the public key from a blob of binary data.
     *
     * @param blob byte[]
     * @param start int
     * @param len int
     * @throws SshException
     * @todo Implement this com.maverick.ssh.SshPublicKey method
     */
    public void init(byte[] blob, int start, int len) throws SshException {

        try {
            
			
        	ByteArrayInputStream is = new ByteArrayInputStream(blob, start, len);

             CertificateFactory cf = JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_X509)==null ? 
            		 CertificateFactory.getInstance(JCEAlgorithms.JCE_X509) : 
            		 CertificateFactory.getInstance(JCEAlgorithms.JCE_X509, JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_X509));
            		 
             this.cert = cf.generateCertificate(is);

             if (!(cert.getPublicKey() instanceof DSAPublicKey ) )
                throw new SshException("Certificate public key is not an DSA public key!", SshException.BAD_API_USAGE);

             this.pubkey = (DSAPublicKey)cert.getPublicKey();

         } catch (Throwable ex) {
             throw new SshException(ex.getMessage(), SshException.JCE_ERROR, ex);
         }
    }

    public Certificate getCertificate() {
        return cert;
    }
    
    public Certificate[] getCertificateChain() {
    	return new Certificate[] { cert};
    }

}