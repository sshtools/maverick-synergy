
import com.sshtools.client.SshKeyExchangeClientFactory;
import com.sshtools.client.components.Curve25519SHA256Client;
import com.sshtools.client.components.Curve25519SHA256LibSshClient;
import com.sshtools.client.components.DiffieHellmanEcdhNistp256;
import com.sshtools.client.components.DiffieHellmanEcdhNistp384;
import com.sshtools.client.components.DiffieHellmanEcdhNistp521;
import com.sshtools.client.components.DiffieHellmanGroup14Sha1JCE;
import com.sshtools.client.components.DiffieHellmanGroup14Sha256JCE;
import com.sshtools.client.components.DiffieHellmanGroup15Sha512JCE;
import com.sshtools.client.components.DiffieHellmanGroup16Sha512JCE;
import com.sshtools.client.components.DiffieHellmanGroup17Sha512JCE;
import com.sshtools.client.components.DiffieHellmanGroup18Sha512JCE;
import com.sshtools.client.components.DiffieHellmanGroup1Sha1JCE;
import com.sshtools.client.components.DiffieHellmanGroupExchangeSha1JCE;
import com.sshtools.client.components.DiffieHellmanGroupExchangeSha256JCE;
import com.sshtools.client.components.Rsa1024Sha1;
import com.sshtools.client.components.Rsa2048Sha256;

@SuppressWarnings("rawtypes")
module com.sshtools.synergy.client {
	requires transitive com.sshtools.synergy.common;
	requires com.sshtools.common.logger;
	requires com.sshtools.common.util;
	exports com.sshtools.client;
	exports com.sshtools.client.components;
	exports com.sshtools.client.events;
	exports com.sshtools.client.scp;
	exports com.sshtools.client.sftp;
	exports com.sshtools.client.shell;
	exports com.sshtools.client.tasks;
	uses SshKeyExchangeClientFactory;
	
	provides SshKeyExchangeClientFactory with 
		Curve25519SHA256Client.Curve25519SHA256ClientFactory,
		Curve25519SHA256LibSshClient.Curve25519SHA256LibSshClientFactory,
		DiffieHellmanEcdhNistp256.DiffieHellmanEcdhNistp256Factory,
		DiffieHellmanEcdhNistp384.DiffieHellmanEcdhNistp384Factory,
		DiffieHellmanEcdhNistp521.DiffieHellmanEcdhNistp521Factory,
		DiffieHellmanGroup14Sha1JCE.DiffieHellmanGroup14Sha1JCEFactory,
		DiffieHellmanGroup14Sha256JCE.DiffieHellmanGroup14Sha256JCEFactory,
		DiffieHellmanGroup15Sha512JCE.DiffieHellmanGroup15Sha512JCEFactory,
		DiffieHellmanGroup16Sha512JCE.DiffieHellmanGroup16Sha512JCEFactory,
		DiffieHellmanGroup17Sha512JCE.DiffieHellmanGroup17Sha512JCEFactory,
		DiffieHellmanGroup18Sha512JCE.DiffieHellmanGroup18Sha512JCEFactory,
		DiffieHellmanGroup1Sha1JCE.DiffieHellmanGroup1Sha1JCEFactory,
		DiffieHellmanGroupExchangeSha1JCE.DiffieHellmanGroupExchangeSha1JCEFactory,
		DiffieHellmanGroupExchangeSha256JCE.DiffieHellmanGroupExchangeSha256JCEFactory,
		Rsa1024Sha1.Rsa1024Sha1Factory,
		Rsa2048Sha256.Rsa2048Sha256Factory;
}
