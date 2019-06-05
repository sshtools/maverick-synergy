/* HEADER */
package com.sshtools.common.ssh.components.jce;

import java.io.IOException;

/**
 * An implementation of the AES 128 bit cipher using a JCE provider.
 *
 * @author Lee David Painter
 */
public class AES256Cbc extends AbstractJCECipher {

  public AES256Cbc() throws IOException {
    super(JCEAlgorithms.JCE_AESCBCNOPADDING, "AES", 32, "aes256-cbc");
  }

}
