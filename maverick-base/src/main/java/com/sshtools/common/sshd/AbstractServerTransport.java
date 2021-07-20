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

package com.sshtools.common.sshd;

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
