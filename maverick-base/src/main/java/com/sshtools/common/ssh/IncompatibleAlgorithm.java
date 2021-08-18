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

public class IncompatibleAlgorithm {

	public enum ComponentType { CIPHER_CS, CIPHER_SC, MAC_CS, MAC_SC, KEYEXCHANGE, PUBLICKEY, COMPRESSION_CS, COMPRESSION_SC };

	ComponentType type;
	String[] localAlgorithms;
	String[] remoteAlgorithms;
	
	public IncompatibleAlgorithm(ComponentType type, String[] localAlgorithms, String[] remoteAlgorithms) {
		this.type = type;
		this.localAlgorithms = localAlgorithms;
		this.remoteAlgorithms = remoteAlgorithms;
	}

	public ComponentType getType() {
		return type;
	}

	public String[] getLocalAlgorithms() {
		return localAlgorithms;
	}

	public String[] getRemoteAlgorithms() {
		return remoteAlgorithms;
	}
	
	
}
