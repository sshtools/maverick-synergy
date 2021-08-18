/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */

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
