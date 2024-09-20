package com.sshtools.server.callback.commands;

/*-
 * #%L
 * Callback Server API
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

import java.io.IOException;
import java.util.Objects;

import org.apache.commons.vfs2.CacheStrategy;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.cache.DefaultFilesCache;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;

import com.sshtools.common.files.vfs.VFSFileFactory;
import com.sshtools.common.files.vfs.VirtualFileFactory;
import com.sshtools.common.files.vfs.VirtualMountTemplate;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.policy.FileSystemPolicy;
import com.sshtools.server.callback.Callback;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.VirtualConsole;
import com.sshtools.vfs.sftp.SftpFileProvider;
import com.sshtools.vfs.sftp.SftpFileSystemConfigBuilder;

public class CallbackMount extends CallbackCommand {

	public CallbackMount() {
		super("mount", "Callback", "mount <name>", "Mount the file system of a callback client");
	}

	@Override
	public void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException, UsageException {
		
		if(args.length != 2) {
			throw new UsageException("Invalid number of arguments");
		}
		
		String clientName = args[1];
		Callback remoteConnection = service.getCallbackByUUID(clientName);
		
		if(Objects.isNull(remoteConnection)) {
			console.println(String.format("%s is not currently connected", clientName));
		}
		
		VirtualFileFactory vff = (VirtualFileFactory) console.getContext()
					.getPolicy(FileSystemPolicy.class)
						.getFileFactory().getFileFactory(console.getConnection());

		DefaultFileSystemManager m = new DefaultFileSystemManager();
		m.addProvider("sftp", new SftpFileProvider());
		m.setCacheStrategy(CacheStrategy.ON_RESOLVE);
		m.setFilesCache(new DefaultFilesCache());
		
		FileSystemOptions opts = new FileSystemOptions();
		SftpFileSystemConfigBuilder.getInstance().setSshConnection(opts, remoteConnection.getConnection());
		String path = String.format("sftp://%s/", remoteConnection.getUuid());
		vff.mount(new VirtualMountTemplate(
				"/" + remoteConnection.getUsername(), path,
					new VFSFileFactory(m, opts, path), false), false);
	}
}
