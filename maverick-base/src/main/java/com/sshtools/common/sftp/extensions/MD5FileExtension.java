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

import com.sshtools.common.logger.Log;
import com.sshtools.common.sftp.SftpSubsystem;
import com.sshtools.common.util.ByteArrayReader;

public class MD5FileExtension extends AbstractMD5Extension {

	public static final String EXTENSION_NAME = "md5-hash";
	
	public MD5FileExtension() {
		super(EXTENSION_NAME);
	}

	@Override
	public void processMessage(ByteArrayReader msg, int requestId, SftpSubsystem sftp) {

        try {
        	String filename = msg.readString();
	        long startOffset = msg.readUINT64().longValue();
	        long length = msg.readUINT64().longValue();
	        byte[] quickCheckHash = msg.readBinaryString();
	        byte[] hashValue;
	        
            hashValue = doMD5Hash(filename, startOffset, length, quickCheckHash, sftp);
            
            sendReply(requestId, hashValue, sftp);
	        
        } catch (Exception e) {
        	Log.error("Failed to process EXT_MD5_HASH", e);
            sftp.sendStatusMessage(requestId, SftpSubsystem.STATUS_FX_FAILURE, e.getMessage());
            return;
        } 
	}
}
