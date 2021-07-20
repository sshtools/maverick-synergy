

package com.sshtools.client;


/**
 *
 * <p>Callback interface to display authentication banner messages.</p>
 *
 * <p>In some jurisdictions sending a warning message before
 * authentication may be relevant for getting legal protection.  Many
 * UNIX machines, for example, normally display text from
 * `/etc/issue', or use "tcp wrappers" or similar software to display
 * a banner before issuing a login prompt.</p>
 *
 * <p>Implement this interface to show the authentication banner message.
 * The method should display the message and should not return until the user
 * accepts the message</p>
 * 
 */
public interface BannerDisplay {
  /**
   * Called when a banner message is received.
   * @param message the message to display.
   */
  public void displayBanner(String message);
}