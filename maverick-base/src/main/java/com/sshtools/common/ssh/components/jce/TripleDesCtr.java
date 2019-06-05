/* HEADER */
package com.sshtools.common.ssh.components.jce;

import java.io.IOException;


/**
 * An implementation of the 3DES cipher using a JCE provider. 
 * 
 * @author Lee David Painter
 */
public class TripleDesCtr extends AbstractJCECipher {

  public TripleDesCtr() throws IOException {
    super(JCEAlgorithms.JCE_3DESCTRNOPADDING, "DESede", 24, "3des-ctr");
  }

}
