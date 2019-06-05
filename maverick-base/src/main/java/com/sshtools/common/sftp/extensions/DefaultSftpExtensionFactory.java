/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
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
