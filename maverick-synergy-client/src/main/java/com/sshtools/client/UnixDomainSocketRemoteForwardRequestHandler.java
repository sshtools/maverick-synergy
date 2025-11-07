package com.sshtools.client;

/*-
 * #%L
 * Client API
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
import java.nio.file.Paths;
import java.util.Optional;

import com.sshtools.common.forwarding.ForwardingHandle;
import com.sshtools.common.forwarding.ForwardingRequest;
import com.sshtools.common.forwarding.ForwardingRequest.ForwardingRequestBuilder;
import com.sshtools.common.forwarding.ForwardingRequest.Protocol;
import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.GlobalRequest;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.synergy.ssh.ConnectionProtocol;
import com.sshtools.synergy.ssh.RemoteForwardRequestHandler;
import com.sshtools.synergy.ssh.UnixDomainSockets;

public class UnixDomainSocketRemoteForwardRequestHandler implements RemoteForwardRequestHandler<SshClientContext> {

	@Override
	public boolean isHandled(ForwardingRequest request, ConnectionProtocol<SshClientContext> conn) {
		return request.protocol() == Protocol.DOMAIN_SOCKETS;
	}

	@Override
	public ForwardingHandle startRemoteForward(ForwardingRequest fwdreq, ConnectionProtocol<SshClientContext> conn)
			throws SshException {
		try(var msg = new ByteArrayWriter()) {
			msg.writeString(fwdreq.bindPath());
			msg.writeString(""); // Reserved
			
			var request = new GlobalRequest(UnixDomainSockets.STREAM_LOCAL_FORWARD_REQUEST, conn.getConnection(), msg.toByteArray());

			conn.sendGlobalRequestAndWait(request, 60000L);
			
			if(request.isSuccess()) {
				if(Log.isInfoEnabled()) {
					Log.info("Remote domain socket forwarding is now active on remote interface " + fwdreq.destinationPath()  
							+ " forwarding to " + fwdreq.bindPath());
				}
				
				return new ForwardingHandle() {
					
					@Override					
					public String toString() {
						return "{" + type().name() + "} : " + fwdreq;
					}
					
					@Override
					public void close(boolean killActiveTunnels) throws IOException {
						try {
							stopRemoteForward(fwdreq.bindPath(), 0, fwdreq.destinationPath(), 0, conn);
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
						return fwdreq;
					}

					@Override
					public ForwardingRequest.ForwardingType type() {
						return ForwardingRequest.ForwardingType.REMOTE;
					}

					@Override
					public Optional<String> boundPath() {
						return Optional.of(fwdreq.bindPath());
					}
				};
			} else {
				throw new SshException("Remote domain socket forwarding on interface " 
							+ fwdreq.bindPath() + " failed", SshException.FORWARDING_ERROR);
			}
		} catch (IOException e) {
			throw new SshException(SshException.INTERNAL_ERROR, e);
		} 
	}

	@Override
	@Deprecated
	public boolean isHandled(String hostToBind, int portToBind, String destinationHost, int destinationPort, ConnectionProtocol<SshClientContext> conn) {
		/* Unix domain sockets will always have both ports zero */
		if(portToBind != 0 || destinationPort != 0)
			return false;
		/* Both hosts will actually be absolute paths */
		return Paths.get(hostToBind).isAbsolute() && Paths.get(hostToBind).isAbsolute();
	}

	@Override
	public int startRemoteForward(String hostToBind, int portToBind, String destinationHost, int destinationPort, ConnectionProtocol<SshClientContext> conn) throws SshException {
		return startRemoteForward(ForwardingRequestBuilder.create().
				withProtocol(Protocol.DOMAIN_SOCKETS).
				withBindPath(destinationHost).
				withDestinationPath(hostToBind).
				build(), conn).boundPort().orElse(0);
	}

	@Override
	public void stopRemoteForward(String hostToBind, int portToBind, String destinationHost, int destinationPort,
			ConnectionProtocol<SshClientContext> conn) throws SshException {

		try(var msg = new ByteArrayWriter()) {
			msg.writeString(hostToBind);

			var request = new GlobalRequest(UnixDomainSockets.CANCEL_STREAM_LOCAL_FORWARD_REQUEST, conn.getConnection(), msg.toByteArray());

			conn.sendGlobalRequest(request);
			request.waitForever();

			if (request.isSuccess()) {

				if (Log.isInfoEnabled()) {
					Log.info("Remote domain socket forwarding cancelled on remote interface " + hostToBind);
				}

			} else {
				throw new SshException(
						"Cancel remote domain socket forwarding on interface " + hostToBind + " failed",
						SshException.FORWARDING_ERROR);
			}
		} catch (IOException e) {
			throw new SshException(SshException.INTERNAL_ERROR, e);
		}
	}
}
