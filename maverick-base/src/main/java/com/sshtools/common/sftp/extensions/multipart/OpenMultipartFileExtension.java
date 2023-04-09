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
