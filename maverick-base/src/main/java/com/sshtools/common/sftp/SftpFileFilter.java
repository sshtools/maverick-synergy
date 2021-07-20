
package com.sshtools.common.sftp;

public interface SftpFileFilter {
	boolean matches(String name);
}
