# Maverick Synergy Java SSH API
Maverick Synergy is the third generation Java SSH API developed by JADAPTIVE Limited (previously known as SSHTOOLS Limited). It is a pure Java implementation of the SSH2 protocol enabling you to create both client and server solutions under the open source and commercial friendly LGPL license.

---
:grey_exclamation: Maverick Synergy is open-source software that is developed by a commercial organization. Whilst we strive to provide support to the open-source edition whenever we can; if you require immediate help, please consider purchasing a support subscription from [Jadaptive Limited](https://www.jadaptive.com). Subscriptions include access to security hotfixes and new features in advance of their open-source release.

Community developers should raise issues or Pull Requests directly in Github. Please ensure you have read our [Contribution Agreement](https://github.com/sshtools/maverick-synergy/blob/master/CONTRIBUTING.md) before submitting any PR requests.

---

Built upon the Java NIO framework, this third generation of the API for the first time provides a unified framework for developing both client and server solutions.

## Built with Maverick Synergy

The following solutions utilize the Maverick Synergy API in some form. If you would like your own project adding here, please get in touch or create a PR with the necersary changes to this page. 

From the API Developers, JADAPTIVE Limited
- [Secure File Exchange](https://jadaptive.com/secure-file-exchange)
- [Desktop SSH Agent](https://jadaptive.com/en/products/desktop-ssh-agent)

Third-party Solutions
- [LogonBox SSPR](https://www.logonbox.com/)

## Features
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
- Per-connection logging

The core API has no third-party dependencies other than a Java 8 Runtime and the Java Cryptography Extensions. The project has been structured so that developers can choose the components they need without being forced to distribute third party libraries for features they do not require.

## Using the Client API
In the first instance, for a no dependency Java SSH client API simply use the maven dependency
```xml
<dependency>
  <groupId>com.sshtools</groupId>
  <artifactId>maverick-synergy-client</artifactId>
  <version>3.1.0</version>
</dependency>

<!-- You may also need this repository if the version is a SNAPSHOT -->
<repository>
   <id>oss-snapshots</id>
   <url>https://oss.sonatype.org/content/repositories/snapshots</url>
   <snapshots>
      <enabled>true</enabled>
   </snapshots>
   <releases>
      <enabled>false</enabled>
   </releases>
</repository>
```
We have created a high level API that makes it easy to make calls to SSH servers and perform common tasks. For example, downloading a file over SFTP is as simple as:
```java	
try(SshClient ssh = new SshClient("hostname", port, "username", password.toCharArray())) {		
   File file = ssh.getFile("report.csv");
  ...
}
```

## Using the Server API
Include the server module to develop your own SSH/SFTP server.
```xml
<dependency>
  <groupId>com.sshtools</groupId>
  <artifactId>maverick-synergy-server</artifactId>
  <version>3.0.10</version>
</dependency>

<!-- You may also need this repository if the version is a SNAPSHOT -->
<repository>
   <id>oss-snapshots</id>
   <url>https://oss.sonatype.org/content/repositories/snapshots</url>
   <snapshots>
      <enabled>true</enabled>
   </snapshots>
   <releases>
      <enabled>false</enabled>
   </releases>
</repository>
```
We also have done a lot of the hard work for you implementing a virtual file system that simply enables you to mount folders and remote file locations into a single file system.
```xml
<dependency>
  <groupId>com.sshtools</groupId>
  <artifactId>maverick-virtual-filesystem</artifactId>
  <version>3.0.10</version>
</dependency>
```	
Creating a server is made equally as easy as the client API. You just need to make a few desisions about how users authenticate and what they can do, for example, the code below creates a simple SFTP server that provides users with their own home folder. To the user, all they see is their own sandboxed home directoy.

```java
SshServer server = new SshServer(2222);
		
Runtime.getRuntime().addShutdownHook(new Thread() {
   public void run() {
      server.close();
   }	
});
		
server.addAuthenticator(new InMemoryPasswordAuthenticator()
   .addUser("admin", "admin".toCharArray()));
		
server.setFileFactory(new VirtualFileFactory(
   new VirtualMountTemplate("/", "tmp/${username}", 
      new VFSFileFactory())));
		
server.start();
```
Try running the above and connecting to port 2222 using and SFTP client

	ssh -oPort=2222 admin@localhost


## Further Information
For more documentation on either the client or server API please visit our [Manpage](https://www.jadaptive.com/app/manpage/en/category/1559751/Maverick-Synergy). 

If you cannot find the documentation for something you want to do with the API, it's hightly likely that it can already be done but we have just not got around to documenting it yet. If you cannot find what you are looking for please [Contact Us](mailto:support@jadaptive.com) with the details and we'll try our best to point you in the right direction.

*SSH is a registered trademark and Secure Shell is a trademark of SSH Communications Security Corp (www.ssh.com)*
