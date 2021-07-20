
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
