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
