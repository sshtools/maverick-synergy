package com.sshtools.common.ssh;

/*-
 * #%L
 * Base API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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
