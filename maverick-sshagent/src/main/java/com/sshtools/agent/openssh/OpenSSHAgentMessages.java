
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
