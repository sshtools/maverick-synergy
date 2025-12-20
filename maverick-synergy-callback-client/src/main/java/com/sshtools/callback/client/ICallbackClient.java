package com.sshtools.callback.client;

/*-
 * #%L
 * Callback Client API
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

import com.sshtools.common.permissions.Policy;
import com.sshtools.common.policy.FileFactory;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.server.SshServerContext;
import com.sshtools.synergy.ssh.ChannelFactory;
import com.sshtools.synergy.ssh.Connection;

public interface ICallbackClient {

	public interface CallbackClientListener {

		default void onClientStarting(ICallbackSession client) {}

		default void onClientStopping(ICallbackSession client) {}

		default void onClientStart(ICallbackSession client, SshConnection connection) {}

		default void onClientStop(ICallbackSession client, Connection<?> con) {}

		default void onConfigureContext(SshServerContext sshContext, CallbackConfiguration config) {}
	}

	ICallbackSession start(CallbackConfiguration config) throws IOException;

	boolean isConnected();
	
	int getConnections();

	void stop();

	void addHostKey(SshKeyPair pair);

	void setFileFactory(FileFactory fileFactory);

	void waitForShutdown();

	Throwable getLastError();

	void setPolicyDefaults(Policy... policies);

	void setChannelFactory(ChannelFactory<SshServerContext> channelFactory);

	void addListener(CallbackClientListener listener);

	void removeListener(CallbackClientListener listener);

}
