package com.sshtools.agent.openssh;

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
