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

package com.sshtools.server.callback;



import com.sshtools.client.SshClientContext;
import com.sshtools.client.TransportProtocolClient;
import com.sshtools.common.logger.Log;
import com.sshtools.server.SshServerContext;
import com.sshtools.server.TransportProtocolServer;
import com.sshtools.synergy.nio.ConnectRequestFuture;
import com.sshtools.synergy.nio.LicenseException;
import com.sshtools.synergy.nio.ProtocolContextFactory;

class TransportProtocolSwitchingClient extends TransportProtocolClient {

	ProtocolContextFactory<SshServerContext> serverFactory;
	String callbackIdentifier;
	ConnectRequestFuture connectFuture;
	
	public TransportProtocolSwitchingClient(SshClientContext sshContext, 
			String callbackIdentifier,
			ProtocolContextFactory<SshServerContext> serverFactory,
			ConnectRequestFuture connectFuture) throws LicenseException {
		super(sshContext, connectFuture);
		this.serverFactory = serverFactory;
		this.callbackIdentifier = callbackIdentifier;
		this.connectFuture = connectFuture;
	}

	@Override
	protected void onRemoteIdentificationReceived(String remoteIdentification) {
		
		if(!remoteIdentification.startsWith("SSH-2.0-" + callbackIdentifier)) {
			try {
				SshServerContext context = serverFactory.createContext(sshContext.getDaemonContext(), getSocketConnection().getSocketChannel());
				TransportProtocolServer engine = (TransportProtocolServer) context.createEngine(connectFuture);
				
				transferState(engine);
				getSocketConnection().setProtocolEngine(engine);
				
			} catch (Exception e) {
				Log.error("Failed to switch roles", e);
				socketConnection.closeConnection(true);
			} finally {
				getContext().getConnectionManager().unregisterTransport(this);
			}
		} else {
			/**
			 * We need to know the user name to initiate authentication since we are acting as the client. So
			 * the callback client places the user name in the initial SSH identification string.
			 */
			int idx = remoteIdentification.indexOf('_');
			if(idx > -1) {
				if(remoteIdentification.trim().length() > idx+1) {
					String username = remoteIdentification.substring(idx+1).trim();
					if(Log.isDebugEnabled()) {
						Log.debug("Callback client username is {}", username);
					}
					getContext().setUsername(username);
					return;
				}
			} else {
				int length = "SSH-2.0-".length() + callbackIdentifier.length();
				String username = remoteIdentification.substring(length+1).trim();
				if(Log.isDebugEnabled()) {
					Log.debug("Callback client username is {}", username);
				}
				getContext().setUsername(username);
				return;
			}
			
			throw new IllegalStateException(String.format("Callback identifier missing _ or username token [%s]", remoteIdentification.trim()));
		}
		
	}

	
}
