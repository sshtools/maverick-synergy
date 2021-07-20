

package com.sshtools.common.ssh.components.jce;

import java.io.IOException;

import com.sshtools.common.ssh.SecurityLevel;


/**
 * An implementation of the Blowfish cipher using a JCE provider. If you
 * have enabled JCE usage there is no need to configure this separately.
 * @author Lee David Painter
 */
public class BlowfishCbc extends AbstractJCECipher {

  public BlowfishCbc() throws IOException {
    super(JCEAlgorithms.JCE_BLOWFISHCBCNOPADDING, "Blowfish", 16, "blowfish-cbc", SecurityLevel.WEAK, 0);
  }

}
