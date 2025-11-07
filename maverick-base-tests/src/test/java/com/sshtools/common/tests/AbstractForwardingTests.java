package com.sshtools.common.tests;

/*-
 * #%L
 * Base API Tests
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.Ignore;

import com.sshtools.common.permissions.UnauthorizedException;
import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.tests.RandomSocketServer.Mode;
import com.sshtools.common.util.Arrays;
import com.sshtools.common.util.IOUtils;

import junit.framework.TestCase;

public abstract class AbstractForwardingTests<T extends Closeable> extends TestCase {

	ForwardingConfiguration config;
	
	public interface Starter<T, RTYPE> {
		RTYPE start(T t, RandomSocketServer r) throws SshException, UnauthorizedException;
	}
	
	@Override
	protected void setUp()  {
		try {
			
			System.setProperty("maverick.failOnUnimplemented", "true");
			
			config = createForwardingConfiguration();

			if(config.enableLogging()) {
				enableLogging(config);
			}

		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
	
	protected abstract void enableLogging(ForwardingConfiguration config);
	
	protected abstract void log(String msg);
	
	protected abstract ForwardingConfiguration createForwardingConfiguration() throws IOException;

	/**
	 * Sanity check test to ensure our randomising client/server test environment completes correctly without
	 * any integration with the SSH server. We do this so that we can be confident any issue in the forwarding
	 * tests are the result of issues in the SSH code and not the client/server environment.
	 */
	public void testRandomClientServer() throws IOException, SshException, InvalidPassphraseException, UnauthorizedException {
		
		log("Starting sanity check");
		
		testTemplate(new ForwardingTestTemplate<T, Integer>() {
			
			@Override
			public Integer startForwarding(T client, Integer targetPort) throws UnauthorizedException, SshException {
				return targetPort;
			}
			
			@Override
			public T createClient(TestConfiguration config)
					throws IOException, SshException, InvalidPassphraseException {
				return null;
			}

			@Override
			public void disconnect(T client) {
				
			}
		}, 
			RandomSocketServer::getPort, 
			totalTests -> new RandomSocketServer(totalTests, config.getForwardingDataAmount()),
			this::createTCPClient
		);
	}
	
	/**
	 * Test LOCAL TCP forwarding with randomised data.
	 * 
	 * This test creates a client/server environment that sends random data to the server and receives the
	 * same data back from the server. The server side will create a digest of the data as well as the client
	 * side to ensure the integrity of the data is not compromised during the forwarding operation.
	 */
	public void testLocalForwarding() throws IOException, SshException, InvalidPassphraseException, UnauthorizedException {
		
		log("Starting LOCAL TCP forwarding test");
		
		testTemplate(
			createLocalForwardingTemplate(), 
			RandomSocketServer::getPort,
			totalTests -> new RandomSocketServer(totalTests, config.getForwardingDataAmount()),
			this::createTCPClient
		);
	}
	
	protected abstract ForwardingTestTemplate<T, Integer> createLocalForwardingTemplate();

	/**
	 * Test LOCAL Unix Domain Socket forwarding with randomised data.
	 * 
	 * This test creates a client/server environment that sends random data to the server and receives the
	 * same data back from the server. The server side will create a digest of the data as well as the client
	 * side to ensure the integrity of the data is not compromised during the forwarding operation.
	 */
	public void testLocalUnixDomainSocketForwarding() throws IOException, SshException, InvalidPassphraseException, UnauthorizedException {
		
		log("Starting LOCAL Unix Domain Socket forwarding test");
		testTemplate(
			createLocalDomainSocketForwardingTemplate(), 
			RandomSocketServer::getPath,
			totalTests -> new RandomSocketServer(Mode.UDS, null, 0, totalTests, config.getForwardingDataAmount()),
			this::createUnixDomainSocketClient
		);
	}
	
	protected abstract ForwardingTestTemplate<T, String> createLocalDomainSocketForwardingTemplate();

	/**
	 * Test REMOTE TCP forwarding with randomised data.
	 * 
	 * This test creates a client/server environment that sends random data to the server and receives the
	 * same data back from the server. The server side will create a digest of the data as well as the client
	 * side to ensure the integrity of the data is not compromised during the forwarding operation.
	 */
	public void testRemoteForwarding() throws IOException, SshException, InvalidPassphraseException, UnauthorizedException {
		
		log("Starting REMOTE TCP forwarding test");
		testTemplate(
			createRemoteForwardingTemplate(), 
			RandomSocketServer::getPort, 
			totalTests -> new RandomSocketServer(totalTests, config.getForwardingDataAmount()),
			this::createTCPClient
		);
	}
	
	protected abstract ForwardingTestTemplate<T, Integer> createRemoteForwardingTemplate();
	
	/**
	 * Test REMOTE Unix Domain Socket forwarding with randomised data.
	 * 
	 * This test creates a client/server environment that sends random data to the server and receives the
	 * same data back from the server. The server side will create a digest of the data as well as the client
	 * side to ensure the integrity of the data is not compromised during the forwarding operation.
	 */
	public void testRemoteDomainSocketForwarding() throws IOException, SshException, InvalidPassphraseException, UnauthorizedException {
		
		log("Starting REMOTE Unix Domain Socket forwarding test");
		testTemplate(
			createRemoteDomainSocketForwardingTemplate(), 
			RandomSocketServer::getPath,
			totalTests -> new RandomSocketServer(Mode.UDS, null, 0, totalTests, config.getForwardingDataAmount()),
			this::createUnixDomainSocketClient
		);
	}
	
	protected RandomClient createTCPClient(int localPort, List<RandomClient> clients) {
		try {
			InetSocketAddress addr = new InetSocketAddress("127.0.0.1", localPort);
			log("Opening TCP socket to " + addr);
			return new RandomClient(SocketChannel.open(addr),
					config.getForwardingDataAmount(),
					config.getForwardingDataBlock(),
					config.getRandomBlockSize()) {
				@Override
				protected void report(RandomClient c) {
					clients.add(c);
				}
			};
		}
		catch(IOException ioe) {
			throw new UncheckedIOException(ioe);
		}
	}
	
	protected RandomClient createUnixDomainSocketClient(String path, List<RandomClient> clients) {
		try {
			SocketChannel ch = SocketChannel.open(StandardProtocolFamily.UNIX);
			UnixDomainSocketAddress addr = UnixDomainSocketAddress.of(Path.of(path));
			ch.connect(addr);
			log("Opening UDS socket at " + addr);
			return new RandomClient(ch,
					config.getForwardingDataAmount(),
					config.getForwardingDataBlock(),
					config.getRandomBlockSize()) {
				@Override
				protected void report(RandomClient c) {
					clients.add(c);
				}
			};
		}
		catch(IOException ioe) {
			throw new UncheckedIOException(ioe);
		}
	}
	
	protected abstract ForwardingTestTemplate<T, String> createRemoteDomainSocketForwardingTemplate();

	@Ignore
	protected <INPUT> void testTemplate(
			ForwardingTestTemplate<T, INPUT> test, 
			Function<RandomSocketServer, INPUT> starter,  
			Function<Integer, RandomSocketServer> randomCreator,
			BiFunction<INPUT, List<RandomClient>, RandomClient> clientCreator) throws IOException, SshException, InvalidPassphraseException, UnauthorizedException {
		
			
			int clientCount = config.getForwardingClientCount();
			long maximumTime = config.getForwardingTimeout();
			int clientInterval = config.getForwardingClientInterval();
			int clientChannels = config.getForwardingChannelsPerClientCount();
			int channelInterval = config.getForwardingChannelInterval();
			int totalTests = clientCount * clientChannels;
			AtomicInteger currentTests = new AtomicInteger(0);
			final List<RandomClient> clients = new ArrayList<>();
			final List<T> sshClients = new ArrayList<>();
			
			RandomSocketServer rss;
			try {
				rss = randomCreator.apply(totalTests);
				
				long start = System.currentTimeMillis();
				long last = start;
				int numClients = 0;
				do {
	
					if(numClients++ < clientCount) {
						T client = test.createClient(config);
						sshClients.add(client);
						INPUT localPort = test.startForwarding(client, starter.apply(rss));
						
						new Thread() {
							public void run() {
								
								for(int i=0;i<clientChannels; i++) {
									try {
										clientCreator.apply(localPort, clients);
										
										currentTests.incrementAndGet();
										try {
											Thread.sleep(channelInterval);
										} catch (InterruptedException e) {
										}
									
									} catch(UncheckedIOException ex) {
										ex.printStackTrace();
										System.exit(1);
									}
								}
							}
						}.start();
					}
					
					try {
						Thread.sleep(clientInterval);
					} catch (InterruptedException e) {
					}
					
					if(System.currentTimeMillis() - last >= 5000) {
						last = System.currentTimeMillis();
						log(String.format("The server is %s and there are still %d clients active with %d still to be created",
									rss.isComplete() ? "complete" : "incomplete", currentTests.get() - clients.size(), totalTests - currentTests.get()));
					}
				} while(System.currentTimeMillis() - start < maximumTime 
						&& (!rss.isComplete() || clients.size() < totalTests));
				
				
				for(T t : sshClients) {
					test.disconnect(t);
				}
			}
			catch(UncheckedIOException ioe) {
				throw ioe.getCause();
			}
			
			assertTrue("The test did not complete within the given timeout threshold", rss.isComplete());
			assertEquals("Incorrect client count", totalTests, clients.size());
			assertEquals("There were fatal errors", rss.getFatalErrorCount(), 0);
			assertEquals("There were checksum errors at the server", rss.getChecksumErrorCount(), 0);
			
			for(RandomClient c : clients) {
				assertTrue("There were checksum errors at the client", c.isChecksumMatch());
			}
		
	}
	
	static AtomicInteger count = new AtomicInteger(0);
	
	abstract class RandomClient extends Thread {
		
		
		SocketChannel ch;
		long totalDataAmount;
		int maximumBlockSize;
		boolean checksumMatches = false;
		Throwable readError;
		Throwable writeError;
		String name;
		boolean randomBlock;
		
		RandomClient(SocketChannel ch, long totalDataAmount, int maximumBlockSize, boolean randomBlock) {
			this.ch = ch;
			this.totalDataAmount = totalDataAmount;
			this.maximumBlockSize = maximumBlockSize;
			this.name = String.format("client-%s", count.getAndAdd(1));
			this.randomBlock = randomBlock;
			start();
		}
		
		public boolean isChecksumMatch() {
			return checksumMatches;
		}
		
		public void run() {
			
			try(var in = new RandomReadableChannel(
						maximumBlockSize, totalDataAmount, randomBlock)) {
				
				log(String.format("Random client %s is starting. Local address is %s, Remote is %s", name, ch.getLocalAddress(), ch.getRemoteAddress()));
				
				try (var din = new DigestReadableChannel(ch)) {

					var thread = new Thread(() -> {
						try {
							long t = 0;
							var tmp = ByteBuffer.allocate(32768);
							int r;
							int m = 0;
							while ((r = din.read(tmp)) > -1) {
								t += r;
								m += r;
								if (m > 1000000) {
									m = 0;
									log(String.format("Random client %s has received %s of data of %s", name,
											IOUtils.toByteSize(t), IOUtils.toByteSize(totalDataAmount)));
								}
								tmp.clear();
							}
						} catch (Throwable e) {
							e.printStackTrace();
							readError = e;
						} finally {
							log(String.format("Random client %s has completed %s input", name,
									IOUtils.toByteSize(totalDataAmount)));
						}
					}, String.format(name + "_output"));

					try {
						thread.start();
						copy(in, ch);
					} finally {
						thread.join();
						checksumMatches = Arrays.areEqual(din.digest(), in.digest());
					}
				}
					
			} catch (Throwable e) {
				e.printStackTrace();
				writeError = e;
			} finally {

				log(String.format("Random client %s has completed and received %s with checksums %s",
						name, IOUtils.toByteSize(totalDataAmount), checksumMatches ? "matching" : "NOT matching"));
				
				report(this);
			}
		}

		protected abstract void report(RandomClient client);
	}
	
	static void copy(ReadableByteChannel in, WritableByteChannel out) throws IOException {
		var buf = ByteBuffer.allocate(32768);
		while ( in.read(buf) != -1) {
			buf.flip();
			out.write(buf);
			buf.clear();
		}
	}
}
