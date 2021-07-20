
package com.sshtools.server.vsession.commands.sftp;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.cli.Option;

public class SftpClientOptions {

	private static Map<String, SftpClientOption> optionsMap = new LinkedHashMap<>();
	private static List<Option> options;
	
	static {
		assembleOptions();
	}
	
	private static void assembleOptions() {
		
		optionsMap.put(Port.PORT_OPTION, Port.instance);
		optionsMap.put(Compression.COMPRESSION_OPTION, Compression.instance);
		optionsMap.put(IdentityFile.IDENTITY_FILE_OPTION, IdentityFile.instance);
		optionsMap.put(CipherSpec.CIPHER_SPEC_OPTION, CipherSpec.instance);
		
		options = optionsMap.values().stream().map(o -> o.option()).collect(Collectors.toList());
	}

	public static Option[] getOptions() {
		return options.toArray(new Option[0]);
	}

	static abstract class SftpClientOption {
		abstract Option option();
	}
	
	public static class Port extends SftpClientOption {
		
		public static final Port instance = new Port();
		
		public static String PORT_OPTION = "P";
		
		private static String description = "Specifies the port to connect to on the remote host.";
		
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
	
	public static class Compression extends SftpClientOption {
		
		public static final Compression instance = new Compression();
		
		public static String COMPRESSION_OPTION = "C";
		
		private static String description = "Requests compression of all data (including stdin, stdout," + 
				" stderr, and data for forwarded X11, TCP and UNIX-domain connections)." + 
				" The compression algorithm is the same used by gzip(1)." + 
				" Compression is desirable on modem lines and other slow connections," + 
				" but will only slow down things on fast networks.  The" + 
				" default value can be set on a host-by-host basis in the configuration" + 
				" files; see the Compression option.";
		
		private Compression() {}
		
		@Override
		Option option() {
			return Option
					.builder(COMPRESSION_OPTION)
					.hasArg(false)
					.desc(description)
					.build();
		}
		
	}
	
	public static class IdentityFile extends SftpClientOption {
		
		public static final IdentityFile instance = new IdentityFile();
		
		public static String IDENTITY_FILE_OPTION = "i";
		
		private static String description = "Selects a file from which the identity (private key) for public key authentication is " + 
					" read.  The default is ~/.ssh/id_dsa, ~/.ssh/id_ecdsa, ~/.ssh/id_ed25519 and ~/.ssh/id_rsa. " + 
					" Identity files may also be specified on a per-host basis in the configuration file.  It is " + 
					" possible to have multiple -i options (and multiple identities specified in configuration " + 
					" files).  If no certificates have been explicitly specified by the CertificateFile directive," + 
					" ssh will also try to load certificate information from the filename obtained by " + 
					" appending -cert.pub to identity filenames.";
		
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
	
	public static class CipherSpec extends SftpClientOption {
		
		public static final CipherSpec instance = new CipherSpec();
		
		public static String CIPHER_SPEC_OPTION = "c";
		
		private static String description = "Selects the cipher specification for encrypting the session." + 
				" cipher_spec is a comma-separated list of ciphers listed in order" + 
				" of preference.  See the Ciphers keyword in ssh_config(5) for more" + 
				" information.";
		
		private CipherSpec() {}
		
		@Override
		Option option() {
			return Option
					.builder(CIPHER_SPEC_OPTION)
					.hasArg()
					.desc(description)
					.argName("cipher_spec")
					.build();
		}
		
	}
}
