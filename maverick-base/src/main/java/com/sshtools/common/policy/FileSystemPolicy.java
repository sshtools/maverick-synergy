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

package com.sshtools.common.policy;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.permissions.Permissions;
import com.sshtools.common.sftp.SftpExtension;
import com.sshtools.common.sftp.SftpExtensionFactory;
import com.sshtools.common.sftp.extensions.DefaultSftpExtensionFactory;
import com.sshtools.common.ssh.SshConnection;

public class FileSystemPolicy extends Permissions {

	long connectionUploadQuota = -1;
	FileFactory fileFactory;
	String sftpCharsetEncoding = "UTF-8";
	boolean allowZeroLengthFileUpload = true;
	boolean sftpVersion4Enabled = true;
	int sftpVersion = 4;
	boolean sftpReadWriteEvents = false;
	boolean scpReadWriteEvents = false;
	int maxConcurrentTransfers = 50;
	int maximumSftpRequests = 10;
	String sftpLongnameDateFormat = "MMM dd  yyyy";
	String sftpLongnameDateFormatWithTime = "MMM dd HH:mm";
	List<SftpExtensionFactory> sftpExtensionFactories = new ArrayList<SftpExtensionFactory>();
	boolean closeFileBeforeFailedTransferEvents = false;
	boolean mkdirParentMustExist = true;
	
	private int sftpMaxPacketSize = 65536;
	private int sftpMaxWindowSize = 1024000;
	private int sftpMinWindowSize = 131072;
	
	public FileSystemPolicy() {
		sftpExtensionFactories.add(new DefaultSftpExtensionFactory());
	}
	
	public long getConnectionUploadQuota() {
		return connectionUploadQuota;
	}
	
	public void setConnectionUploadQuota(long connectionUploadQuota) {
		this.connectionUploadQuota = connectionUploadQuota;
	}
	
	public boolean hasUploadQuota() {
		return connectionUploadQuota > -1;
	}
	
	/**
	 * Get the current encoding value for filenames in SFTP sessions.
	 * 
	 * @return String
	 */
	public String getSFTPCharsetEncoding() {
		return sftpCharsetEncoding;
	}

	/**
	 * Set the default encoding for filenames in SFTP sessions. The default
	 * encoding for the currently supported SFTP protocol is ISO-8859-1.
	 * 
	 * @param sftpCharsetEncoding
	 *            String
	 */
	public void setSFTPCharsetEncoding(String sftpCharsetEncoding) {
		this.sftpCharsetEncoding = sftpCharsetEncoding;
	}
	
	/**
	 * Set the file factory for this context.
	 * @param fileFactory
	 */
	public void setFileFactory(FileFactory fileFactory) {
		this.fileFactory = new CachingFileFactory(fileFactory);
	}
	
	/**
	 * Get the file factory for this context.
	 * @return
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public FileFactory getFileFactory() {
		return fileFactory;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean isAllowZeroLengthFileUpload() {
		return allowZeroLengthFileUpload;
	}

	/**
	 * 
	 * @param allowZeroLengthFileUpload
	 */
	public void setAllowZeroLengthFileUpload(boolean allowZeroLengthFileUpload) {
		this.allowZeroLengthFileUpload = allowZeroLengthFileUpload;
	}
	
	public void setMaxConcurrentTransfers(int maxConcurrentTransfers) {
		this.maxConcurrentTransfers = maxConcurrentTransfers;
	}

	public int getMaxConcurrentTransfers() {
		return maxConcurrentTransfers;
	}
	
	public void setSupportedSFTPVersion(int sftpVersion) {
		if(sftpVersion < 1 || sftpVersion > 4) {
			throw new IllegalArgumentException("SFTP version must be between 1 and 4");
		}
		this.sftpVersion = sftpVersion;
	}
	
	public int getSFTPVersion() {
		return sftpVersion;
	}
	
	public void setSFTPReadWriteEvents(boolean sftpReadWriteEvents) {
		this.sftpReadWriteEvents = sftpReadWriteEvents;
	}

	public boolean isSFTPReadWriteEvents() {
		return sftpReadWriteEvents;
	}

	public void setSCPReadWriteEvents(boolean scpReadWriteEvents) {
		this.scpReadWriteEvents = scpReadWriteEvents;
	}

	public boolean isSCPReadWriteEvents() {
		return scpReadWriteEvents;
	}
	
	public int getMaximumNumberOfAsyncSFTPRequests() {
		return maximumSftpRequests;
	}
	
	public void setMaximumNumberofAsyncSFTPRequests(int maximumSftpRequests) {
		this.maximumSftpRequests = maximumSftpRequests;
	}

	public String getSFTPLongnameDateFormat() {
		return sftpLongnameDateFormat; //"MMM dd yyyy";
	}

	public String getSFTPLongnameDateFormatWithTime() {
		return sftpLongnameDateFormatWithTime; //"MMM dd HH:mm";
	}

	public SftpExtension getSFTPExtension(String requestName) {
		for(SftpExtensionFactory factory : sftpExtensionFactories) {
			if(factory.getSupportedExtensions().contains(requestName)) {
				return factory.getExtension(requestName);
			}
		}
		return null;
	}

	public Collection<SftpExtensionFactory> getSFTPExtensionFactories() {
		return sftpExtensionFactories;
	}

	public boolean isSFTPCloseFileBeforeFailedTransferEvents() {
		return closeFileBeforeFailedTransferEvents;
	}
	
	public void setSFTPCloseFileBeforeFailedTransferEvents(boolean closeFileBeforeFailedTransferEvents) {
		this.closeFileBeforeFailedTransferEvents = closeFileBeforeFailedTransferEvents;
	}
	public int getSftpMaxPacketSize() {
		return sftpMaxPacketSize;
	}
	public void setSftpMaxPacketSize(int sftpMaxPacketSize) {
		this.sftpMaxPacketSize = sftpMaxPacketSize;
	}
	public int getSftpMaxWindowSize() {
		return sftpMaxWindowSize;
	}
	public void setSftpMaxWindowSize(int sftpMaxWindowSize) {
		this.sftpMaxWindowSize = sftpMaxWindowSize;
	}
	public int getSftpMinWindowSize() {
		return sftpMinWindowSize;
	}
	public void setSftpMinWindowSize(int sftpMinWindowSize) {
		this.sftpMinWindowSize = sftpMinWindowSize;
	}

	class CachingFileFactory implements FileFactory {

		AbstractFileFactory<?> ff = null;
		FileFactory fileFactory;
		
		CachingFileFactory(FileFactory fileFactory) {
			this.fileFactory = fileFactory;
		}
		
		@Override
		public AbstractFileFactory<?> getFileFactory(SshConnection con) 
				throws IOException, PermissionDeniedException {
			if(Objects.nonNull(ff)) {
				return ff;
			}
			return ff = fileFactory.getFileFactory(con);
		}
		
	}

	public void setMkdirParentMustExist(boolean mkdirParentMustExist) {
		this.mkdirParentMustExist = mkdirParentMustExist;
	}
	
	public boolean isMkdirParentMustExist() {
		return mkdirParentMustExist;
	}
}
