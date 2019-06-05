/* HEADER */
package com.sshtools.common.ssh2;


/**
 *
 * <p>
 * This class represents a global request.
 * </p>
 * @author Lee David Painter
 */
public class GlobalRequest {

  String name;
  byte[] requestdata;

  /**
   * Contstruct a request.
   * @param name the name of the request
   * @param requestdata the request data
   */
  public GlobalRequest(String name, byte[] requestdata) {
    this.name = name;
    this.requestdata = requestdata;
  }

  /**
   * Get the name of the request.
   * @return String
   */
  public String getName() {
    return name;
  }

  /**
       * Get the request data, if the request has been sent and processed, this will
   * return the response data (which can be null).
   * @return either the request data or response data according to the current state.
   */
  public byte[] getData() {
    return requestdata;
  }

  /**
   * Set the data.
   * @param requestdata
   */
  public void setData(byte[] requestdata) {
    this.requestdata = requestdata;
  }

}