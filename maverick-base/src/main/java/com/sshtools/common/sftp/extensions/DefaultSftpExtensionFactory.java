package com.sshtools.common.sftp.extensions;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sshtools.common.sftp.SftpExtension;
import com.sshtools.common.sftp.SftpExtensionFactory;
import com.sshtools.common.sftp.extensions.filter.OpenDirectoryWithFilterExtension;

public class DefaultSftpExtensionFactory implements SftpExtensionFactory {

	Map<String,SftpExtension> extensions = new HashMap<String,SftpExtension>();
	
	public DefaultSftpExtensionFactory() {
	}
	
	public DefaultSftpExtensionFactory(SupportedSftpExtensions... supportedExtensions) {
		
		List<SupportedSftpExtensions> supported = Arrays.asList(supportedExtensions);
		
		if(supported.contains(SupportedSftpExtensions.MD5_FILE_HASH)) {
			extensions.put(MD5FileExtension.EXTENSION_NAME, new MD5FileExtension());
			extensions.put(MD5HandleExtension.EXTENSION_NAME, new MD5HandleExtension());
		}
		if(supported.contains(SupportedSftpExtensions.POSIX_RENAME)) {
			extensions.put(PosixRenameExtension.EXTENSION_NAME, new PosixRenameExtension());
		}
		if(supported.contains(SupportedSftpExtensions.COPY_FILE)) {
			extensions.put(CopyFileSftpExtension.EXTENSION_NAME, new CopyFileSftpExtension());
		}
		if(supported.contains(SupportedSftpExtensions.OPEN_DIRECTORY_WITH_FILTER)) {
			extensions.put(OpenDirectoryWithFilterExtension.EXTENSION_NAME, new OpenDirectoryWithFilterExtension());
		}
	}
	
	@Override
	public SftpExtension getExtension(String requestName) {
		return extensions.get(requestName);
	}

	@Override
	public Set<String> getSupportedExtensions() {
		return Collections.unmodifiableSet(extensions.keySet());
	}

	@Override
	public Collection<SftpExtension> getExtensions() {
		return Collections.unmodifiableCollection(extensions.values());
	}

}
