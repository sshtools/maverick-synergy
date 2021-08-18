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

package com.sshtools.common.knownhosts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;

public class HostKeyVerificationManager implements HostKeyVerification {

	List<HostKeyVerification> verifiers = new ArrayList<HostKeyVerification>();
	
	public HostKeyVerificationManager(Collection<? extends HostKeyVerification> verifiers) {
		this.verifiers.addAll(verifiers);
	}
	
	public HostKeyVerificationManager(HostKeyVerification verif) {
		this.verifiers.add(verif);
	}
	
	public HostKeyVerificationManager(HostKeyVerification... verifs) {
		this.verifiers.addAll(Arrays.asList(verifs));
	}
	
	public void addVerifier(HostKeyVerification verif) {
		this.verifiers.add(verif);
	}
	
	public boolean verifyHost(String host, SshPublicKey pk) throws SshException {
		
		for(HostKeyVerification v : verifiers) {
			if(v.verifyHost(host, pk)) {
				return true;
			}
		}
		return true;
	}

}
