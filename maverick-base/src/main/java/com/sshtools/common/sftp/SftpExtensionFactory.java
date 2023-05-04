package com.sshtools.common.sftp;

import java.util.Collection;
import java.util.Set;

public interface SftpExtensionFactory {

	Set<String> getSupportedExtensions();

	SftpExtension getExtension(String requestName);

	Collection<SftpExtension> getExtensions();

}
