
package com.sshtools.common.sftp;

import com.sshtools.common.util.ByteArrayReader;

public interface SftpExtension {

	public static final int SSH_FXP_EXTENDED_REPLY = 201;
	
	void processMessage(ByteArrayReader msg, int requestId, SftpSubsystem sftp);

	boolean supportsExtendedMessage(int messageId);

	void processExtendedMessage(ByteArrayReader msg, SftpSubsystem sftp);

	boolean isDeclaredInVersion();

	byte[] getDefaultData();

	String getName();

}
