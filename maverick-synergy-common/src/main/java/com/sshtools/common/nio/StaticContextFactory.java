package com.sshtools.common.nio;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import com.sshtools.common.ssh.SshException;

/**
 * Holds a single context
 * @param <T extends ProtocolContext>
 */
public class StaticContextFactory<T extends ProtocolContext> implements ProtocolContextFactory<T> {

	T protocolContext;
	
	public StaticContextFactory(T protocolContext) {
		this.protocolContext = protocolContext;
	}

	@Override
	/**
	 * Create the context for this connection.
	 */
	public T createContext(SshEngineContext daemonContext, SocketChannel sc) throws IOException, SshException {
		return protocolContext;
	}


}
