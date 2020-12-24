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
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.synergy.common.nio;

import java.io.IOException;
import java.nio.channels.SelectionKey;

/**
 * An abstract class for the NIO framework to accept client connections.
 */
public abstract class ClientAcceptor {

	  ListeningInterface li;

	  /**
	   * Construct an acceptor with a protocol context.
	   * @param protocolContext ProtocolContext
	   */
	  public ClientAcceptor(ListeningInterface li) {
	      this.li = li;
	  }

	  /**
	   * Called by the framework when the OP_ACCEPT event is fired for this
	   * acceptor.
	   *
	   * @param key SelectionKey
	   * @return boolean
	   */
	  public void finishAccept(SelectionKey key) {
	      if(finishAccept(key, li)) {
	    	  key.cancel();
	      }
	  }

	  /**
	   * Complete the accept operation.
	   *
	   * @param key SelectionKey
	   * @param protocolContext ProtocolContext
	   * @return boolean
	   */
	  public abstract boolean finishAccept(SelectionKey key, ListeningInterface li);
	  
	  /**
	   * Stop accepting clients
	 * @throws IOException 
	   */
	  public abstract void stopAccepting() throws IOException;

	}

