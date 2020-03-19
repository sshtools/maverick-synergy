package com.sshtools.vsession.commands.ssh;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.cli.Option;

public class SshClientOptions {
	
	private static Map<String, SshClientOption> optionsMap = new LinkedHashMap<>();
	private static List<Option> options;
	
	static {
		assembleOptions();
	}
	
	private static void assembleOptions() {
		
		optionsMap.put(Port.PORT_OPTION, Port.instance);
		optionsMap.put(LoginName.LOGIN_NAME_OPTION, LoginName.instance);
		optionsMap.put(IdentityFile.IDENTITY_FILE_OPTION, IdentityFile.instance);
		
		
		options = optionsMap.values().stream().map(o -> o.option()).collect(Collectors.toList());
	}

	public static Option[] getOptions() {
		return options.toArray(new Option[0]);
	}

	static abstract class SshClientOption {
		abstract Option option();
	}
	
	public static class Port extends SshClientOption {
		
		public static final Port instance = new Port();
		
		public static String PORT_OPTION = "p";
		
		private static String description = "Port to connect to on the remote host."
				+ "  This can be specified on a per-host basis in the configuration file.";
		
		private Port() {}
		
		@Override
		Option option() {
			return Option
					.builder(PORT_OPTION)
					.hasArg()
					.desc(description)
					.argName("port")
					.build();
		}
	}
	
	public static class LoginName extends SshClientOption {
		
		public static final LoginName instance = new LoginName();
			
		public static String LOGIN_NAME_OPTION = "l";
		
		private static String description = "Specifies the user to log in as on the remote machine."
				+ "  This also may be specified on a per-host basis in the configuration file.";
		
		private LoginName() {}
		
		@Override
		Option option() {
			return Option
					.builder(LOGIN_NAME_OPTION)
					.hasArg()
					.desc(description)
					.argName("login_name")
					.build();
		}
	}

	public static class IdentityFile extends SshClientOption {
		
		public static final IdentityFile instance = new IdentityFile();
		
		public static String IDENTITY_FILE_OPTION = "i";
		
		private static String description = "Selects a file from which the identity (private key) for public key authentication is " + 
					"read.  The default is ~/.ssh/id_dsa, ~/.ssh/id_ecdsa, ~/.ssh/id_ed25519 and ~/.ssh/id_rsa. " + 
					"Identity files may also be specified on a per-host basis in the configuration file.  It is " + 
					"possible to have multiple -i options (and multiple identities specified in configuration " + 
					"files).  If no certificates have been explicitly specified by the CertificateFile direc" + 
					"tive, ssh will also try to load certificate information from the filename obtained by " + 
					"appending -cert.pub to identity filenames.";
		
		private IdentityFile() {}
		
		@Override
		Option option() {
			return Option
					.builder(IDENTITY_FILE_OPTION)
					.hasArg()
					.desc(description)
					.argName("identity_file")
					.build();
		}
	}
}
