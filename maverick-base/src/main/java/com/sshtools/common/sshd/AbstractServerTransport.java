package com.sshtools.common.sshd;

/*-
 * #%L
 * Base API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
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

import com.sshtools.common.nio.IdleStateListener;
import com.sshtools.common.ssh.ConnectionAwareTask;
import com.sshtools.common.ssh.Context;
import com.sshtools.common.ssh.SshConnection;

public interface AbstractServerTransport<C extends Context> {

	void disconnect(int reason, String message);

	C getContext();

	void postMessage(SshMessage sshMessage, boolean kex);

	void sendNewKeys();
	
	SshConnection getConnection();

	void postMessage(SshMessage sshMessage);

	boolean isConnected();

	void addTask(Integer messageQueue, ConnectionAwareTask r);

	void startService(Service<C> service);

	byte[] getSessionKey();

	void registerIdleStateListener(IdleStateListener listener);

	void removeIdleStateListener(IdleStateListener listener);

	void resetIdleState(IdleStateListener listener);

	boolean isSelectorThread();

}
