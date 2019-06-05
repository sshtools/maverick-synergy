/* HEADER */
package com.sshtools.common.ssh.components.jce;

import java.io.IOException;

/**
 * An implementation of the AES 128 bit cipher using a JCE provider.
 *
 * @author Lee David Painter
 */
public class AES192Cbc extends AbstractJCECipher {

  public AES192Cbc() throws IOException {
    super(JCEAlgorithms.JCE_AESCBCNOPADDING, "AES", 24, "aes192-cbc");
  }

}
