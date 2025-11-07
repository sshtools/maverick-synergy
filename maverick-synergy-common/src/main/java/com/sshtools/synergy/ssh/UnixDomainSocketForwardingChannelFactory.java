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
import java.net.SocketException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.util.Optional;

import com.sshtools.common.forwarding.ForwardingHandle;
import com.sshtools.common.forwarding.ForwardingRequest;
import com.sshtools.common.forwarding.ForwardingRequest.ForwardingType;
import com.sshtools.common.logger.Log;

public abstract class UnixDomainSocketForwardingChannelFactory<C extends SshContext>  
	extends SocketListeningForwardingChannelFactoryImpl<C, UnixDomainSocketAddress> {

	private final ForwardingType type;
	private final ThreadLocal<Boolean> deleteOnClose = new ThreadLocal<Boolean>();

	protected UnixDomainSocketForwardingChannelFactory(ForwardingRequest.ForwardingType type) {
		this.type = type;
	}

	@Override
	public int bindInterface(String addressToBind, int portToBind, ConnectionProtocol<?> connection, String channelType)
			throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void onAccept(final SocketChannel sc) throws SocketException {
		if(Log.isDebugEnabled()) { 
			Log.debug(channelType + " UNIX domain socket forwarding socket accepted");
		}
	}

	@Override
	protected UnixDomainSocketAddress createAddress(ForwardingRequest request) {
		return request.bindPathOr().map(bpath -> UnixDomainSocketAddress.of(bpath)).orElseGet(() -> {
              deleteOnClose.set(true);
	          return UnixDomainSockets.createTemporayAddress();
		});
	} 

	@Override
	protected ServerSocketChannel createSocketChannel() throws IOException {
        return ServerSocketChannel.open(StandardProtocolFamily.UNIX);
	}

	@Override
	protected ForwardingHandle createHandle() {
		boolean deleteOnClose = this.deleteOnClose.get();
		this.deleteOnClose.remove();
		
        return new ForwardingHandle() {

			@Override					
			public String toString() {
				return "{" + type.name() + "} : " + request + " @ " + boundPort().orElse(0);
			}

			@Override
			public void close(boolean killActiveTunnels) throws IOException {
				stopListening(killActiveTunnels);
				if(deleteOnClose) {
					Files.delete(addr.getPath().getFileName());
				}
			}

			@Override
			public ForwardingRequest.ForwardingType type() {
				return type;
			}

			@Override
			public ForwardingRequest request() {
				return request;
			}

			@Override
			public Optional<Integer> boundPort() {
				return Optional.empty();
			}

			@Override
			public Optional<String> boundPath() {
				return Optional.of(addr.getPath().toString());
			}
        	
        };
	}
}
