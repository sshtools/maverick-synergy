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
