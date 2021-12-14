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

package com.sshtools.common.ssh;

import java.io.IOException;

public interface Channel {

	int getLocalWindow();

	int getRemoteWindow();

	int getLocalPacket();

	void close();

	void sendData(byte[] array, int i, int size) throws IOException;

	void sendWindowAdjust(int bytesSinceLastWindowIssue);

	boolean isClosed();

	void addEventListener(ChannelEventListener listener);

	void sendChannelRequest(String requestName, boolean wantReply, byte[] data);

	void sendChannelRequest(String type, boolean wantreply,
			byte[] requestdata, ChannelRequestFuture future);

	SshConnection getConnection();

	Context getContext();

	boolean isRemoteEOF();

	boolean isLocalEOF();

	String getChannelType();
}
