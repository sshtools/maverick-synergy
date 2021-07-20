

package com.sshtools.common.ssh.components.jce;

import java.io.IOException;

import com.sshtools.common.ssh.SecurityLevel;

/**
 * An implementation of the AES 128 bit cipher using a JCE provider.
 *
 * @author Lee David Painter
 */
public class AES128Cbc extends AbstractJCECipher {

  public AES128Cbc() throws IOException {
    super(JCEAlgorithms.JCE_AESCBCNOPADDING, "AES", 16, "aes128-cbc", SecurityLevel.WEAK, 0);
  }

}
