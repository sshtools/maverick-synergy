package com.sshtools.common.zlib;


public class OpenSSHZLibCompression
   extends ZLibCompression {

  public String getAlgorithm() {
    return "zlib@openssh.com";
  }
}
