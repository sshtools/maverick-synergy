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

package com.sshtools.agent.openssh;

public class OpenSSHAgentMessages {

	public static final int SSH_AGENT_RSA_SHA2_256 = 2;
	public static final int SSH_AGENT_RSA_SHA2_512 = 4;
	
	public static final int SSH_AGENT_FAILURE = 5;
	public static final int SSH_AGENT_SUCCESS = 6;

	public static final int SSH2_AGENTC_REQUEST_IDENTITIES		=	11;
	public static final int SSH2_AGENTC_SIGN_REQUEST	=		13;
	public static final int SSH2_AGENTC_ADD_IDENTITY	=		17;
	public static final int SSH2_AGENTC_REMOVE_IDENTITY		=	18;
	public static final int SSH2_AGENTC_REMOVE_ALL_IDENTITIES	=	19;
	public static final int SSH2_AGENTC_ADD_ID_CONSTRAINED	=		25;
	public static final int SSH_AGENTC_LOCK			=		22;
	public static final int SSH_AGENTC_UNLOCK		=		23;
	
	public static final int SSH2_AGENT_IDENTITIES_ANSWER = 12;
	public static final int SSH2_AGENT_SIGN_RESPONSE = 14;
}
