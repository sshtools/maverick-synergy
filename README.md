# Maverick Synergy Java SSH API
Maverick Synergy is the third generation Java SSH API developed by JADAPTIVE Limited (previously known as SSHTOOLS Limited). It is a pure Java implementation of the SSH2 protocol enabling you to create both client and server solutions under the open source and commercial friendly LGPL license.

Built upon the Java NIO framework, this third generation of the API for the first time provides a unified framework for developing both client and server solutions.

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
