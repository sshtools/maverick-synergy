package com.sshtools.common.ssh.components;

import java.util.Date;
import java.util.Set;

public interface SshCertificate {

	public static final int SSH_CERT_TYPE_USER = 1;
	public static final int SSH_CERT_TYPE_HOST = 2;

	SshPublicKey getSignedBy();

	int getType();

	Date getValidAfter();

	Date getValidBefore();

	Set<String> getPrincipals();

}
