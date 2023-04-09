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
package com.sshtools.common.sftp;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.util.UnsignedInteger64;

public class Multipart {

	AbstractFile targetFile;
	String transaction;
	String partIdentifier;
	UnsignedInteger64 startPosition;
	UnsignedInteger64 length;
	
	public String getTransactionUUID() {
		return transaction;
	}

	public void setTransaction(String transaction) {
		this.transaction = transaction;
	}
	
	public UnsignedInteger64 getStartPosition() {
		return startPosition;
	}
	
	public void setStartPosition(UnsignedInteger64 startPosition) {
		this.startPosition = startPosition;
	}
	
	public UnsignedInteger64 getLength() {
		return length;
	}
	
	public void setLength(UnsignedInteger64 length) {
		this.length = length;
	}

	public String getPartIdentifier() {
		return partIdentifier;
	}

	public void setPartIdentifier(String partIdentifier) {
		this.partIdentifier = partIdentifier;
	}

	public AbstractFile getTargetFile() {
		return targetFile;
	}
	
	public void setTargetFile(AbstractFile targetFile) {
		this.targetFile = targetFile;
	}
}
