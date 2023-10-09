
import com.sshtools.common.ssh.compression.SshCompressionFactory;
import com.sshtools.common.zlib.OpenSSHZLibCompression;
import com.sshtools.common.zlib.ZLibCompression;

@SuppressWarnings("rawtypes")
open module com.sshtools.common.zlib {
	requires com.sshtools.maverick.base;
	provides SshCompressionFactory with 
		ZLibCompression.ZLibCompressionFactory, 
		OpenSSHZLibCompression.OpenSSHZLibCompressionFactory;
}
