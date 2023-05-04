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
package com.sshtools.common.ssh;

import java.io.IOException;

import com.sshtools.common.util.UnsignedInteger32;

public interface Channel {

	UnsignedInteger32 getLocalWindow();

	UnsignedInteger32 getRemoteWindow();

	int getLocalPacket();

	void close();

	void sendData(byte[] array, int i, int size) throws IOException;

	void sendWindowAdjust(UnsignedInteger32 count);

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
