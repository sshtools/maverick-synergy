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
 * Copyright (C) 2002-2023 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */
package com.sshtools.synergy.niofs;

/*-
 * #%L
 * Java NIO File System ImplemnentationI
 * %%
 * Copyright (C) 2002 - 2026 JADAPTIVE Limited
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

import static com.sshtools.common.util.Utils.encodeUserInfo;

import java.net.URI;
import java.util.Optional;

import com.sshtools.common.util.Utils;

public  final class SftpPathBuilder {

	private Optional<String> username = Optional.empty();
	private Optional<char[]> password = Optional.empty();
	private Optional<String> host = Optional.empty();
	private Optional<Integer> port = Optional.empty();
	private Optional<String> path = Optional.empty();
	
	public static SftpPathBuilder create() {
		return new SftpPathBuilder();
	}
	
	private SftpPathBuilder() {
	}

	public final SftpPathBuilder withUsername(String username) {
		return withUsername(Utils.emptyOptionalIfBlank(username));
	}

	public final SftpPathBuilder withUsername(Optional<String> username) {
		this.username = username;
		return this;
	}

	public final SftpPathBuilder withPassword(String password) {
		return withPasswordCharacters(password.toCharArray());
	}

	public final SftpPathBuilder withPassword(Optional<String> password) {
		return withPasswordCharacters(password.map(p -> p.toCharArray()));
	}

	public final SftpPathBuilder withPasswordCharacters(char[] password) {
		return withPasswordCharacters(Utils.emptyOptionalIfBlank(password));
	}

	public final SftpPathBuilder withPasswordCharacters(Optional<char[]> password) {
		this.password = password;
		return this;
	}

	public final SftpPathBuilder withPath(String path) {
		this.path = Optional.ofNullable(path);
		return this;
	}

	public final SftpPathBuilder withHost(String host) {
		this.host = Optional.of(host);
		return this;
	}

	public final SftpPathBuilder withPort(int port) {
		this.port = Optional.of(port);
		return this;
	}

	public URI build() {
		var uriStr = new StringBuilder("sftp://");
		uriStr.append(encodeUserInfo( username.orElse("guest")));
		password.ifPresent(p -> {
			uriStr.append(":");
			uriStr.append(encodeUserInfo( new String(p)));
		});
		uriStr.append("@");
		uriStr.append(host.orElse("localhost"));
		port.ifPresent(p -> {
			if(p != 22) {
				uriStr.append(":");
				uriStr.append(p);
			}
		});
		uriStr.append("/" + path.orElse(""));
		return URI.create(uriStr.toString());

	}
}
