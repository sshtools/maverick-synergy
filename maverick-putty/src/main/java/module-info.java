
import com.sshtools.common.publickey.SshPrivateKeyProvider;
import com.sshtools.common.publickey.putty.PuTTYPrivateKeyProvider;

module com.sshtools.common.publickey.putty {
	requires com.sshtools.maverick.base;
	requires org.bouncycastle.provider;
	
	provides SshPrivateKeyProvider with PuTTYPrivateKeyProvider;
}
