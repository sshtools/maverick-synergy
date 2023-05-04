import com.sshtools.common.ssh.components.SshPublicKeyFactory;
import com.sshtools.common.ssh.x509.SshX509DsaPublicKey;
import com.sshtools.common.ssh.x509.SshX509DsaPublicKeyRfc6187;
import com.sshtools.common.ssh.x509.SshX509EcdsaSha2Nist256Rfc6187;
import com.sshtools.common.ssh.x509.SshX509EcdsaSha2Nist384Rfc6187;
import com.sshtools.common.ssh.x509.SshX509EcdsaSha2Nist521Rfc6187;
import com.sshtools.common.ssh.x509.SshX509Rsa2048Sha256Rfc6187;
import com.sshtools.common.ssh.x509.SshX509RsaPublicKey;
import com.sshtools.common.ssh.x509.SshX509RsaPublicKeyRfc6187;
import com.sshtools.common.ssh.x509.SshX509RsaSha1PublicKey;

@SuppressWarnings("rawtypes")
open module com.sshtools.common.ssh.x509 {
	requires transitive com.sshtools.maverick.base;
	requires com.sshtools.common.logger;
	requires com.sshtools.common.util;
	exports com.sshtools.common.ssh.x509;
	
	provides SshPublicKeyFactory with 
			SshX509EcdsaSha2Nist256Rfc6187.SshX509EcdsaSha2Nist256Rfc6187Factory,
			SshX509EcdsaSha2Nist384Rfc6187.SshX509EcdsaSha2Nist384Rfc6187Factory,
			SshX509EcdsaSha2Nist521Rfc6187.SshX509EcdsaSha2Nist521Rfc6187Factory,
			SshX509DsaPublicKey.SshX509DsaPublicKeyFactory,
			SshX509Rsa2048Sha256Rfc6187.SshX509Rsa2048Sha256Rfc6187Factory,
			SshX509RsaPublicKey.SshX509RsaPublicKeyFactory,
			SshX509RsaPublicKeyRfc6187.SshX509RsaPublicKeyRfc6187Factory,
			SshX509RsaSha1PublicKey.SshX509RsaSha1PublicKeyFactory,
			SshX509DsaPublicKeyRfc6187.SshX509DsaPublicKeyRfc6187Factory;
}