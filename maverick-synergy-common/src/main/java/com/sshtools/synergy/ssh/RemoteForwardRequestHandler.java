package com.sshtools.synergy.ssh;

/*-
 * #%L
 * Common API
 * %%
 * Copyright (C) 2002 - 2025 JADAPTIVE Limited
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

import java.io.IOException;
import java.util.Optional;

import com.sshtools.common.forwarding.ForwardingHandle;
import com.sshtools.common.forwarding.ForwardingRequest;
import com.sshtools.common.ssh.SshException;

/**
 * Interface to be implemented by classes that can start a <em>Remote Forward</em>
 * of some sort, e.g. TCP or Unix Domain Socket.
 * 
 * @param <T> type of context
 */
public interface RemoteForwardRequestHandler<T extends SshContext> {

	/**
	 * Query if this handler will handle a particular forwarding request.
	 * <p>
	 * Note, from version 3.3.0 it will be required to implement this method
	 * and {@link #startRemoteForward(ForwardingRequest, ConnectionProtocol)} will
	 * be removed. As from 3.2.0, you can implement both of these now, but you 
	 * will also need to continue to implement the deprecated methods until they
	 * are removed. The implementations need not do anything other than throw an
	 * {@link UnsupportedOperationException}, as they will never been called by 
	 * Maverick Synergy itself. 
	 *  
	 * @param request request
	 * @param conn connect
	 * @return will handle
	 */
	default boolean isHandled(ForwardingRequest request, ConnectionProtocol<T> conn) {
		switch(request.protocol()) {
		case DOMAIN_SOCKETS:
			return isHandled(request.destinationPath(), 90, request.bindPath().toString(), 0, conn);
		case TCP:
			return isHandled(request.bindAddress(), request.bindPort(), request.destinationAddress(), request.destinationPort(), conn);
		default:
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Query if this handler will handle a particular forwarding request.
	 * <p>
	 * Note, as from 3.2.0, you should consider instead implementing {@link #startRemoteForward(ForwardingRequest, ConnectionProtocol)}
	 * instead, and throwing an {@link UnsupportedOperationException} from your implementation of
	 * this method.
	 *  
	 * @param request request
	 * @param conn connect
	 * @return will handle
	 */
	@Deprecated(forRemoval = true, since = "3.2.0")
	boolean isHandled(String hostToBind, int portToBind, String destinationHost, int destinationPort, ConnectionProtocol<T> conn);
	
	/**
	 * Start a remote forward.
	 * <p>
	 * Note, as from 3.2.0, you should consider instead implementing {@link #startRemoteForward(ForwardingRequest, ConnectionProtocol)}
	 * instead, and throwing an {@link UnsupportedOperationException} from your implementation of
	 * this method.
	 * 
	 * @param hostToBind host to bind (remote listening address)
	 * @param portToBind port to bind (remote listening port)
	 * @param destinationHost destination host (local address to forward to)
	 * @param destinationPort destination port (local port to forward to)
	 * @return the actual bound port (where applicable, zero otherwise)
	 * @throws SshException on error
	 */
	@Deprecated(forRemoval = true, since = "3.2.0")
	int startRemoteForward(String hostToBind, int portToBind, String destinationHost, int destinationPort, ConnectionProtocol<T> conn) throws SshException;
	
	/**
	 * Start a remote forward.
	 * <p>
	 * Note, from version 3.3.0 it will be required to implement this method
	 * and {@link #isHandled(ForwardingRequest, ConnectionProtocol)} will
	 * be removed. As from 3.2.0, you can implement both of these now, but you 
	 * will also need to continue to implement the deprecated methods until they
	 * are removed. The implementations need not do anything other than throw an
	 * {@link UnsupportedOperationException}, as they will never been called by 
	 * Maverick Synergy itself.
	 * 
	 * @param request request
	 * @param conn connection
	 * @return handle handle that may be used to {@link ForwardingHandle#close()} the forwarding,
	 *         or query the actual bound port (where applicable)
	 * @throws SshException on error
	 */
	default ForwardingHandle startRemoteForward(ForwardingRequest request, ConnectionProtocol<T> conn) throws SshException {
		switch(request.protocol()) {
		case DOMAIN_SOCKETS:
			return new ForwardingHandle() {

				@Override					
				public String toString() {
					return type().name() + " : " + request;
				}
				
				@Override
				public void close(boolean killActiveTunnels) throws IOException {
					try {
						stopRemoteForward(request.destinationPath(), 0, request.bindPath().toString(), 0, conn);
					} catch (SshException e) {
						throw new IOException("Failed to close remote forward.", e);
					}
				}
				
				@Override
				public Optional<Integer> boundPort() {
					return Optional.empty();
				}

				@Override
				public ForwardingRequest request() {
					return request;
				}

				@Override
				public ForwardingRequest.ForwardingType type() {
					return ForwardingRequest.ForwardingType.REMOTE;
				}

				@Override
				public Optional<String> boundPath() {
					return request.bindPathOr();
				}
			};
		default:
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Stop a remote forward.
	 * <p>
	 * Deprecated, instead using {@link ForwardingHandle#close()}.
	 * 
	 * @param hostToBind host to bind (remote listening address)
	 * @param portToBind port to bind (remote listening port)
	 * @param destinationHost destination host (local address to forward to)
	 * @param destinationPort destination port (local port to forward to)
	 * @param conn
	 * @throws SshException
	 */
	@Deprecated(forRemoval = true, since = "3.2.0")
	void stopRemoteForward(String hostToBind, int portToBind, String destinationHost, int destinationPort, ConnectionProtocol<T> conn) throws SshException;
}
