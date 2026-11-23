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

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import com.sshtools.client.SshClient;
import com.sshtools.client.sftp.SftpClient;
import com.sshtools.client.sftp.SftpClient.SftpClientBuilder;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.Utils;

/**
 * Convenience methods to create new SFTP {@link FileSystem} instances directly,
 * rather through resolution of {@link Path} through that standard libraries.
 * <p>
 * This allows you to easily do things such as re-use existing {@link SftpClient} instances
 * as more.
 *
 */
public class SftpFileSystems {
	
	private SftpFileSystems() {
	}

	/**
	 * Create a new file system given an existing {@link SshClient} and using
	 * the default directory (usually the users home directory) as the root 
	 * of the file system.
	 * 
	 * @param ssh ssh instance
	 * @return file system
	 * @throws IOException if file system cannot be created
	 */
	public static FileSystem newFileSystem(SshClient ssh) throws IOException {
		try {
			return newFileSystem(SftpClientBuilder.create().withClient(ssh).build());
		} catch (SshException | PermissionDeniedException e) {
			throw new IOException("Failed to create file system.", e);
		}
	}

	/**
	 * Create a new file system given an existing {@link SshClient} and using
	 * the default directory (usually the users home directory) as the root 
	 * of the file system.
	 * 
	 * @param ssh ssh instance
	 * @param path path
	 * @return file system
	 * @throws IOException if file system cannot be created
	 */
	public static FileSystem newFileSystem(SshClient ssh, Path path) throws IOException {
		try {
			return newFileSystem(SftpClientBuilder.create().withClient(ssh).build(), path);
		} catch (SshException | PermissionDeniedException e) {
			throw new IOException("Failed to create file system.", e);
		}
	}

	/**
	 * Create a new file system given an existing {@link SftpClient} and using
	 * the default directory (usually the users home directory) as the root 
	 * of the file system.
	 * 
	 * @param sftp sftp instance
	 * @return file system
	 * @throws IOException if file system cannot be created
	 */
	public static FileSystem newFileSystem(SftpClient sftp) throws IOException {
		try {
			return newFileSystem(sftp, Paths.get(sftp.pwd()));
		} catch (SftpStatusException | SshException e) {
			throw new IOException("Failed to create file system.", e);
		}
	}

	/**
	 * Create a new file system given an existing {@link SftpClient} and using
	 * a specified remote directory as the root of the file system. 
	 * 
	 * @param sftp sftp instance
	 * @param path path of remote root.
	 * @return file system
	 * @throws IOException if file system cannot be created
	 */
	public static FileSystem newFileSystem(SftpClient sftp, Path path) throws IOException {
		return newFileSystem(sftp, path.toString());
	}

	/**
	 * Create a new file system given an existing {@link SftpClient} and using
	 * a specified remote directory as the root of the file system. 
	 * 
	 * @param sftp sftp instance
	 * @param path path of remote root.
	 * @return file system
	 * @throws IOException if file system cannot be created
	 */
	public static FileSystem newFileSystem(SftpClient sftp, String path) throws IOException {
		var conx = sftp.getSubsystemChannel().getConnection();
		return FileSystems.newFileSystem(URI.create(String.format(
				"sftp://%s@%s%s%s", conx.getUsername(), 
					Utils.formatHostnameAndPort(
							conx.getRemoteIPAddress(), conx.getRemotePort()), path.equals("") ? "" : "/", path )), 
							Map.of(SftpFileSystemProvider.SFTP_CLIENT, sftp));
	}

	/**
	 * Create a new file system given all configuration via the environment {@link Map}.
	 * 
	 * @param environment configuration of file system
	 * @return file system
	 * @throws IOException if file system cannot be created
	 */
	public static FileSystem newFileSystem(Map<String, ?> environment) throws IOException {
		return FileSystems.newFileSystem(URI.create("sftp:////"), environment);
	}
}
