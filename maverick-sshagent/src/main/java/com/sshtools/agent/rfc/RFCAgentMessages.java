package com.sshtools.agent.rfc;

/*-
 * #%L
 * Key Agent
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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
