/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
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
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.common.sftp.extensions.multipart;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.AbstractFileSystem;
import com.sshtools.common.sftp.MultipartTransfer;
import com.sshtools.common.sftp.MultipartTransferRegistry;
import com.sshtools.common.sftp.SftpExtension;
import com.sshtools.common.sftp.SftpSubsystem;
import com.sshtools.common.sftp.TransferEvent;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.UnsignedInteger32;

public class OpenMultipartFileExtension implements SftpExtension {

	public static final String EXTENSION_NAME = "open-part-file@sshtools.com";
	
	@Override
	public void processMessage(ByteArrayReader bar, int requestId, SftpSubsystem sftp) {
		
		byte[] transaction = null;
		String partId = null;
		
		Date started = new Date();
		
		try {

			AbstractFileSystem fs = sftp.getFileSystem();
			
			transaction = bar.readBinaryString();
			
			MultipartTransfer t = MultipartTransferRegistry.getTransfer(fs.handleToString(transaction));
			
			partId = bar.readString(sftp.getCharsetEncoding());
				
			byte[] handle = fs.openPart(fs.handleToString(transaction), partId);
			
			TransferEvent evt = new TransferEvent();
			evt.setPath(t.getPath() + "/" + partId);
			evt.setNfs(fs);
			evt.setHandle(handle);
			evt.setForceClose(true);
			evt.setExists(t.getExists());
			evt.setFlags(new UnsignedInteger32(AbstractFileSystem.OPEN_WRITE));
			evt.setKey(fs.handleToString(handle));	
			sftp.addTransferEvent(fs.handleToString(handle), evt);
			
			sftp.sendHandleMessage(requestId, handle);
		
		} catch (FileNotFoundException ioe) {
			sftp.sendStatusMessage(requestId, SftpSubsystem.STATUS_FX_NO_SUCH_FILE, ioe.getMessage());
		} catch (IOException ioe2) {
			sftp.sendStatusMessage(requestId, SftpSubsystem.STATUS_FX_FAILURE, ioe2.getMessage());
		} catch (PermissionDeniedException pde) {
			sftp.sendStatusMessage(requestId, SftpSubsystem.STATUS_FX_PERMISSION_DENIED,
					pde.getMessage());
		} finally {
			bar.close();
		}
	}
	
	@Override
	public boolean supportsExtendedMessage(int messageId) {
		return false;
	}

	@Override
	public void processExtendedMessage(ByteArrayReader msg, SftpSubsystem sftp) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isDeclaredInVersion() {
		return true;
	}

	@Override
	public byte[] getDefaultData() {
		return new byte[] { };
	}


	@Override
	public String getName() {
		return EXTENSION_NAME;
	}

}
