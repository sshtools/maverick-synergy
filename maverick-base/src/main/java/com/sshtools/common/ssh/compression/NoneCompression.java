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

package com.sshtools.common.ssh.compression;

import java.io.IOException;


public class NoneCompression implements SshCompression {

	public void init(int type, int level) {
	}

	public byte[] compress(byte[] data, int start, int len) throws IOException {
		return uncompress(data, start, len);
	}

	public byte[] uncompress(byte[] data, int start, int len)
			throws IOException {
		if(len != data.length || start != 0) {
			byte[] arr = new byte[len];
			System.arraycopy(data, start, arr, 0, len);
			return arr;
		}
		else
			return data;
	}

	public String getAlgorithm() {
		return "none";
	}

}
