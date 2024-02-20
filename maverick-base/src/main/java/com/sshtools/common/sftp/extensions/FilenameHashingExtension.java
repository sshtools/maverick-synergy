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

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.AbstractFileSystem;
import com.sshtools.common.sftp.SftpSubsystem;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.UnsignedInteger32;

public class FilenameHashingExtension extends FileHashingExtension {

	public static final String EXTENSION_NAME = "check-file-name";
    
	public FilenameHashingExtension() {
		super(EXTENSION_NAME);
	}
	
	protected byte[] getFileHandle(ByteArrayReader msg, SftpSubsystem sftp) throws IOException, PermissionDeniedException {
		String filename = msg.readString();
		AbstractFileSystem fs = sftp.getFileSystem();
		return fs.openFile(filename, new UnsignedInteger32(AbstractFileSystem.OPEN_READ), null);
	}
}
