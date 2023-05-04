package com.sshtools.synergy.nio;

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

