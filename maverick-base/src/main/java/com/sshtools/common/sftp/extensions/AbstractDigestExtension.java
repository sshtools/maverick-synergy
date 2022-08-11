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

package com.sshtools.common.sftp.extensions;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.AbstractFileSystem;
import com.sshtools.common.sftp.InvalidHandleException;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.sftp.SftpSubsystem;
import com.sshtools.common.ssh.Packet;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.Digest;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.common.ssh.components.jce.MD5Digest;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.UnsignedInteger32;
import com.sshtools.common.util.UnsignedInteger64;

public abstract class AbstractDigestExtension extends AbstractSftpExtension {

    static final Map<String,String> ALGOS;
    static {
		ALGOS = new HashMap<>();
		ALGOS.put("md5", JCEComponentManager.JCE_MD5);
		ALGOS.put("sha1", JCEComponentManager.JCE_SHA1);
		ALGOS.put("sha256", JCEComponentManager.JCE_SHA256);
		ALGOS.put("sha384", JCEComponentManager.JCE_SHA384);
		ALGOS.put("sha512", JCEComponentManager.JCE_SHA512);
	}
    
	AbstractDigestExtension(String extensionName) {
		super(extensionName, false);
	}

	protected byte[] doHash(String algorithm, String filename, long startOffset, long length,SftpSubsystem sftp) throws FileNotFoundException, PermissionDeniedException, IOException, SshException, InvalidHandleException {
		
		AbstractFileSystem fs = sftp.getFileSystem();
		byte[] handle = fs.openFile(filename, new UnsignedInteger32(AbstractFileSystem.OPEN_READ), null);
		try {
			return doHash(algorithm, handle, startOffset, length, sftp);
		} finally {
			fs.closeFile(handle);
			fs.freeHandle(handle);
		}
	}
	
	protected byte[] doHash(String algorithm, byte[] handle, long startOffset, long length, SftpSubsystem sftp) throws SshException, EOFException, InvalidHandleException, IOException, PermissionDeniedException {
		
		
		byte[] tmp = new byte[32768];
		AbstractFileSystem fs = sftp.getFileSystem();

		Digest digest = (Digest) JCEComponentManager.getInstance().supportedDigests().getInstance(ALGOS.get(algorithm));
		int read;
		long total = 0L;
		do {
			read = fs.readFile(handle, new UnsignedInteger64(startOffset), 
					tmp, 
					0, 
					length==0 ? tmp.length : Math.min(tmp.length, (int) (length - total)));
			if(read > 0) {
				digest.putBytes(tmp, 0, read);
				total += read;
				startOffset += read;
			} else {
				if(total == 0) {
					throw new EOFException();
				}
			}
		} while(read > -1 && (length==0 || total < length));
		return digest.doFinal();
	}
	
	@Override
	public boolean supportsExtendedMessage(int messageId) {
		return false;
	}

	@Override
	public void processExtendedMessage(ByteArrayReader msg, SftpSubsystem sftp) {
	}

}
