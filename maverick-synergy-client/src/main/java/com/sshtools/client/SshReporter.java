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

package com.sshtools.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;

import com.sshtools.client.sftp.SftpClient;
import com.sshtools.client.sftp.TransferCancelledException;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.ChannelOpenException;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.IOUtils;

public class SshReporter {

	public static void main(String[] args) throws SshException, IOException, SftpStatusException, ChannelOpenException, TransferCancelledException, PermissionDeniedException {
		
		String hostname;
		int port = 22;
		String username;
		String password;
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		if(args.length >= 4) {
			hostname = args[0];
			port = Integer.parseInt(args[1]);
			username = args[2];
			password = args[3];
		} else {
			
			System.out.print("Hostname: ");
			hostname = reader.readLine();
			
			int idx = hostname.indexOf(":");
			if(idx > -1) {
				hostname = hostname.substring(0,  idx);
				port = Integer.parseInt(hostname.substring(idx+1));
			}
			
			System.out.print("Username: ");
			username = reader.readLine();
			
			System.out.print("Password: ");
			password = reader.readLine();
			
		}
		
		SshConnection cfg = SshCompatibilityUtils.getRemoteConfiguration(hostname, port);
		
		System.out.println(String.format("%s on port %d identifies as %s", hostname, port, cfg.getRemoteIdentification()));

		System.out.println("Key exchanges");
		for(String obj : cfg.getRemoteKeyExchanges()) {
			System.out.println(String.format("   %s", obj));
		}
		
		System.out.println("Host keys");
		for(String obj : cfg.getRemotePublicKeys()) {
			System.out.println(String.format("   %s", obj));
		}
		
		System.out.println("Ciphers");
		for(String obj : cfg.getRemoteCiphersCS()) {
			System.out.println(String.format("   %s", obj));
		}
		
		System.out.println("Macs");
		for(String obj : cfg.getRemoteMacsCS()) {
			System.out.println(String.format("   %s", obj));
		}
		
		System.out.println("Compression");
		for(String obj : cfg.getRemoteCompressionsCS()) {
			System.out.println(String.format("   %s", obj));
		}
		
		reportNegotiated("Default", cfg);
		
		String size = "250MB";
		generateLargeFile("file.dat", size);
		
		probeSFTP(SshCompatibilityUtils.getRemoteClient(hostname, port, username, password, true));
		
		reportSFTP("SFTP with TCP No Delay 32k blocksize with 16 max requests", "file.dat",size, 32768, 16, SshCompatibilityUtils.getRemoteClient(hostname, port, username, password, true));
		reportSFTP("SFTP without TCP No Delay 32k blocksize with 16 max requests", "file.dat", size, 32768, 16, SshCompatibilityUtils.getRemoteClient(hostname, port, username, password, false));
		
		reportSFTP("SFTP with TCP No Delay 16k blocksize with 16 max requests", "file.dat",size, 16384, 16, SshCompatibilityUtils.getRemoteClient(hostname, port, username, password, true));
		reportSFTP("SFTP without TCP No Delay 16k blocksize with 16 max requests", "file.dat", size, 16384, 16, SshCompatibilityUtils.getRemoteClient(hostname, port, username, password, false));
		
		reportSFTP("SFTP with TCP No Delay 8k blocksize with 16 max requests", "file.dat",size, 8192, 16, SshCompatibilityUtils.getRemoteClient(hostname, port, username, password, true));
		reportSFTP("SFTP without TCP No Delay 8k blocksize with 16 max requests", "file.dat", size, 8192, 16, SshCompatibilityUtils.getRemoteClient(hostname, port, username, password, false));
		
//		reportSCP("SCP with TCP No Delay", "file.dat", size, SshCompatibilityUtils.getRemoteClient(hostname, port, username, password, true));
//		reportSCP("SCP with without TCP No Delay", "file.dat", size, SshCompatibilityUtils.getRemoteClient(hostname, port, username, password, false));
		
		System.exit(0);
	}
	
