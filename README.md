# Maverick Synergy Java SSH API
Maverick Synergy is the third generation Java SSH API developed by JADAPTIVE Limited (previously known as SSHTOOLS Limited). It is a pure Java implementation of the SSH2 protocol enabling you to create both client and server solutions under the open source and commercial friendly LGPL license.

Built upon the Java NIO framework, this third generation of the API for the first time provides a unified framework for developing both client and server solutions.

# Features
Here are just some of the notable features of the APIs

- SSH2 client and server implementations
- Authenticate with password, keyboard-interactive or public keys
- Execute commands, transfer files via SFTP/SCP
- Port forwarding
- Support new OpenSSH private key format
- Support for ed25519, RSA, ECDSA and DSA private keys
- OpenSSH certificates, X509 certificates, SHA-256 and SHA-512 RSA signing algorithms
- Extensive key exchange support including rsa2048-sha256, curve25519-sha256@libssh.org, ecdh-sha2-nistp256, ecdh-sha2-nistp384, ecdh-sha2-nistp521, diffie-hellman-group-exchange-sha256, diffie-hellman-group14-sha256, diffie-hellman-group15-sha512, diffie-hellman-group16-sha512, diffie-hellman-group17-sha512, diffie-hellman-group18-sha512
- Load custom DH primes from /etc/ssh/moduli
- Virtual shell for building interactive CLI
- Easy file system implementation with pre-built implementations for local and commons-vfs
- Virtual file system allows mounting of disparate systems into a single unified file system
- Task-based API
- ssh-agent authentication and server implementation
- Tight integration with BouncyCastle FIPS
- Android compatible
- Callback client and server for creating zero-firewall "phone home" solutions

# Using the API
The core API has no third-party dependencies other than a Java 8 Runtime and the Java Cryptography Extensions. The project has been structured so that developers can choose the components they need without being forced to distribute third party libraries for features they do not require.

In the first instance, for a no dependency Java SSH client API simply use the maven dependency

	<dependency>
	  <groupId>com.sshtools</groupId>
	  <artifactId>maverick-synergy-client</artifactId>
	  <version>3.0.0-SNAPSHOT</version>
	</dependency>

We have created a high level API that makes it easy to make calls to SSH servers and perform common tasks. For example, downloading a file over SFTP is as simple as:
		
	try(SshClient ssh = new SshClient("hostname", port, "username", password.toCharArray())) {		
	  File file = ssh.getFile("Downloads/1.png");
     ...
	}
  
For more information visit our website at https://www.jadaptive.com
