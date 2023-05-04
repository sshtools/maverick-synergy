package com.sshtools.common.ssh.components.jce;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.components.SshCipherFactory;

public class ArcFour256 extends AbstractJCECipher {

	private static final String CIPHER = "arcfour256";

	public static class ArcFour256Factory implements SshCipherFactory<ArcFour256> {

		@Override
		public ArcFour256 create() throws NoSuchAlgorithmException, IOException {
			return new ArcFour256();
		}

		@Override
		public String[] getKeys() {
			return new String[] { CIPHER };
		}

		@Override
		public boolean isEnabledByDefault() {
			return false;
		}
	}
	
	public ArcFour256() throws IOException {
		super("ARCFOUR", "ARCFOUR", 32, CIPHER, SecurityLevel.WEAK, 1);	
	}

	public void init(int mode, byte[] iv, byte[] keydata) throws IOException {
	      try {

	          cipher = JCEProvider.getProviderForAlgorithm(spec)==null ?
	              	Cipher.getInstance(spec)
	              	: Cipher.getInstance(spec, JCEProvider.getProviderForAlgorithm(spec));

	          if(cipher==null) {
	              throw new IOException("Failed to create cipher engine for "
	                                    + spec);
	          }

	          // Create a byte key
	          byte[] actualKey = new byte[keylength];
	          System.arraycopy(keydata, 0, actualKey, 0, actualKey.length);

	          SecretKeySpec kspec = new SecretKeySpec(actualKey, keyspec);

	          // Create the cipher according to its algorithm
	          cipher.init(((mode == ENCRYPT_MODE) ? Cipher.ENCRYPT_MODE
	                       : Cipher.DECRYPT_MODE),
	                      kspec);
	          
	          byte[] tmp = new byte[1536];
	          cipher.update(tmp);
	          
	      } catch (NoSuchPaddingException nspe) {
	          throw new IOException("Padding type not supported");
	      } catch (NoSuchAlgorithmException nsae) {
	          throw new IOException("Algorithm not supported:"+spec);
	      } catch (InvalidKeyException ike) {
	          throw new IOException("Invalid encryption key");
	      } 
	}

	public int getBlockSize() {
		return 8;
	}
}
