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

package com.sshtools.synergy.ssh;

import java.io.IOException;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.util.ByteArrayWriter;

public class TransportProtocolHelper {

	public static byte[] generateKexInit(SshContext sshContext, boolean supportsExtInfo, String extInfo) throws SshException, IOException {
		
		try(ByteArrayWriter baw = new ByteArrayWriter()) {
			
			byte[] cookie = new byte[16];
			ComponentManager.getDefaultInstance().getRND().nextBytes(cookie);

			baw.write((byte) TransportProtocol.SSH_MSG_KEX_INIT);

			baw.write(cookie);

			String list = sshContext.supportedKeyExchanges().list(
					sshContext.getPreferredKeyExchange());

			if(supportsExtInfo) {
				list += "," + extInfo;
			}
			baw.writeString(list);

			list = sshContext.getSupportedPublicKeys();

			baw.writeString(list);

			list = sshContext.supportedCiphersCS().list(
					sshContext.getPreferredCipherCS());

			baw.writeString(list);

			list = sshContext.supportedCiphersSC().list(
					sshContext.getPreferredCipherSC());

			baw.writeString(list);

			list = sshContext.supportedMacsCS().list(
					sshContext.getPreferredMacCS());

			baw.writeString(list);

			list = sshContext.supportedMacsSC().list(
					sshContext.getPreferredMacSC());

			baw.writeString(list);

			list = sshContext.supportedCompressionsCS().list(
					sshContext.getPreferredCompressionCS());

			baw.writeString(list);

			list = sshContext.supportedCompressionsSC().list(
					sshContext.getPreferredCompressionSC());

			baw.writeString(list);

			baw.writeInt(0);
			baw.writeInt(0);
			baw.write((byte) 0);
			baw.writeInt(0);
			
			return baw.toByteArray();
		}
	}

}
