
package com.sshtools.common.ssh.components;

import java.io.IOException;

import com.sshtools.common.ssh.SecurityLevel;

/**
 *
 * <p>Base class for all SSH protocol ciphers. The cipher
 * itself has 2 modes, encryption or decrpytion. The same method is used
 * to transporm the data depending upon the mode.</p>
 *
 * @author Lee David Painter
 */
public abstract class AbstractSshCipher implements SshCipher {

    final String algorithm;
    final SecurityLevel securityLevel;
    final int priority;
    
    public AbstractSshCipher(String algorithm, SecurityLevel securityLevel, int priority) {
        this.algorithm = algorithm;
        this.securityLevel = securityLevel;
        this.priority = priority;
    }

    @Override
	public SecurityLevel getSecurityLevel() {
    	return securityLevel;
    }
    
    @Override
	public int getPriority() {
    	return priority;
    }
    
    @Override
	public String getAlgorithm() {
        return algorithm;
    }

  /**
   * Get the cipher block size.
   *
   * @return the block size in bytes.
   */
  @Override
public abstract int getBlockSize();

  /**
   * Return the key length required
   * @return
   */
  @Override
public abstract int getKeyLength();
  
  /**
   * Initialize the cipher with up to 40 bytes of iv and key data. Each implementation
   * should take as much data from the initialization as it needs ignoring any data
   * that it does not require.
   *
   * @param mode     the mode to operate
   * @param iv       the initiaization vector
   * @param keydata  the key data
   * @throws IOException
   */
  @Override
public abstract void init(int mode, byte[] iv, byte[] keydata) throws
      IOException;

  /**
   * Transform the byte array according to the cipher mode.
   * @param data
   * @throws IOException
   */
  @Override
public void transform(byte[] data) throws IOException {
    transform(data, 0, data, 0, data.length);
  }

  /**
   * Transform the byte array according to the cipher mode; it is legal for the
   * source and destination arrays to reference the same physical array so
   * care should be taken in the transformation process to safeguard this rule.
   *
   * @param src
   * @param start
   * @param dest
   * @param offset
   * @param len
   * @throws IOException
   */
  @Override
public abstract void transform(byte[] src, int start, byte[] dest, int offset,
                                 int len) throws
      IOException;


   @Override
public boolean isMAC() {
	return false;
   }
   
   @Override
public int getMacLength() {
	   return 0;
   }


   @Override
public abstract String getProviderName();

}
