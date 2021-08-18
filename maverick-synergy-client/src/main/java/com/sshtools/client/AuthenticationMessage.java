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

package com.sshtools.client;

import java.nio.ByteBuffer;

import com.sshtools.common.logger.Log;
import com.sshtools.common.sshd.SshMessage;
import com.sshtools.synergy.util.EncodingUtils;

public class AuthenticationMessage implements SshMessage {

	byte[] username;
	byte[] servicename;
	byte[] methodname;

	public AuthenticationMessage(String username, String servicename, String methodname) {
		this.username = EncodingUtils.getUTF8Bytes(username);
		this.servicename = EncodingUtils.getUTF8Bytes(servicename);
		this.methodname = EncodingUtils.getUTF8Bytes(methodname);
	}

	@Override
	public boolean writeMessageIntoBuffer(ByteBuffer buf) {

		buf.put((byte) AuthenticationProtocolClient.SSH_MSG_USERAUTH_REQUEST);
		buf.putInt(username.length);
		buf.put(username);
		buf.putInt(servicename.length);
		buf.put(servicename);
		buf.putInt(methodname.length);
		buf.put(methodname);

		return true;
	}
	
	@Override
	public void messageSent(Long sequenceNo) {

		if(Log.isDebugEnabled()) {
			Log.info("SSH_MSG_USERAUTH_REQUEST sent method="
					+ EncodingUtils.getUTF8String(methodname) + " username="
					+ EncodingUtils.getUTF8String(username));
		}
	}
}
