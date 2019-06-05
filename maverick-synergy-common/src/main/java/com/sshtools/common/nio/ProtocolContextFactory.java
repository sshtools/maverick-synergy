package com.sshtools.common.nio;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import com.sshtools.common.ssh.SshException;

/**
 * Creates the ProtocolContext for a new incoming connection.
 * @param <T>
 */
public interface ProtocolContextFactory<T extends ProtocolContext> {

	T createContext(SshEngineContext daemonContext, SocketChannel sc) throws IOException, SshException;
}
