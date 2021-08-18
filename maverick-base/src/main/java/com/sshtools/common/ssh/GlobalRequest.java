/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */

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