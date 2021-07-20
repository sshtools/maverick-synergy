

package com.sshtools.common.ssh.components.jce;

import java.io.IOException;

import com.sshtools.common.ssh.SecurityLevel;


/**
 * An implementation of the 3DES cipher using a JCE provider. 
 * 
 * @author Lee David Painter
 */
public class TripleDesCbc extends AbstractJCECipher {

  public TripleDesCbc() throws IOException {
    super(JCEAlgorithms.JCE_3DESCBCNOPADDING, "DESede", 24, "3des-cbc", SecurityLevel.WEAK, -1);
  }

}
