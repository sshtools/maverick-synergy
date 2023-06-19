package com.sshtools.synergy.niofs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

import org.junit.After;
import org.junit.Before;

import com.sshtools.client.SshClient.SshClientBuilder;
import com.sshtools.client.sftp.SftpClient.SftpClientBuilder;
import com.sshtools.common.files.direct.NioFileFactory.NioFileFactoryBuilder;
import com.sshtools.common.policy.FileSystemPolicy;
import com.sshtools.common.sftp.SftpExtension;
import com.sshtools.common.sftp.extensions.BasicSftpExtensionFactory;
import com.sshtools.common.sftp.extensions.HardLinkExtension;
import com.sshtools.common.sftp.extensions.StatVFSExtension;
import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.jce.JCEProvider;
import com.sshtools.common.util.IOUtils;
import com.sshtools.server.InMemoryPasswordAuthenticator;
import com.sshtools.server.SshServer;
import com.sshtools.server.SshServerContext;
import com.sshtools.synergy.nio.SshEngineContext;

public abstract class AbstractNioFsTest {
	
	@FunctionalInterface
	public interface FsTestTask {
		void test(SftpFileSystem fs) throws Exception;
	}

	static {
		JCEProvider.enableBouncyCastle(true);
	}

	protected SshServer server;
	protected Path tmpDir;
	protected int port = -1;
	protected static boolean sandbox = true;
	protected SshServerContext currentContext;

	protected void createRandomContent(Path src) throws IOException {
		createRandomContent(src, 1024);
	}
	
	protected void createRandomContent(Path src, long size) throws IOException {
		var rnd = new Random();
		try(var out = Files.newOutputStream(src)) {
			for(long i = 0 ; i < 1024; i++) {
				out.write(rnd.nextInt(256));
			}
		}
	}
	
	protected static boolean compareFiles(Path f1, Path f2) {
		try {
			return Arrays.equals(Files.readAllBytes(f1), Files.readAllBytes(f2));
		}
		catch(IOException ioe) {
			throw new UncheckedIOException(ioe);
		}
	}

	@Before
	public void setupSshd() throws Exception {
		tmpDir = Files.createTempDirectory("niofsTests");

		server = new SshServer("127.0.0.1", 0) {

			@Override
			public SshServerContext createContext(SshEngineContext daemonContext, SocketChannel sc) throws IOException, SshException {
				setSecurityLevel(SecurityLevel.WEAK);
				var ctx = super.createServerContext(daemonContext, sc);
				currentContext = ctx;
				ctx.getPolicy(FileSystemPolicy.class).setSupportedSFTPVersion(6);
				ctx.getPolicy(FileSystemPolicy.class).getSFTPExtensionFactories().add(
						new BasicSftpExtensionFactory(new HardLinkExtension(), createStatVFSExtension()));
				return ctx;
			}
		};
		configureSshd(server);
		server.addAuthenticator(new InMemoryPasswordAuthenticator().addUser("test", "test".toCharArray()));
		server.setFileFactory((con) -> NioFileFactoryBuilder.create().
				withHome(tmpDir).
				withSandbox(sandbox).
				build());
		server.start();
		port = server.getEngine().getContext().getListeningInterfaces()[0].getActualPort();
	}

	protected SftpExtension createStatVFSExtension() {
		return new StatVFSExtension();
	}
	
	protected void configureSshd(SshServer server2) {
	}

	protected void testWithFilesystem(FsTestTask task) throws Exception {
		try (var ssh = SshClientBuilder.create().
				withTarget("localhost", port).
				withUsername("test").
				withPassword("test").
				build()) {
			var sftp = SftpClientBuilder.create().withClient(ssh).build();
			try(var fs = SftpFileSystems.newFileSystem(sftp)) {
				task.test((SftpFileSystem)fs);
			}
		}
	}

	@After
	public void teardownSshd() {
		try {
			server.close();
		} finally {
			IOUtils.silentRecursiveDelete(tmpDir);
		}
	}
}
