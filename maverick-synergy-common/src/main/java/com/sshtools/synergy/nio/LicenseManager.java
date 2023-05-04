package com.sshtools.synergy.nio;
/**
 * Add your license using the static method provided by this class. When you
 * received your license from JADAPTIVE you would have been provided with some
 * java code which uses this class to install the license.
 *
 * @author Lee David Painter
 */
public class LicenseManager {

  /**
   * Add the license.
   * @param license
   */
  public static void addLicense(String license) {
    SshEngine.setLicense(license);
  }
}
