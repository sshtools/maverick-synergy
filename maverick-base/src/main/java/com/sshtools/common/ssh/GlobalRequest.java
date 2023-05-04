package com.sshtools.common.ssh;

/**
 * This class represents a global request.
 */
public class GlobalRequest extends AbstractRequestFuture {

  String name;
  SshConnection con;
  byte[] requestdata;

  /**
   * Contstruct a request.
   * @param name the name of the request
   * @param the connection of the request
   * @param requestdata the request data
   */
  public GlobalRequest(String name, SshConnection con, byte[] requestdata) {
	    this.name = name;
	    this.con = con;
	    this.requestdata = requestdata;
  }


  /**
   * Get the name of the request.
   * @return String
   */
  public String getName() {
    return name;
  }
  
  public SshConnection getConnection() {
	  return con;
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

  public void complete(boolean success) {
	  done(success);
  }
}