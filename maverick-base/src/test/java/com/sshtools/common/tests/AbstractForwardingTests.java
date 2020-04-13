/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.common.tests;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import org.junit.Ignore;

import com.sshtools.common.permissions.UnauthorizedException;
import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.Arrays;
import com.sshtools.common.util.IOUtils;

import junit.framework.TestCase;

public abstract class AbstractForwardingTests<T extends Closeable> extends TestCase {

	ForwardingConfiguration config;
	
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
//	@Ignore
	public void testRandomClientServer() throws IOException, SshException, InvalidPassphraseException, UnauthorizedException {
		
		log("Starting sanity check");
		
		testTemplate(new ForwardingTestTemplate<T>() {
			
			@Override
			public int startForwarding(T client, int targetPort) throws UnauthorizedException, SshException {
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
		});
	}
	
	
	/**
	 * Test LOCAL forwarding with randomised data.
	 * 
	 * This test creates a client/server environment that sends random data to the server and receives the
	 * same data back from the server. The server side will create a digest of the data as well as the client
	 * side to ensure the integrity of the data is not compromised during the forwarding operation.
	 */
	@Ignore
	public void testLocalForwarding() throws IOException, SshException, InvalidPassphraseException, UnauthorizedException {
		
		log("Starting LOCAL forwarding test");
		
		testTemplate(createLocalForwardingTemplate());
	}
	
	protected abstract ForwardingTestTemplate<T> createLocalForwardingTemplate();

	/**
	 * Test REMOTE forwarding with randomised data.
	 * 
	 * This test creates a client/server environment that sends random data to the server and receives the
	 * same data back from the server. The server side will create a digest of the data as well as the client
	 * side to ensure the integrity of the data is not compromised during the forwarding operation.
	 */
	public void testRemoteForwarding() throws IOException, SshException, InvalidPassphraseException, UnauthorizedException {
		
		log("Starting REMOTE forwarding test");
		
		testTemplate(createRemoteForwardingTemplate());
	}
	
	protected abstract ForwardingTestTemplate<T> createRemoteForwardingTemplate();

	@Ignore
	protected void testTemplate(ForwardingTestTemplate<T> test) throws IOException, SshException, InvalidPassphraseException, UnauthorizedException {
		
			
			int clientCount = config.getForwardingClientCount();
			long maximumTime = config.getForwardingTimeout();
			int clientInterval = config.getForwardingClientInterval();
			int clientChannels = config.getForwardingChannelsPerClientCount();
			int channelInterval = config.getForwardingChannelInterval();
			int totalTests = clientCount * clientChannels;
			int currentTests = 0;
			
			RandomSocketServer rss = new RandomSocketServer(totalTests);
			
			long start = System.currentTimeMillis();
			long last = start;
			final List<RandomClient> clients = new ArrayList<>();
			final List<T> sshClients = new ArrayList<>();
			int x = 0;

			do {

				if(x < clientCount) {
					T client = test.createClient(config);
					sshClients.add(client);
					int localPort = test.startForwarding(client, rss.getPort());
					
					for(int i=0;i<clientChannels; i++) {
						new RandomClient(new Socket("127.0.0.1", localPort),
								config.getForwardingDataAmount(),
								config.getForwardingDataBlock(), x, i) {
							@Override
							protected void report(RandomClient c) {
								clients.add(c);
							}
						};
						
						currentTests++;
						try {
							Thread.sleep(channelInterval);
						} catch (InterruptedException e) {
						}
					}
					x++;
				}
				
				try {
					Thread.sleep(clientInterval);
				} catch (InterruptedException e) {
				}
				
				if(System.currentTimeMillis() - last >= 60000) {
					last = System.currentTimeMillis();
					log(String.format("The server is %s and there are still %d clients active with %d still to be created",
								rss.isComplete() ? "complete" : "incomplete", currentTests - clients.size(), totalTests - currentTests));
				}
			} while(System.currentTimeMillis() - start < maximumTime 
					&& (!rss.isComplete() || clients.size() < totalTests));
			
			
			for(T t : sshClients) {
				test.disconnect(t);
			}
			
			assertTrue("The test did not complete within the given timeout threshold", rss.isComplete());
			assertEquals("Incorrect client count", totalTests, clients.size());
			assertEquals("There were fatal errors", rss.getFatalErrorCount(), 0);
			assertEquals("There were checksum errors at the server", rss.getChecksumErrorCount(), 0);
			
			for(RandomClient c : clients) {
				assertTrue("There were checksum errors at the client", c.isChecksumMatch());
			}
		
	}
	
	abstract class RandomClient extends Thread {
		
		Socket s;
		long totalDataAmount;
		int maximumBlockSize;
		boolean checksumMatches = false;
		Throwable readError;
		Throwable writeError;
		String name;
		RandomClient(Socket s, long totalDataAmount, int maximumBlockSize, int index, int channel) {
			super(String.format("client-%d-%d_input", index, channel));
			this.s = s;
			this.totalDataAmount = totalDataAmount;
			this.maximumBlockSize = maximumBlockSize;
			this.name = String.format("client-%d-%d", index, channel);
			start();
		}
		
		public boolean isChecksumMatch() {
			return checksumMatches;
		}
		
		public void run() {
			
			try {
				
				log(String.format("Random client %s is starting", name));
				
				final RandomInputStream in = new RandomInputStream(maximumBlockSize, totalDataAmount);
				final OutputStream out = s.getOutputStream();
				new Thread(String.format(name + "_output")) {
					public void run() {
						try {
							IOUtils.copy(in, out);
						} catch (Throwable e) {
							e.printStackTrace();
							writeError = e;
						} finally {
							log(String.format("Random client %s has completed %s output", name, IOUtils.toByteSize(totalDataAmount)));
						}
					}
				}.start();
				
				DigestInputStream din = new DigestInputStream(s.getInputStream(), MessageDigest.getInstance("MD5"));
				long t = 0;
				while(din.read() > -1 && ++t < totalDataAmount) {
					if(t % (1000000) == 0) {
						log(String.format("Random client %s has received %s of data of %s",
									name, IOUtils.toByteSize(t), IOUtils.toByteSize(totalDataAmount)));
					}
				}
				
				
				s.close();
				checksumMatches = Arrays.areEqual(din.getMessageDigest().digest(), in.digest.digest());
				
				log(String.format("Random client %s has completed and received %s with checksums %s",
						name, IOUtils.toByteSize(totalDataAmount), checksumMatches ? "matching" : "NOT matching"));
				
			} catch (Throwable e) {
				e.printStackTrace();
				readError = e;
			} finally {
				report(this);
			}
		}

		protected abstract void report(RandomClient client);
	}
	
	class RandomInputStream extends InputStream {

		long totalDataAmount;
		int maximumBlockSize;
		Random r = new Random();
		MessageDigest digest;
		RandomInputStream(int maximumBlockSize, long totalDataAmount) throws NoSuchAlgorithmException {
			this.maximumBlockSize = maximumBlockSize;
			this.totalDataAmount = totalDataAmount;
			
			this.digest = MessageDigest.getInstance("MD5");
		}
		
		public int read(byte[] buf, int off, int len) {
		
			if(totalDataAmount==0) {
				return -1;
			}
			int max = Math.min(len, maximumBlockSize);
			if(totalDataAmount < max) {
				max = (int) totalDataAmount;
			}
			int s = r.nextInt(max);
			if(s==0) {
				s = max;
			}
			
			byte[] b = new byte[s];
			r.nextBytes(b);
			
			digest.update(b);
			System.arraycopy(b, 0, buf, off, b.length);
			totalDataAmount-=b.length;
			return b.length;
		}
		
		@Override
		public int read() throws IOException {
			byte[] b = new byte[1];
			if(read(b) > 0) {
				return b[1] & 0xFF;
			};
			return -1;
		}
		
	}
	
	class RandomSocketServer extends Thread {
		
		ServerSocket sock;
		Throwable lastError;
		int count;
		List<RandomSocketClient> completed = new ArrayList<RandomSocketClient>();
		List<RandomSocketClient> fatalErrors = new ArrayList<RandomSocketClient>();
		List<RandomSocketClient> checksumErrors = new ArrayList<RandomSocketClient>();
		
		RandomSocketServer(int count) throws IOException {
			super("RandomSocketServer");
			this.count = count;
			sock = new ServerSocket(0);
			start();
		}
		
		public int getFinishedCount() {
			return completed.size() + fatalErrors.size() + checksumErrors.size();
		}

		public int getPort() {
			return sock.getLocalPort();
		}
		
		public void report(RandomSocketClient client) {
			if(client.hasError()) {
				fatalErrors.add(client);
			} if(!client.hasMatchingChecksums()) { 
			    checksumErrors.add(client);	
			} else {
				completed.add(client);
			}
		}
		
		public boolean isComplete() {
			return getFinishedCount() == count;
		}
		
		public int getChecksumErrorCount() {
			return checksumErrors.size();
		}
		
		public int getFatalErrorCount() {
			return fatalErrors.size();
		}
		
		public void run() {
			
			Socket s;
			try {
				int index = 0;
				while(index < count && (s = sock.accept()) != null) {
					new RandomSocketClient(this, s, index++).start();
				}
			} catch (Throwable e) {
				e.printStackTrace();
				lastError = e;
			}
		}
	}
	
	class RandomSocketClient extends Thread {
		
		Socket s;
		Throwable lastError;
		RandomSocketServer server;
		Boolean matchingChecksums = false;
		RandomSocketClient(RandomSocketServer server, Socket s, int index) {
			super("RandomSocketClient_" + index);
			this.server = server;
			this.s = s;
		}
		
		public boolean hasError() {
			return !Objects.isNull(lastError);
		}
		
		public boolean hasMatchingChecksums() {
			return matchingChecksums;
		}
		
		public void run() {
			
			try {
				DigestInputStream in = new DigestInputStream(s.getInputStream(), MessageDigest.getInstance("MD5"));
				DigestOutputStream out = new DigestOutputStream(s.getOutputStream(), MessageDigest.getInstance("MD5"));
				IOUtils.copy(in, out);
				
				matchingChecksums = Arrays.areEqual(in.getMessageDigest().digest(), out.getMessageDigest().digest());
			} catch (Throwable e) {
				e.printStackTrace();
				lastError = e;
			} finally {
				server.report(this);
			}
		}
	}
}
