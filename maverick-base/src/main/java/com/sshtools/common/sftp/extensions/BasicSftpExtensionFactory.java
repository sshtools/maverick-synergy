package com.sshtools.common.sftp.extensions;

/*-
 * #%L
 * Base API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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
