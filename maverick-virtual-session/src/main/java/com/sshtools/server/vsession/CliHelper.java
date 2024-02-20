package com.sshtools.server.vsession;

import java.util.Objects;

public class CliHelper {

	public static boolean hasOption(String[] args, char shortOpt, String longOpt) {
		return hasShortOption(args, shortOpt) || hasLongOption(args, longOpt);
	}
	
	public static String getValue(String[] args, char shortOpt, String longOpt) throws UsageException {
		return getValue(args, shortOpt, longOpt, null);
	}
	
	public static String getValue(String[] args, char shortOpt, String longOpt, String defaultValue) throws UsageException {
		if(hasShortOption(args, shortOpt)) {
			return getShortValue(args, shortOpt);
		} else if(hasLongOption(args, longOpt)) {
			return getLongValue(args, longOpt);
		} else {
			if(Objects.isNull(defaultValue)) {
				throw new UsageException(String.format("Missing -%c or --%s option", shortOpt, longOpt));
			}
		}
		return defaultValue;
	}
	
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
					if(args.length > i+1) {
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
					if(args.length > i+1) {
						return args[i+1];
					} else {
						throw new UsageException("Missing value for option " + opt);
					}
				}
			}
		}
		
		throw new UsageException("Missing option " + opt);
	}

	public static boolean isOption(String opt, String shortOptions) {
		if(opt.startsWith("--")) {
			return shortOptions.contains(opt.substring(2));
		} else if(opt.startsWith("-")) {
			return shortOptions.contains(opt.substring(1));
		}
		return false;
	}

}
