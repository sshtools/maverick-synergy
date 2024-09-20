package com.sshtools.vsession.commands.ssh;

/*-
 * #%L
 * Virtual Sessions
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SshClientOptionsExtractor {


	private static Set<String> SSH_SINGLE_OPTIONS = new HashSet<String>(Arrays.asList("46AaCfGgKkMNnqsTtVvXxYy".split("")));
	private static Set<String> SSH_DOUBLE_OPTIONS = new HashSet<String>(Arrays.asList("BbcDEeFIiJLlmOopQRSWw".split("")));
	
	private static Set<String> SSH_CUSTOM_SINGLE_OPTIONS = new HashSet<String>(Arrays.asList("".split("")));
	private static Set<String> SSH_CUSTOM_DOUBLE_OPTIONS = new HashSet<String>(Arrays.asList("S".split("")));
	
	private static String CUSTOM_OPTION_START = "J";
	private static String CUSTOM_OPTION_STARTER = String.format("-%s", CUSTOM_OPTION_START);
	
	/**
	 * Parses options as mentioned in manual pages for ssh.
	 * 
	 * ssh [-46AaCfGgKkMNnqsTtVvXxYy] [-B bind_interface] [-b bind_address]
         [-c cipher_spec] [-D [bind_address:]port] [-E log_file]
         [-e escape_char] [-F configfile] [-I pkcs11] [-i identity_file]
         [-J destination] [-L address] [-l login_name] [-m mac_spec]
         [-O ctl_cmd] [-o option] [-p port] [-Q query_option] [-R address]
         [-S ctl_path] [-W host:port] [-w local_tun[:remote_tun]] destination
         [command]
         
	 * @param values
	 * @return
	 */
	public static int extractSshCommandLineFromExecuteCommand(String[] values) {
		// the arguments starts with ssh client 'ssh' followed by arguments passed, hence we start from 1
		int i = 1;
		
		if (values == null || values.length < 2) {
			throw new IllegalArgumentException("Not enough arguments!");
		}
		
		if (!values[1].startsWith("-")) {
			// seems no option
			return i;
		}
		
		while(i < values.length) {
			// after option jumping most likely we have ended up at host
			// this will happen when host is last argument and no execute command present
			if (i + 1 > (values.length-1)) {
				break;
			}
			// we are actually trying to figure out pairs -o something or singular option -4
			// for pairs we look at i and i +1
			String option = values[i];
			String optionStripped = stripOption(option);
			String optionValue = values[i + 1];
			
			if (option.startsWith("-") && SSH_SINGLE_OPTIONS.contains(optionStripped)) {
				// -4 -C
				i+=1; // we have single value option hence jump 1
			} else if (option.startsWith(CUSTOM_OPTION_STARTER) && option.length() == 2 
					&& SSH_CUSTOM_SINGLE_OPTIONS.contains(optionStripped)) {
				// -4 -C
				i+=1; // we have single value option hence jump 1
			} else if (option.startsWith("-") && SSH_DOUBLE_OPTIONS.contains(optionStripped)){
				//-c blowsfish -m hmac-sha1
				i+=2; // we have pair hence jump 2
			} else if (option.startsWith(CUSTOM_OPTION_STARTER) && option.length() == 2 
					&& SSH_CUSTOM_DOUBLE_OPTIONS.contains(optionStripped)){
				//-c blowsfish -m hmac-sha1
				i+=2; // we have pair hence jump 2
			} else if (!option.startsWith("-") && !optionValue.startsWith("-")) {
				// something like => ssh ....... localhost ls (both localhost and ls do not start with -)
				break; // we have host and post that we have some possible command
			} else {
				throw new IllegalStateException("Cannot tokenize options.");
			}
			
		}
		
		return i;
	}

	private static String stripOption(String option) {
		if (option.startsWith(CUSTOM_OPTION_STARTER)) {
			return option.replaceAll(CUSTOM_OPTION_STARTER, "");
		}
		return option.replaceAll("-", "");
	}
}
