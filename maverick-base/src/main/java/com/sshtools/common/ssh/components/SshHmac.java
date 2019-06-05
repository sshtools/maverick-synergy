package com.sshtools.common.ssh.components;

import com.sshtools.common.ssh.SshException;

/**
 * This interface should be implemented by all message authentication
 * implementations.
 * @author Lee David Painter
 *
 */
public interface SshHmac extends SshComponent {

   /**
    * The size of the message digest output by the hmac algorithm
    * @return
    */
   public int getMacSize();
   
   /**
    * The length of the message digest output by this implementation (maybe lower than mac size);
    * @return
    */
   public int getMacLength();

   public void generate(long sequenceNo, byte[] data, int offset,
           int len, byte[] output, int start);

   public void init(byte[] keydata) throws SshException;

   public boolean verify(long sequenceNo, byte[] data, int start, int len,
           byte[] mac, int offset);
   
   public void update(byte[] b);
   
   public byte[] doFinal();
   
   public String getAlgorithm();
   
   boolean isETM();
   
}
