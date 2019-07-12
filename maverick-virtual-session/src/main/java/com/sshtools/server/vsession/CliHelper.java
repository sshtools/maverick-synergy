/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.server.vsession;

public class CliHelper {

	public static boolean hasShortOption(String[] args, char opt) {
				
		for(String arg : args) {
			if(arg.startsWith("-") && !arg.startsWith("--")) {
				if(arg.length()==2 && arg.indexOf(opt) == 1) {
					return true;
				}
			}
		}
		
		return false;
	}
	
    public static boolean hasLongOption(String[] args, String opt) {
		
    	while(!opt.startsWith("--")) {
    		opt = "-" + opt;
    	}
		
		for(String arg : args) {
			if(arg.equals(opt)) {
				return true;
			}
		}
		return false;
	}

    public static String getLongValue(String[] args, String opt) throws UsageException {
		
    	while(!opt.startsWith("--")) {
    		opt = "-" + opt;
    	}
    	
		for(int i=0; i<args.length; i++) {
			String arg = args[i];
			if(arg.startsWith("--")) {
				if(arg.equals(opt)) {
					if(args.length > i+i) {
						return args[i+1];
					} else {
						throw new UsageException("Missing value for long option " + opt);
					}
				}
			}
		}
		
		throw new UsageException("Missing long option " + opt);
	}

	public static String getShortValue(String[] args, char opt) throws UsageException {
		
		for(int i=0; i<args.length; i++) {
			String arg = args[i];
			if(arg.startsWith("-") && !arg.startsWith("--")) {
				if(arg.length()==2 && arg.indexOf(opt) == 1) {
					if(args.length > i+i) {
						return args[i+1];
					} else {
						throw new UsageException("Missing value for option " + opt);
					}
				}
			}
		}
		
		throw new UsageException("Missing option " + opt);
	}

}
