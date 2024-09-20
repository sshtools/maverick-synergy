package com.sshtools.common.files.nio;

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
import java.net.URI;
import java.net.URISyntaxException;

import com.sshtools.common.ssh.SshConnection;


public class AbstractFileURI {

	public static final String URI_SCHEME = "abfs";

	private String path;
	String connectionUUID;
	
	private AbstractFileURI(URI uri) {
		validate(uri);
		connectionUUID = uri.getAuthority();
		path = uri.getPath();
	}

	public static URI create(SshConnection con, String... paths) {
		try {
			return new URI(URI_SCHEME, con.getUUID(), "/" + String.join("/", paths), null, null);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Can not create URI from given input", e);
		}
	}

	static AbstractFileURI parse(URI uri) throws IllegalArgumentException {
		return new AbstractFileURI(uri);
	}

	public String getPath() {
		return path;
	}
	
	public String getConnectionId() {
		return connectionUUID;
	}
	
	private static void validate(URI uri) {
		if (!URI_SCHEME.equals(uri.getScheme())) {
			throw new IllegalArgumentException("URI must have " + URI_SCHEME + " scheme");
		}
		if (uri.getAuthority() == null) {
			throw new IllegalArgumentException("URI must have an authority");
		}
		if (uri.getPath() == null || uri.getPath().isEmpty()) {
			throw new IllegalArgumentException("URI must have a path");
		}
		if (uri.getQuery() != null) {
			throw new IllegalArgumentException("URI must not have a query part");
		}
		if (uri.getFragment() != null) {
			throw new IllegalArgumentException("URI must not have a fragment part");
		}
	}

}