	private static void probeSFTP(SshClient ssh) throws SftpStatusException, SshException, ChannelOpenException, PermissionDeniedException, IOException {
		
		try(SftpClient sftp = new SftpClient(ssh)) {
		
			System.out.println("##### SFTP Configuration");
			 
			System.out.println("Local window: " + sftp.getSubsystemChannel().getMaximumLocalWindowSize());
			System.out.println("Local packet: " + sftp.getSubsystemChannel().getMaximumLocalPacketLength());
			System.out.println("Remote window: " + sftp.getSubsystemChannel().getMaximumRemoteWindowSize());
			System.out.println("Remote packet: " + sftp.getSubsystemChannel().getMaximumRemotePacketLength());
			
			System.out.println("#####");
		}
		
		ssh.disconnect();
		
		
	}

//	private static void reportSCP(String testName, String filename, String size, SshClient ssh) throws SftpStatusException, SshException, ChannelOpenException, FileNotFoundException, TransferCancelledException {
//		
//		System.out.println("##### " + testName);
//		
//		ScpClient scp = new ScpClient(ssh);
//		
//		File local = new File(System.getProperty("user.dir"), filename);
//		
//        System.out.println("Uploading " + size + " File");
//        long started = System.currentTimeMillis();
//        scp.put(local.getAbsolutePath(), filename, false);
//        long ended = System.currentTimeMillis();
//        System.out.println("Upload took " + ((double)(ended-started)/ 1000) + " seconds");
//        
//        System.out.println("Downloading " + size + " File");
//        started = System.currentTimeMillis();
//        scp.get(local.getAbsolutePath(), filename, false);
//        ended = System.currentTimeMillis();
//        System.out.println("Download took " + ((double)(ended-started)/ 1000) + " seconds");
//		
//        ssh.disconnect();
//        
//        System.out.println("#####");
//	}

	private static void reportSFTP(String testName, String filename, String size, int blocksize, int maxRequests, SshClient ssh) throws SftpStatusException, SshException, ChannelOpenException, TransferCancelledException, PermissionDeniedException, IOException {
		
		System.out.println("##### " + testName);
		
		try(SftpClient sftp = new SftpClient(ssh)) {
		
			System.out.println("Block size: " + blocksize);
			System.out.println("Max Requests: " + maxRequests);
			
			sftp.setBlockSize(blocksize);
			
			sftp.lcd(System.getProperty("user.dir"));
			
	        System.out.println("Uploading " + size + " File");
	        long started = System.currentTimeMillis();
	        sftp.put(filename);
	        long ended = System.currentTimeMillis();
	        System.out.println("Upload took " + ((double)(ended-started)/ 1000) + " seconds");
	        
	        System.out.println("Optimized Block: " + System.getProperty("maverick.write.optimizedBlock"));
	        System.out.println("Round Trip: " + System.getProperty("maverick.write.blockRoundtrip"));
	        
	        System.out.println("Downloading " + size + " File");
	        started = System.currentTimeMillis();
	        sftp.get(filename);
	        ended = System.currentTimeMillis();
	        System.out.println("Download took " + ((double)(ended-started)/ 1000) + " seconds");
		}
		
        ssh.disconnect();
        
        System.out.println("Optimized Block: " + System.getProperty("maverick.read.optimizedBlock"));
        System.out.println("Final Block: " + System.getProperty("maverick.read.finalBlock"));
        System.out.println("Round Trip: " + System.getProperty("maverick.read.blockRoundtrip"));
        
        System.out.println("#####");
	}

	private static void generateLargeFile(String name, String size) throws IOException {
		
		System.out.println("Generating " + size + " file");
		File f = new File(name);
		byte[] tmp = new byte[32768];
		new Random().nextBytes(tmp);
		long s = IOUtils.fromByteSize(size);
		try(FileOutputStream out = new FileOutputStream(f)) {
			for(int i=0;i<s;i+=tmp.length) {
				out.write(tmp);
				out.flush();
			}
		}

        System.out.println("#####");
	}

	private static void reportNegotiated(String name, SshConnection cfg) {
		System.out.println(String.format("%s configuration", name));
		System.out.println(String.format("Key exchange: %s", cfg.getKeyExchangeInUse()));
		System.out.println(String.format("Host key    : %s", cfg.getHostKey().getAlgorithm()));
		System.out.println(String.format("Cipher      : %s,%s", cfg.getCipherInUseCS(), cfg.getCipherInUseSC()));
		System.out.println(String.format("Mac         : %s,%s", cfg.getMacInUseCS(), cfg.getMacInUseSC()));
		System.out.println(String.format("Compression : %s,%s", cfg.getCompressionInUseCS(), cfg.getCompressionInUseSC()));
        System.out.println("#####");
	}
}
