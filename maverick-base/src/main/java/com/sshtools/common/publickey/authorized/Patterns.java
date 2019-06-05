package com.sshtools.common.publickey.authorized;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import com.sshtools.common.net.CIDRNetwork;

/**
 * Implements OpenSSH patterns as defined in http://man.openbsd.org/ssh_config.5#PATTERNS
 * @author lee
 *
 */
public class Patterns {

	public static boolean matchesWithCIDR(Collection<String> patterns, String value) throws IOException {
		
		if(patterns==null || patterns.isEmpty()) {
			return false;
		}

		boolean positiveMatch = false;
		boolean negativeMatch = false;
		
		for(String pattern : patterns) {
			if(pattern.startsWith("!") && !negativeMatch) {
				if(pattern.contains("/")) {
					
					CIDRNetwork cidr = new CIDRNetwork(pattern);
					negativeMatch = cidr.isValidAddressForNetwork(value);
					continue;
				}
				if(value.matches(convertToRegex(pattern.substring(1)))) {
					negativeMatch = true;
				}
			} else if(!positiveMatch) {
				if(pattern.contains("/")) {
					
					CIDRNetwork cidr = new CIDRNetwork(pattern);
					positiveMatch = cidr.isValidAddressForNetwork(value);
					continue;
				}
				if(value.matches(convertToRegex(pattern))) {
					positiveMatch = true;
				}
			}
		}
		
		return !negativeMatch && positiveMatch;
	}
	
	public static boolean matches(Collection<String> patterns, String value) {
		
		if(patterns==null || patterns.isEmpty()) {
			return false;
		}

		boolean positiveMatch = false;
		boolean negativeMatch = false;
		
		for(String pattern : patterns) {
			if(pattern.startsWith("!") && !negativeMatch) {
				if(value.matches(convertToRegex(pattern.substring(1)))) {
					negativeMatch = true;
				}
			} else if(!positiveMatch) {
				if(value.matches(convertToRegex(pattern))) {
					positiveMatch = true;
				}
			}
		}
		
		return !negativeMatch && positiveMatch;
	}
	
	static String convertToRegex(String pattern) {
		
		pattern = pattern.replace(".", "\\.");
		pattern = pattern.replace("*", ".*");
		pattern = pattern.replace("?", ".");
		
		return pattern;
		
	}
	
	public static void main(String[] args) throws IOException {
		
		System.out.println("Expecting true: " + Patterns.matchesWithCIDR(Arrays.asList("!*.dialup.example.com", "*.example.com"), "foo.example.com"));
		System.out.println("Expecting false: " + Patterns.matchesWithCIDR(Arrays.asList("!*.dialup.example.com", "*.example.com"), "foo.dialup.example.com"));
		System.out.println("Expecting false: " + Patterns.matchesWithCIDR(Arrays.asList("!*.dialup.example.com", "*.example.com"), "foo.com"));
		
		
	}
}
