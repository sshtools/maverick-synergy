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
import com.sshtools.common.sftp.extensions.multipart.CreateMultipartFileExtension;
import com.sshtools.common.sftp.extensions.multipart.OpenMultipartFileExtension;

/**
 * Deprecated. See {@link SftpExtensionLoaderFactory} and {@link BasicSftpExtensionFactory}.
 */
@Deprecated(since = "3.1.0", forRemoval = true)
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
		if(supported.contains(SupportedSftpExtensions.COPY_DATA)) {
			extensions.put(CopyDataSftpExtension.EXTENSION_NAME, new CopyDataSftpExtension());
		}
		if(supported.contains(SupportedSftpExtensions.CHECK_FILE_NAME)) {
			extensions.put(FilenameHashingExtension.EXTENSION_NAME, new FilenameHashingExtension());
		}
		if(supported.contains(SupportedSftpExtensions.CHECK_FILE_HANDLE)) {
			extensions.put(FileHandleHashingExtension.EXTENSION_NAME, new FileHandleHashingExtension());
		}
		if(supported.contains(SupportedSftpExtensions.OPEN_PART_FILE)) {
			extensions.put(OpenMultipartFileExtension.EXTENSION_NAME, new OpenMultipartFileExtension());
		}
		if(supported.contains(SupportedSftpExtensions.CREATE_MULTIPART_FILE)) {
			extensions.put(CreateMultipartFileExtension.EXTENSION_NAME, new CreateMultipartFileExtension());
		}
		if(supported.contains(SupportedSftpExtensions.HARDLINK)) {
			extensions.put(HardLinkExtension.EXTENSION_NAME, new HardLinkExtension());
		}
		if(supported.contains(SupportedSftpExtensions.STATVFS)) {
			extensions.put(StatVFSExtension.EXTENSION_NAME, new StatVFSExtension());
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
