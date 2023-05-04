/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.client;

import java.io.IOException;
import java.nio.file.Paths;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.GlobalRequest;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.synergy.ssh.ConnectionProtocol;
import com.sshtools.synergy.ssh.RemoteForwardRequestHandler;

public class DefaultRemoteForwardRequestHandler implements RemoteForwardRequestHandler<SshClientContext> {

	@Override
	public boolean isHandled(String hostToBind, int portToBind, String destinationHost, int destinationPort,
			ConnectionProtocol<SshClientContext> conn) {
		/* Avoid unix domain socket paths. Both ports will also both be zero */
		if (portToBind == 0 && destinationPort == 0)
			return false;
		return !Paths.get(hostToBind).isAbsolute() || !Paths.get(hostToBind).isAbsolute();
	}

	@Override
	public int startRemoteForward(String hostToBind, int portToBind, String destinationHost, int destinationPort,
			ConnectionProtocol<SshClientContext> conn) throws SshException {
		try (var msg = new ByteArrayWriter()) {
			msg.writeString(hostToBind);
			msg.writeInt(portToBind);

			var request = new GlobalRequest("tcpip-forward", conn.getConnection(), msg.toByteArray());

			conn.sendGlobalRequest(request, true);
			request.waitForever();

			if (request.isSuccess()) {

				if (request.getData().length > 0) {
					try (ByteArrayReader r = new ByteArrayReader(request.getData())) {
						portToBind = (int) r.readInt();
					}
				}

				if (Log.isInfoEnabled()) {
					Log.info("Remote forwarding is now active on remote interface " + hostToBind + ":" + portToBind
							+ " forwarding to " + destinationHost + ":" + destinationPort);
				}

				return portToBind;
			} else {
				throw new SshException("Remote forwarding on interface " + hostToBind + ":" + portToBind + " failed",
						SshException.FORWARDING_ERROR);
			}
		} catch (IOException e) {
			throw new SshException(SshException.INTERNAL_ERROR, e);
		}
	}

	@Override
	public void stopRemoteForward(String hostToBind, int portToBind, String destinationHost, int destinationPort,
			ConnectionProtocol<SshClientContext> conn) throws SshException {

		try(var msg = new ByteArrayWriter()) {
			msg.writeString(hostToBind);
			msg.writeInt(portToBind);

			var request = new GlobalRequest("cancel-tcpip-forward", conn.getConnection(), msg.toByteArray());

			conn.sendGlobalRequest(request, true);
			request.waitForever();

			if (request.isSuccess()) {

				if (Log.isInfoEnabled()) {
					Log.info("Remote forwarding cancelled on remote interface " + hostToBind + ":" + portToBind);
				}

			} else {
				throw new SshException(
						"Cancel remote forwarding on interface " + hostToBind + ":" + portToBind + " failed",
						SshException.FORWARDING_ERROR);
			}
		} catch (IOException e) {
			throw new SshException(SshException.INTERNAL_ERROR, e);
		}
	}

}
