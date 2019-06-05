/* HEADER */
package com.sshtools.common.ssh;

/**
 * Interface for receiving {@link Channel} events (currently only supports the
 * close event).
 *
 * @author Lee David Painter
 */
public interface ChannelEventListener {

  /**
   * The channel has been opened.
   * @param channel
   */
  public void onChannelOpen(Channel channel);

  /**
   * The channel has been closed
   * @param channel
   */
  public void onChannelClose(Channel channel);

  /**
   * The channel has received an EOF from the remote client
   * @param channel
   */
  public void onChannelEOF(Channel channel);


  /**
   * The channel is closing, but has not sent its SSH_MSG_CHANNEL_CLOSE
   * @param channel Channel
   */
  public void onChannelClosing(Channel channel);

  /**
   * When the remote side adjusts its window.
   * @param channel
   */
  public void onWindowAdjust(Channel channel, long currentWindowSpace);
}
