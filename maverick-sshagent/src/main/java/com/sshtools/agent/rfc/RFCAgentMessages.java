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

package com.sshtools.agent.rfc;

public class RFCAgentMessages {

	public final static int SSH_MSG_USERAUTH_PK_OK = 60;
	public static final int SSH_AGENT_SUCCESS = 101;
	public static final int SSH_AGENT_FAILURE = 102;
	public static final int SSH_AGENT_VERSION_RESPONSE = 103;
	public static final int SSH_AGENT_KEY_LIST = 104;
	public static final int SSH_AGENT_OPERATION_COMPLETE = 105;
	public static final int SSH_AGENT_RANDOM_DATA = 106;
	public static final int SSH_AGENT_ALIVE = 150;
	public static final int SSH_AGENT_ADD_KEY = 202;
	public static final int SSH_AGENT_DELETE_ALL_KEYS = 203;
	public static final int SSH_AGENT_LIST_KEYS = 204;
	public static final int SSH_AGENT_PRIVATE_KEY_OP = 205;
	public static final int SSH_AGENT_FORWARDING_NOTICE = 206;
	public static final int SSH_AGENT_DELETE_KEY = 207;
	public static final int SSH_AGENT_LOCK = 208;
	public static final int SSH_AGENT_UNLOCK = 209;
	public static final int SSH_AGENT_PING = 212;
	public static final int SSH_AGENT_RANDOM = 213;

	// Agent errors
	public static final int SSH_AGENT_ERROR_TIMEOUT = 1;
	public static final int SSH_AGENT_ERROR_KEY_NOT_FOUND = 2;
	public static final int SSH_AGENT_ERROR_DECRYPT_FAILED = 3;
	public static final int SSH_AGENT_ERROR_SIZE_ERROR = 4;
	public static final int SSH_AGENT_ERROR_KEY_NOT_SUITABLE = 5;
	public static final int SSH_AGENT_ERROR_DENIED = 6;
	public static final int SSH_AGENT_ERROR_FAILURE = 7;
	public static final int SSH_AGENT_ERROR_UNSUPPORTED_OP = 8;

	public static final int SSH_AGENT_REQUEST_VERSION = 9;

	// Agent version
	public static final int SSH_AGENT_VERSION = 2;
}
