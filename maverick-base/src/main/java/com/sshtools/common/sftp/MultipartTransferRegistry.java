package com.sshtools.common.sftp;

import java.util.HashMap;
import java.util.Map;

public class MultipartTransferRegistry {

	static Map<String,MultipartTransfer> transfers = new HashMap<>(); 
	
	public static void registerTransfer(MultipartTransfer transfer) {
		transfers.put(transfer.getUuid(), transfer);
	}
	
	public static MultipartTransfer getTransfer(String uuid) {
		return transfers.get(uuid);
	}
	
	public static void removeTransfer(String uuid) {
		transfers.remove(uuid);
	}
}
