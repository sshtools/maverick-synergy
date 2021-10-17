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

package com.sshtools.server.callback.commands;

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
		String path = String.format("sftp://%s/", remoteConnection.getUUID());
		vff.getMountManager().mount(new VirtualMountTemplate(
				"/" + remoteConnection.getUsername(), path,
					new VFSFileFactory(m, opts, path), false), false);
	}
}
