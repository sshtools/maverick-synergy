/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
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