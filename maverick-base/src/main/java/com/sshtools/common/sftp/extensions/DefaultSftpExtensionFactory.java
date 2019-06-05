package com.sshtools.common.sftp.extensions;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.sshtools.common.sftp.SftpExtension;
import com.sshtools.common.sftp.SftpExtensionFactory;

public class DefaultSftpExtensionFactory implements SftpExtensionFactory {

	
	    
	Map<String,SftpExtension> extensions = new HashMap<String,SftpExtension>();
	
	public DefaultSftpExtensionFactory() {
		extensions.put(MD5FileExtension.EXT_MD5_HASH, new MD5FileExtension());
		extensions.put(MD5HandleExtension.EXT_MD5_HASH_HANDLE, new MD5HandleExtension());
		extensions.put(PosixRenameExtension.POSIX_RENAME, new PosixRenameExtension());
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
