package com.sshtools.common.sftp.extensions;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.sshtools.common.sftp.SftpExtension;
import com.sshtools.common.sftp.SftpExtensionFactory;

public class BasicSftpExtensionFactory implements SftpExtensionFactory {
	private final Map<String,SftpExtension> extensions;
	
	public BasicSftpExtensionFactory(SftpExtension... extensions) {
		this(Arrays.asList(extensions));
	}

	public BasicSftpExtensionFactory(Collection<SftpExtension> extensions) {
		var m = new HashMap<String, SftpExtension>();
		extensions.forEach(x -> m.put(x.getName(), x));
		this.extensions = Collections.unmodifiableMap(m);
	}

	@Override
	public Set<String> getSupportedExtensions() {
		return extensions.keySet();
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
