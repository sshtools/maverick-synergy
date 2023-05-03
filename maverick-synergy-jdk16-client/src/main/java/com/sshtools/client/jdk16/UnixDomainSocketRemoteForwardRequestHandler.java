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
package com.sshtools.client.jdk16;

import java.io.IOException;
import java.nio.file.Paths;

import com.sshtools.client.SshClientContext;
import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.GlobalRequest;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.synergy.jdk16.UnixDomainSockets;
import com.sshtools.synergy.ssh.ConnectionProtocol;
import com.sshtools.synergy.ssh.RemoteForwardRequestHandler;

public class UnixDomainSocketRemoteForwardRequestHandler implements RemoteForwardRequestHandler<SshClientContext> {

	@Override
	public boolean isHandled(String hostToBind, int portToBind, String destinationHost, int destinationPort, ConnectionProtocol<SshClientContext> conn) {
		/* Unix domain sockets will always have both ports zero */
		if(portToBind != 0 || destinationPort == 0)
			return false;
		/* Both hosts will actually be absolute paths */
		return Paths.get(hostToBind).isAbsolute() && Paths.get(hostToBind).isAbsolute();
	}

	@Override
	public int startRemoteForward(String hostToBind, int portToBind, String destinationHost, int destinationPort, ConnectionProtocol<SshClientContext> conn) throws SshException {
		try(var msg = new ByteArrayWriter()) {
			msg.writeString(hostToBind);
			msg.writeString(""); // Reserved
			
			var request = new GlobalRequest(UnixDomainSockets.STREAM_LOCAL_FORWARD_REQUEST, conn.getConnection(), msg.toByteArray());

			conn.sendGlobalRequest(request, true);
			request.waitForever();
			
			if(request.isSuccess()) {
				if(Log.isInfoEnabled()) {
					Log.info("Remote domain socket forwarding is now active on remote interface " + hostToBind  
							+ " forwarding to " + destinationHost);
				}
				
				return 0;
			} else {
				throw new SshException("Remote domain socket forwarding on interface " 
							+ hostToBind + ":" + portToBind + " failed", SshException.FORWARDING_ERROR);
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

			var request = new GlobalRequest(UnixDomainSockets.CANCEL_STREAM_LOCAL_FORWARD_REQUEST, conn.getConnection(), msg.toByteArray());

			conn.sendGlobalRequest(request, true);
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
