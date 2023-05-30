package com.sshtools.common.sftp.extensions;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import com.sshtools.common.sftp.SftpExtension;
import com.sshtools.common.sftp.SftpExtensionFactory;

public final class SftpExtensionLoaderFactory implements SftpExtensionFactory {
	
	private Map<String, SftpExtension> extensions = Collections.synchronizedMap(new HashMap<>());
	
	public SftpExtensionLoaderFactory() {
		for(var ext : ServiceLoader.load(SftpExtension.class)) {
			extensions.put(ext.getName(), ext);
		}
	}

	@Override
	public Set<String> getSupportedExtensions() {
		return Collections.unmodifiableSet(extensions.keySet());
	}

	@Override
	public SftpExtension getExtension(String requestName) {
		return extensions.get(requestName);
	}

	@Override
	public Collection<SftpExtension> getExtensions() {
		return extensions.values();
	}

}
