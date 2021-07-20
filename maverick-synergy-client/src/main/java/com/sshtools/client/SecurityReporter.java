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

import java.io.IOException;

import com.sshtools.common.ssh.SecureComponent;
import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshException;

public class SecurityReporter {

	public static void main(String[] args) throws SshException, IOException {
		
		
		SshClientContext context = new SshClientContext(SecurityLevel.WEAK);
		
		
		System.out.println(String.format("%-15s%-50s%s", "Security Level", "Algorithm", "Score"));
		System.out.println("#####################################################################################");
		System.out.println("Ciphers");
		System.out.println("-------");
		for(String name : context.supportedCiphersCS().order()) {
			SecureComponent obj = (SecureComponent) context.supportedCiphersCS().getInstance(name);
			System.out.println(String.format("%-15s%-50s%d", obj.getSecurityLevel().name(), obj.getAlgorithm(), obj.getPriority()));
		}
		
		System.out.println();
		System.out.println("Macs");
		System.out.println("----");
		for(String name : context.supportedMacsCS().order()) {
			SecureComponent obj = (SecureComponent) context.supportedMacsCS().getInstance(name);
			System.out.println(String.format("%-15s%-50s%d", obj.getSecurityLevel().name(), obj.getAlgorithm(), obj.getPriority()));
		}
		
		System.out.println();
		System.out.println("Public Keys");
		System.out.println("-----------");
		for(String name : context.supportedPublicKeys().order()) {
			SecureComponent obj = (SecureComponent) context.supportedPublicKeys().getInstance(name);
			System.out.println(String.format("%-15s%-50s%d", obj.getSecurityLevel().name(), obj.getAlgorithm(), obj.getPriority()));
		}
		
		System.out.println();
		System.out.println("Key Exchange");
		System.out.println("------------");
		
		for(String name : context.supportedKeyExchanges().order()) {
			SecureComponent obj = (SecureComponent) context.supportedKeyExchanges().getInstance(name);
			System.out.println(String.format("%-15s%-50s%d", obj.getSecurityLevel().name(), obj.getAlgorithm(), obj.getPriority()));
		}

	}

}
