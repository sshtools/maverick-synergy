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
package com.sshtools.bc.tests;

import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.jce.JCEProvider;
import com.sshtools.common.tests.Ed25519PublicKeyTests;

public class BCEd25519PublicKeyTests extends Ed25519PublicKeyTests {

	public void setUp() {
		JCEProvider.enableBouncyCastle(true);
		ComponentManager.reset();
	}

	public void tearDown() {
		JCEProvider.disableBouncyCastle();
		ComponentManager.reset();
	}
	
	@Override
	protected String getTestingJCE() {
		return "BC";
	}

}
