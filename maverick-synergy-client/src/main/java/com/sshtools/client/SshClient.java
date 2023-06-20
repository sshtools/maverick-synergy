/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.client;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.UnresolvedAddressException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import com.sshtools.client.PasswordAuthenticator.PasswordPrompt;
import com.sshtools.client.tasks.CommandTask;
import com.sshtools.client.tasks.CommandTask.CommandTaskBuilder;
import com.sshtools.client.tasks.DownloadFileTask.DownloadFileTaskBuilder;
import com.sshtools.client.tasks.Task;
import com.sshtools.client.tasks.UploadFileTask.UploadFileTaskBuilder;
import com.sshtools.common.auth.PasswordAuthentication;
import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.events.EventListener;
import com.sshtools.common.forwarding.ForwardingPolicy;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.UnauthorizedException;
import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.ssh.Channel;
import com.sshtools.common.ssh.ChannelEventListener;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.synergy.nio.ConnectRequestFuture;
import com.sshtools.synergy.ssh.Connection;

public class SshClient implements Closeable {

	public static final String GUEST_USERNAME = System.getProperty("maverick.guestUsername", "guest");
	public static final long DEFAULT_CONNECT_TIMEOUT = Long.parseLong(System.getProperty("maverick.defaultConnectTimeout", "30000"));

	@FunctionalInterface
	public interface OnConfiguration {
		void accept(SshClientContext ctx) throws IOException, SshException;
	}

	public final static class PreConnectedSshClientBuilder {
		
		private final SshConnection con;
		private boolean closeOnDisconnect = true;

		private PreConnectedSshClientBuilder(SshConnection con) {
			this.con = con;
		}
		
		/**
		 * Set whether to close the {@link SshConnection} when this client
		 * disconnects.
		 * 
		 * @param close on disconnect
		 * @return this for chaining
		 */
		public PreConnectedSshClientBuilder withoutCloseOnDisconnect() {
			this.closeOnDisconnect = false;
			return this;
		}

		/**
		 * Build a new {@link SshClientBuilder} set.
		 * 
		 * @return permissions
		 */
		public SshClient build() {
			return new SshClient(con, closeOnDisconnect);
		}
	}

	public final static class SshClientBuilder {
		private Optional<SshClientContext> sshContext = Optional.empty();
		private Optional<String> hostname = Optional.empty();
		private Optional<Integer> port  = Optional.empty();
		private Optional<String> username = Optional.empty();
		private Optional<Duration> connectTimeout = Optional.empty();
		private Set<ClientAuthenticator> authenticators = new LinkedHashSet<>();
		private Set<SshKeyPair> identities = new LinkedHashSet<>();
		private Optional<OnConfiguration> onConfigure = Optional.empty();
		
		/**
		 * Set a {@link Consumer} that receives a {@link SshClientContext} when the connection
		 * is ready for configuration. You may use this to configure socket options and other
		 * advanced settings.
		 * 
		 * @param onConfigure callback invoked on configuration
		 */
		public SshClientBuilder onConfigure(OnConfiguration onConfigure) {
			this.onConfigure = Optional.of(onConfigure);
			return this;
		}
		
		/**
		 * Set a private key file to use for authentication. Internally, this adds a {@link PrivateKeyFileAuthenticator}.
		 * 
		 * @param file private key file
		 * @return this for chaining
		 * @throws UncheckedIOException on any I/O error or parsing of key
		 */
		public SshClientBuilder withPrivateKeyFile(Path file) {
			try {
				return addAuthenticators(new PrivateKeyFileAuthenticator(file));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		
		/**
		 * Set a private key file to use for authentication. Internally, this adds a {@link PrivateKeyFileAuthenticator}.
		 * 
		 * @param file private key file
		 * @return this for chaining
		 * @throws UncheckedIOException on any I/O error or parsing of key
		 */
		public SshClientBuilder withPrivateKeyFile(File file) {
			try {
				return addAuthenticators(new PrivateKeyFileAuthenticator(file));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		
		/**
		 * Set the password to use for authentication. Internally, this adds a {@link PasswordAuthentication}.
		 * 
		 * @param password password
		 * @return this for chaining
		 */
		public SshClientBuilder withPassword(char[] password) {
			return withPassword(password == null ? null : new String(password));
		}
		
		/**
		 * Set the password to use for authentication. Internally, this adds a {@link PasswordAuthentication}.
		 * 
		 * @param password password
		 * @return this for chaining
		 */
		public SshClientBuilder withPassword(String password) {
			return withPassword(Optional.ofNullable(password));
		}
		
		/**
		 * Set the password to use for authentication. Internally, this adds a {@link PasswordAuthentication}.
		 * 
		 * @param password password
		 * @return this for chaining
		 */
		public SshClientBuilder withPassword(Optional<String> password) {
			return withPasswordPrompt(() -> password.orElse(null));
		}
		
		/**
		 * Set the password prompt to use for authentication. Internally, this adds a {@link PasswordAuthentication}.
		 * 
		 * @param prompt password prompt
		 * @return this for chaining
		 */
		public SshClientBuilder withPasswordPrompt(PasswordPrompt prompt) {
			return addAuthenticators(PasswordAuthenticator.of(prompt));
		}
		
		/**
		 * Add on more identities (key pairs) to use for authentication. 
		 * 
		 * @param identities identities
		 * @return this for chaining
		 */
		public SshClientBuilder addIdentities(SshKeyPair... identities) {
			return addIdentities(Arrays.asList(identities));
		}
		
		/**
		 * Add a key pair to use for authentication. 
		 * 
		 * @param keyPair key pair
		 */
		public SshClientBuilder addIdentities(Collection<SshKeyPair> identities) {
			this.identities.addAll(identities);
			return this;
		}
		
		/**
		 * Set one more identities (key pairs) to use for authentication. Any existing
		 * built identities will be replaced. 
		 * 
		 * @param identities identities
		 * @return this for chaining
		 */
		public SshClientBuilder withIdentities(SshKeyPair... identities) {
			this.identities.clear();
			return addIdentities(identities);
		}
		
		/**
		 * Add a key pair to use for authentication. 
		 * 
		 * @param keyPair key pair
		 */
		public SshClientBuilder withIdentities(Collection<SshKeyPair> identities) {
			this.identities.clear();	
			return addIdentities(identities);
		}
		
		/**
		 * Add one or more {@link ClientAuthenticator} instances that will be presented
		 * to the server one at a time. This will be used by convenience methods such
		 * as {@link #withPassword(String)} and TODO XXXXXXXXX
		 * 
		 * @param authenticators authenticators
		 * @return this for chaining
		 */
		public SshClientBuilder addAuthenticators(ClientAuthenticator... authenticators) {
			return addAuthenticators(Arrays.asList(authenticators));
		}
		
		/**
		 * Add one or more {@link ClientAuthenticator} instances that will be presented
		 * to the server one at a time. This will be used by convenience methods such
		 * as {@link #withPassword(String)} and TODO XXXXXXXXX
		 * 
		 * @param authenticators authenticators
		 * @return this for chaining
		 */
		public SshClientBuilder addAuthenticators(Collection<ClientAuthenticator> authenticators) {
			this.authenticators.addAll(authenticators);
			return this;
		}
		
		/**
		 * Set the list of one or more {@link ClientAuthenticator} instances that will be presented
		 * to the server one at a time. This will replace any other built authenticators. 
		 * 
		 * @param authenticators authenticators
		 * @return this for chaining
		 */
		public SshClientBuilder withAuthenticators(ClientAuthenticator... authenticators) {
			return withAuthenticators(Arrays.asList(authenticators));
		}
		
		/**
		 * Set the list of one or more {@link ClientAuthenticator} instances that will be presented
		 * to the server one at a time. This will replace any other built authenticators.
		 * 
		 * @param authenticators authenticators
		 * @return this for chaining
		 */
		public SshClientBuilder withAuthenticators(Collection<ClientAuthenticator> authenticators) {
			this.authenticators.clear();
			return addAuthenticators(authenticators);
		}
		
		/**
		 * Set the connection timeout in milliseconds.
		 * 
		 * @param connectionTimeout connection timeout
		 * @return this for chaining
		 */
		public SshClientBuilder withConnectTimeout(long connectTimeout) {
			return withConnectTimeout(Duration.ofMillis(connectTimeout));
		}
		
		/**
		 * Set the connection timeout.
		 * 
		 * @param milliseconds connection timeout
		 * @return this for chaining
		 */
		public SshClientBuilder withConnectTimeout(Duration connectTimeout) {
			this.connectTimeout = Optional.of(connectTimeout);
			return this;
		}
		
		/**
		 * Set the username to use for authentication. If not provided, <strong>guest</strong> will be used. A blank
		 * or null string will be treated as if the password was not provided. 
		 * 
		 *  @param username username
		 *  @return this for chaining
		 */
		public SshClientBuilder withUsername(String username) {
			return withUsername("".equals(username) ? Optional.empty() : Optional.ofNullable(username));
		}
		
		/**
		 * Set the username to use for authentication as currently local in local user, 
		 * what is returned by <code>System.getProperty("user.name");</code>. 
		 * 
		 *  @return this for chaining
		 */
		public SshClientBuilder withCurrentUsername(String username) {
			return withUsername(System.getProperty("user.name"));
		}

		/**
		 * Set the username to use for authentication. If not provided, <strong>guest</strong> will be used.
		 * 
		 *  @param username username
		 *  @return this for chaining
		 */
		public SshClientBuilder withUsername(Optional<String> username) {
			this.username = username;
			return this;
		}
		
		/**
		 * Set the port to use. If not provided, the default of <code>22</code> will be used.
		 * 
		 *  @param port port
		 *  @return this for chaining
		 */
		public SshClientBuilder withPort(int port) {
			return withPort(Optional.of(port));
		}
		
		/**
		 * Set the port to use. If not provided, the default of <code>22</code> will be used.
		 * 
		 *  @param port port
		 *  @return this for chaining
		 */
		public SshClientBuilder withPort(Optional<Integer> port) {
			this.port = port;
			return this;
		}
		
		/**
		 * Set the <strong>host</strong> to use. May result in name resolution as internally {@link InetAddress#getHostName()}
		 * will be used resolve the address to string. 
		 * 
		 * @param address address
		 * @return this for chaining
		 */
		public SshClientBuilder withHost(InetAddress address) {
			return withHostname(address.getHostName());
		}
		
		/**
		 * Set the <strong>hostname</strong> to use. This must be either a valid hostname or IP address.
		 * If not provided, <code>localhost</code> will be used.
		 * 
		 * @param SshClientContext sshContext
		 * @return this for chaining
		 */
		public SshClientBuilder withHostname(String hostname) {
			this.hostname = Optional.of(hostname);
			return this;
		}
		
		/**
		 * Set the <strong>host</strong> and <strong>port</code> to use from the provided address. 
		 * May result in name resolution as internally {@link InetAddress#getHostName()}
		 * will be used resolve the address to string. 
		 * 
		 * @param address address
		 * @return this for chaining
		 */
		public SshClientBuilder withTarget(InetSocketAddress address) {
			return withHostname(address.getHostName()).withPort(address.getPort());
		}
		
		/**
		 * Set the <strong>hostname</strong> and <strong>port</code> to use from the provided address. 
		 * 
		 * @param hostname hostname
		 * @param port port
		 * @return this for chaining
		 */
		public SshClientBuilder withTarget(String hostname, int port) {
			return withHostname(hostname).withPort(port);
		}
		
		/**
		 * Set the {@link SshClientContext} to use. If not provided, a default implementation will
		 * be used.
		 * 
		 * @param SshClientContext sshContext
		 * @return this for chaining
		 */
		public SshClientBuilder withSshContext(SshClientContext context) {
			this.sshContext = Optional.of(context);
			return this;
		}
		
		/**
		 * Create a new {@link SshClientBuilder}
		 * 
		 * @return builder
		 */
		public static SshClientBuilder create() {
			return new SshClientBuilder();
		}
		
		/**
		 * Create a new {@link SshClientBuilder} using an existing connection. 
		 * 
		 * @return builder
		 */
		public static PreConnectedSshClientBuilder create(SshConnection connection) {
			return new PreConnectedSshClientBuilder(connection);
		}
		
		private SshClientBuilder() {
		}

		/**
		 * Build a new {@link SshClientBuilder} set.
		 * 
		 * @return permissions
		 * @throws SshException 
		 * @throws IOException 
		 */
		public SshClient build() throws IOException, SshException {
			return new SshClient(this);
		}
	}

	private final SshClientContext sshContext;
	private final String hostname;
	private final int port;
	private final boolean closeConnection;
	private final Optional<OnConfiguration> onConfigure;
	private final String[] remotePublicKeys; 
	private final Connection<SshClientContext> con;
	
	private SshClient(SshClientBuilder builder) throws IOException, SshException {
		this.sshContext = builder.sshContext.isPresent() ? builder.sshContext.get() :new SshClientContext();
		this.hostname = builder.hostname.orElse("localhost");
		this.port = builder.port.orElse(22);
		this.closeConnection = true;
		this.onConfigure = builder.onConfigure;
		
		sshContext.setUsername(builder.username.orElseGet(() -> GUEST_USERNAME));
		
		var keys = new ArrayList<String>();
		con = doConnect(hostname, port, sshContext, builder.connectTimeout.map(Duration::toMillis).orElse(DEFAULT_CONNECT_TIMEOUT), keys);
		remotePublicKeys = keys.toArray(new String[0]); 
		
		if(!builder.authenticators.isEmpty() || !builder.identities.isEmpty()) {
			var auths = new ArrayList<>(builder.authenticators);
			
			if(!builder.identities.isEmpty()) {
				auths.add(0, new KeyPairAuthenticator(builder.identities.toArray(new SshKeyPair[0])));
			}
	
			while(!isAuthenticated() && !auths.isEmpty()) {
				authenticate(auths.remove(0), builder.connectTimeout.map(Duration::toMillis).orElse(DEFAULT_CONNECT_TIMEOUT));
			}
			
			if(!isAuthenticated()) {
				close();
				throw new IOException("Authentication failed");
			}
		}
	}

	/**
	 * Create a new client.
	 * 
	 * @param hostname host
	 * @param port port
	 * @param username user
	 * @param connectTimeout timeout
	 * @param password password
	 * @throws IOException on I/O error
	 * @throws SshException on general SSH error
	 * @deprecated 
	 * @see SshClientBuilder
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public SshClient(String hostname, int port, String username, long connectTimeout, char[] password) throws IOException, SshException {
		this(hostname, port, username, new SshClientContext(), connectTimeout, password);
	}
	
	/**
	 * Create a new client.
	 * 
	 * @param hostname host
	 * @param port port
	 * @param username user
	 * @param password password
	 * @throws IOException on I/O error
	 * @throws SshException on general SSH error
	 * @deprecated 
	 * @see SshClientBuilder
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public SshClient(String hostname, int port, String username, char[] password) throws IOException, SshException {
		this(hostname, port, username, new SshClientContext(), DEFAULT_CONNECT_TIMEOUT, password);
	}
	
	/**
	 * Create a new client.
	 * 
	 * @param hostname host
	 * @param port port
	 * @param username user
	 * @param password password
	 * @param sshContext context
	 * @throws IOException on I/O error
	 * @throws SshException on general SSH error
	 * @deprecated 
	 * @see SshClientBuilder
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public SshClient(String hostname, int port, String username, char[] password, SshClientContext context) throws IOException, SshException {
		this(hostname, port, username, context, DEFAULT_CONNECT_TIMEOUT, password);
	}

	/**
	 * Create a new client.
	 * 
	 * @param hostname host
	 * @param port port
	 * @param username user
	 * @param connectTimeout timeout
	 * @param key key file
	 * @throws IOException on I/O error
	 * @throws SshException on general SSH error
	 * @deprecated 
	 * @see SshClientBuilder
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public SshClient(String hostname, int port, String username, long connectTimeout, File key) throws IOException, SshException, InvalidPassphraseException {
		this(hostname, port, username, connectTimeout, key, null);
	}

	/**
	 * Create a new client.
	 * 
	 * @param hostname host
	 * @param port port
	 * @param username user
	 * @param key key file
	 * @throws IOException on I/O error
	 * @throws SshException on general SSH error
	 * @deprecated 
	 * @see SshClientBuilder
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public SshClient(String hostname, int port, String username, File key) throws IOException, SshException, InvalidPassphraseException {
		this(hostname, port, username, DEFAULT_CONNECT_TIMEOUT, key, null);
	}

	/**
	 * Create a new client.
	 * 
	 * @param hostname host
	 * @param port port
	 * @param username user
	 * @param connectTimeout timeout
	 * @param key key file
	 * @param passphrase key passphrase
	 * @throws IOException on I/O error
	 * @throws SshException on general SSH error
	 * @deprecated 
	 * @see SshClientBuilder
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public SshClient(String hostname, int port, String username, long connectTimeout, File key, String passphrase) throws IOException, SshException, InvalidPassphraseException {
		this(hostname, port, username, connectTimeout, SshKeyUtils.getPrivateKey(key, passphrase));
	}

	/**
	 * Create a new client.
	 * 
	 * @param hostname host
	 * @param port port
	 * @param username user
	 * @param key key file
	 * @param passphrase key passphrase
	 * @throws IOException on I/O error
	 * @throws SshException on general SSH error
	 * @deprecated 
	 * @see SshClientBuilder
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public SshClient(String hostname, int port, String username,  File key, String passphrase) throws IOException, SshException, InvalidPassphraseException {
		this(hostname, port, username, DEFAULT_CONNECT_TIMEOUT, SshKeyUtils.getPrivateKey(key, passphrase));
	}
	
	/**
	 * Create a new client.
	 * 
	 * @param hostname host
	 * @param port port
	 * @param username user
	 * @param connectTimeout timeout
	 * @param identities identities
	 * @throws IOException on I/O error
	 * @throws SshException on general SSH error
	 * @deprecated 
	 * @see SshClientBuilder
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public SshClient(String hostname, int port, String username, long connectTimeout, SshKeyPair... identities) throws IOException, SshException, InvalidPassphraseException {
		this(hostname, port, username, new SshClientContext(), connectTimeout, identities);
	}
	
	/**
	 * Create a new client.
	 * 
	 * @param hostname host
	 * @param port port
	 * @param username user
	 * @param identities identities
	 * @throws IOException on I/O error
	 * @throws SshException on general SSH error
	 * @deprecated 
	 * @see SshClientBuilder
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public SshClient(String hostname, int port, String username, SshKeyPair... identities) throws IOException, SshException, InvalidPassphraseException {
		this(hostname, port, username, new SshClientContext(), DEFAULT_CONNECT_TIMEOUT, identities);
	}
	
	/**
	 * Create a new client.
	 * 
	 * @param hostname host
	 * @param port port
	 * @param username user
	 * @param sshContext context
	 * @param connectTimeout timeout
	 * @param identities identities
	 * @throws IOException on I/O error
	 * @throws SshException on general SSH error
	 * @deprecated 
	 * @see SshClientBuilder
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public SshClient(String hostname, int port, String username, SshClientContext sshContext, long connectTimeout, SshKeyPair... identities) throws IOException, SshException {
		this(hostname, port, username, sshContext, connectTimeout, null, identities);
	}
	
	/**
	 * Create a new client.
	 * 
	 * @param hostname host
	 * @param port port
	 * @param username user
	 * @param sshContext context
	 * @param identities identities
	 * @throws IOException on I/O error
	 * @throws SshException on general SSH error
	 * @deprecated 
	 * @see SshClientBuilder
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public SshClient(String hostname, int port, String username, SshClientContext sshContext, SshKeyPair... identities) throws IOException, SshException {
		this(hostname, port, username, sshContext, DEFAULT_CONNECT_TIMEOUT, null, identities);
	}
	
	/**
	 * Create a new client.
	 * 
	 * @param hostname host
	 * @param port port
	 * @param username user
	 * @param connectTimeout timeout
	 * @param password password
	 * @param identities identities
	 * @throws IOException on I/O error
	 * @throws SshException on general SSH error
	 * @deprecated 
	 * @see SshClientBuilder
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public SshClient(String hostname, int port, String username, long connectTimeout, char[] password, SshKeyPair... identities) throws IOException, SshException {
		this(hostname, port, username, new SshClientContext(), connectTimeout, password, identities);
	}
	
	/**
	 * Create a new client.
	 * 
	 * @param hostname host
	 * @param port port
	 * @param username user
	 * @param password password
	 * @param identities identities
	 * @throws IOException on I/O error
	 * @throws SshException on general SSH error
	 * @deprecated 
	 * @see SshClientBuilder
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public SshClient(String hostname, int port, String username, char[] password, SshKeyPair... identities) throws IOException, SshException {
		this(hostname, port, username, new SshClientContext(), DEFAULT_CONNECT_TIMEOUT, password, identities);
	}

	/**
	 * Create a new client.
	 * 
	 * @param hostname host
	 * @param port port
	 * @param username user
	 * @param connectTimeout timeout
	 * @param password password
	 * @param key key file
	 * @param passphrase key passphrase
	 * @throws IOException on I/O error
	 * @throws SshException on general SSH error
	 * @deprecated 
	 * @see SshClientBuilder
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public SshClient(String hostname, Integer port, String username, long connectTimeout, char[] password, File key, String passphrase) throws IOException, SshException, InvalidPassphraseException {
		this(hostname, port, username, connectTimeout, password, SshKeyUtils.getPrivateKey(key, passphrase));
	}
	
	/**
	 * Create a new client.
	 * 
	 * @param hostname host
	 * @param port port
	 * @param username user
	 * @param password password
	 * @param key key
	 * @param passphrase passphrase
	 * @throws IOException on I/O error
	 * @throws SshException on general SSH error
	 * @deprecated 
	 * @see SshClientBuilder
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public SshClient(String hostname, Integer port, String username, char[] password, File key, String passphrase) throws IOException, SshException, InvalidPassphraseException {
		this(hostname, port, username, DEFAULT_CONNECT_TIMEOUT, password, SshKeyUtils.getPrivateKey(key, passphrase));
	}
	
	/**
	 * Create a new client.
	 * 
	 * @param hostname host
	 * @param port port
	 * @param username user
	 * @param connectTimeout timeout
	 * @throws IOException on I/O error
	 * @throws SshException on general SSH error
	 * @deprecated 
	 * @see SshClientBuilder
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public SshClient(String hostname, Integer port, String username, long connectTimeout) throws IOException, SshException {
		this(hostname, port, username, new SshClientContext(), connectTimeout);
	}
	
	/**
	 * Create a new client.
	 * 
	 * @param hostname host
	 * @param port port
	 * @param username user
	 * @throws IOException on I/O error
	 * @throws SshException on general SSH error
	 * @deprecated 
	 * @see SshClientBuilder
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public SshClient(String hostname, Integer port, String username) throws IOException, SshException {
		this(hostname, port, username, new SshClientContext(), DEFAULT_CONNECT_TIMEOUT);
	}
	
	/**
	 * Create a new client.
	 * 
	 * @param hostname host
	 * @param port port
	 * @param username user
	 * @param sshContext context
	 * @param connectTimeout timeout
	 * @throws IOException on I/O error
	 * @throws SshException on general SSH error
	 * @deprecated 
	 * @see SshClientBuilder
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public SshClient(String hostname, Integer port, String username, SshClientContext sshContext, long connectTimeout) throws IOException, SshException {
		this(hostname, port, username, sshContext, connectTimeout, (char[])null);
	}
	
	/**
	 * Create a new client.
	 * 
	 * @param hostname host
	 * @param port port
	 * @param username user
	 * @param sshContext context
	 * @throws IOException on I/O error
	 * @throws SshException on general SSH error
	 * @deprecated 
	 * @see SshClientBuilder
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public SshClient(String hostname, Integer port, String username, SshClientContext sshContext) throws IOException, SshException {
		this(hostname, port, username, sshContext, DEFAULT_CONNECT_TIMEOUT, (char[])null);
	}

	/**
	 * Create a new client.
	 * 
	 * @param hostname host
	 * @param port port
	 * @param username user
	 * @param sshContext context
	 * @param connectTimeout timeout
	 * @param password password
	 * @param identities identities
	 * @throws IOException on I/O error
	 * @throws SshException on general SSH error
	 * @deprecated 
	 * @see SshClientBuilder
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public SshClient(SshConnection con) {
		this(con, true);
	}
	
	@SuppressWarnings("unchecked")
	
	/**
	 * Create a new client.
	 * 
	 * @param hostname host
	 * @param port port
	 * @param username user
	 * @param sshContext context
	 * @param connectTimeout timeout
	 * @param password password
	 * @param identities identities
	 * @throws IOException on I/O error
	 * @throws SshException on general SSH error
	 * @deprecated 
	 * @see SshClientBuilder
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public SshClient(SshConnection con, boolean closeConnection) {
		this.con = (Connection<SshClientContext>) con;
		this.closeConnection = closeConnection;
		this.onConfigure = Optional.empty();
		this.sshContext = (SshClientContext) con.getContext();
		this.hostname = con.getRemoteIPAddress();
		this.port = con.getRemotePort();
		this.remotePublicKeys = con.getRemotePublicKeys();
	}
	
	/**
	 * Create a new client.
	 * 
	 * @param hostname host
	 * @param port port
	 * @param username user
	 * @param sshContext context
	 * @param connectTimeout timeout
	 * @param password password
	 * @param identities identities
	 * @throws IOException on I/O error
	 * @throws SshException on general SSH error
	 * @deprecated 
	 * @see SshClientBuilder
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public SshClient(String hostname, int port, String username, SshClientContext sshContext, long connectTimeout, char[] password, SshKeyPair... identities) throws IOException, SshException {
		this.sshContext = sshContext;
		this.hostname = hostname;
		this.port = port;
		this.onConfigure = Optional.empty();
		this.closeConnection = true;
		sshContext.setUsername(username);
		var keys = new ArrayList<String>();
		con = doConnect(hostname, port, sshContext, connectTimeout, keys);
		this.remotePublicKeys = keys.toArray(new String[0]);
		boolean attempted = false;

		if(!isAuthenticated() && identities.length > 0) {
			attempted = true;
			authenticate(new KeyPairAuthenticator(identities), 30000);
		}
		
		if(!isAuthenticated() && Objects.nonNull(password) && password.length > 0) {
			attempted = true;
			authenticate(new PasswordAuthenticator(password), 30000);
		}
		
		if(attempted && !isAuthenticated()) {
			close();
			throw new IOException("Authentication failed");
		}
	}
	
	@SuppressWarnings("unchecked")
	protected final Connection<SshClientContext> doConnect(String hostname, int port, SshClientContext sshContext, long connectTimeout, List<String> keys) throws SshException, IOException {
		configure(sshContext);
		if(onConfigure.isPresent())
			onConfigure.get().accept(sshContext);
		try {
			ConnectRequestFuture future = sshContext.getEngine().connect(hostname, port, sshContext);
			future.waitFor(connectTimeout);
			if(!future.isSuccess()) {
				var lastErr = future.getLastError();
				if(lastErr != null) {
					if(lastErr instanceof IOException)
						throw (IOException)lastErr;
					else if(lastErr instanceof SshException)
						throw (SshException)lastErr;
				}
				throw new IOException(String.format("Failed to connect to %s:%d", hostname, port));
			}
			var con = (Connection<SshClientContext>) future.getConnection();
			con.addEventListener(new EventListener() {
				@Override
				public void processEvent(Event evt) {
					switch(evt.getId()) {
					case EventCodes.EVENT_KEY_EXCHANGE_INIT:
						keys.addAll(Arrays.asList(((String) evt.getAttribute(EventCodes.ATTRIBUTE_REMOTE_PUBLICKEYS)).split(",")));
						break;
					case EventCodes.EVENT_DISCONNECTED:
						disconnect();
						break;
					default:
						break;
					}
				}
			});
			if(!sshContext.getAuthenticators().isEmpty()) {
				con.getAuthenticatedFuture().waitForever();
				if(!con.getAuthenticatedFuture().isSuccess()) {
					close();
					throw new IOException(
							String.format("Failed to authenticate user %s at %s:%d", sshContext.getUsername(), hostname, port));
				}
			}
			return con;
		}
		catch(UnresolvedAddressException uae) {
			UnknownHostException uhe = new UnknownHostException(hostname);
			uhe.initCause(uae);
			throw uhe;
		}
	}
	
	/**
	 * Further configuration the client context.
	 * 
	 * @deprecated
	 * @see SshClientBuilder#onConfigure(Consumer)}.
	 * @param sshContext
	 * @throws SshException
	 * @throws IOException
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	protected void configure(SshClientContext sshContext) throws SshException, IOException {
		
	}

	public synchronized Task addTask(Task task) throws IOException {
		if(con==null) {
			throw new IOException("Client is no longer connected!");
		}
		con.addTask(task);
		return task;
	}

	@Override
	public void close() throws IOException {
		if(closeConnection) {
			con.disconnect();
		}
	}
	
	public SshClientContext getContext() {
		return con.getContext();
	}

	public Connection<SshClientContext> getConnection() {
		return con;
	}
	
	public ForwardingPolicy getForwardingPolicy() {
		return con.getContext().getForwardingPolicy();
	}
	
	public int startLocalForwarding(String addressToBind, String destinationHost) throws UnauthorizedException, SshException {
		return startLocalForwarding(addressToBind, 0, destinationHost, 0);
	}
	
	public int startLocalForwarding(String addressToBind, int portToBind, String destinationHost, int destinationPort) throws UnauthorizedException, SshException {
		ConnectionProtocolClient client = (ConnectionProtocolClient) con.getConnectionProtocol();
		return client.startLocalForwarding(addressToBind, portToBind, destinationHost, destinationPort);
	}
	
	public void stopLocalForwarding(String addressToBind, int portToBind) {
		ConnectionProtocolClient client = (ConnectionProtocolClient) con.getConnectionProtocol();
		client.stopLocalForwarding(addressToBind, portToBind);
	}
	
	public void stopLocalForwarding() {
		ConnectionProtocolClient client = (ConnectionProtocolClient) con.getConnectionProtocol();
		client.stopLocalForwarding();
	}
	
	public int startRemoteForwarding(String addressToBind, int portToBind, String destinationHost, int destinationPort) throws SshException {
		ConnectionProtocolClient client = (ConnectionProtocolClient) con.getConnectionProtocol();
		return client.startRemoteForwarding(addressToBind, portToBind, destinationHost, destinationPort);
	}

	public int startRemoteForwarding(String addressToBind, String destinationHost) throws SshException {
		return startRemoteForwarding(addressToBind, 0, destinationHost, 0);
	}
	
	public void stopRemoteForwarding(String addressToBind, int portToBind) throws SshException {
		ConnectionProtocolClient client = (ConnectionProtocolClient) con.getConnectionProtocol();
		client.stopRemoteForwarding(addressToBind, portToBind);
	}
	
	public void stopRemoteForwarding() throws SshException {
		ConnectionProtocolClient client = (ConnectionProtocolClient) con.getConnectionProtocol();
		client.stopRemoteForwarding();
	}

	public boolean isConnected() {
		return !con.isDisconnected();
	}

	public void disconnect() {
		if(isConnected() && closeConnection) {
			con.disconnect();
		}
	}
	
	protected <T extends Task> T doTask(T task, long timeout) throws IOException {
		addTask(task);
		if(timeout > 0) {
			task.waitFor(timeout);
		} else {
			task.waitForever();
		}
		if(!task.isDone()) {
			throw new IOException("Task did not complete before the specified timeout");
		}
		if(!task.isSuccess()) {
			if(!Objects.isNull(task.getLastError())) {
				if(task.getLastError() instanceof IOException) {
					throw (IOException)task.getLastError();
				}
				else {
					throw new IOException(task.getLastError().getMessage(), task.getLastError());
				}
			} else {
				throw new IOException("Task did not succeed but did not report an error");
			}
			
		}
		return task;
	}
	
	public File getFile(String path) throws IOException {
		return getFile(path, 0);
	}
	
	public File getFile(String path, long timeout) throws IOException {
		return doTask(DownloadFileTaskBuilder.create().withConnection(getConnection()).withRemotePath(path).build(), timeout).getDownloadedFile();
	}
	
	public void getFile(String path, File destination) throws IOException {
		getFile(path, destination, 0L);
	}
	
	public void getFile(String path, File destination, long timeout) throws IOException {
		doTask(DownloadFileTaskBuilder.create().withConnection(getConnection()).withRemotePath(path).withLocalFile(destination).build(), timeout);
	}

	public void putFile(File file) throws IOException {
		putFile(file, file.getName(), 0);
	}
	
	public void putFile(File file, String path) throws IOException {
		putFile(file, path, 0);
	}
	
	public void putFile(File file, String path, long timeout) throws IOException {
		doTask(UploadFileTaskBuilder.create().withConnection(getConnection()).withLocalFile(file).withRemotePath(path).build(), timeout);
	}

	public String executeCommand(String cmd) throws IOException {
		return executeCommand(cmd, 0, "UTF-8");
	}
	
	public String executeCommand(String cmd, long timeout) throws IOException {
		return executeCommand(cmd, timeout, "UTF-8");
	}
	
	public String executeCommand(String cmd, String charset) throws IOException {
		return executeCommand(cmd, 0, charset);
	}
	
	public String executeCommand(String cmd, long timeout, String charset) throws IOException {

		StringBuffer buffer = new StringBuffer();
		executeCommandWithResult(cmd, buffer, timeout, charset);
		return buffer.toString();
	}

	public int executeCommandWithResult(String cmd, StringBuffer buffer) throws IOException {
		return executeCommandWithResult(cmd, buffer, 0L);
	}
	
	public int executeCommandWithResult(String cmd) throws IOException {
		return executeCommandWithResult(cmd, new StringBuffer(), 0L);
	}
	
	public int executeCommandWithResult(String cmd, StringBuffer buffer, long timeout) throws IOException {
		return executeCommandWithResult(cmd, buffer, timeout, "UTF-8");
	}
	
	public int executeCommandWithResult(String cmd, StringBuffer buffer, String charset) throws IOException {
		return executeCommandWithResult(cmd, buffer, 0L, charset);
	}
	
	public int executeCommandWithResult(String cmd, StringBuffer buffer, long timeout, String charset) throws IOException {
		
		InteractiveOutputListener listener = new InteractiveOutputListener(buffer);
		CommandTask task = CommandTaskBuilder.create().
				withCommand(cmd).
				withClient(this).
				withEncoding(charset).
				onBeforeExecute((t, session) -> session.addEventListener(listener)).
				onTask((t, session) -> {
					try {
						while(session.getInputStream().read() > -1);
					} catch (IOException e) {
						throw new IllegalStateException(e.getMessage(), e);				
					}	
				}).
				build();
		
		doTask(task, timeout);
		return task.getExitCode();
	}
	
	class InteractiveOutputListener implements ChannelEventListener {

		StringBuffer output;
		
		InteractiveOutputListener(StringBuffer output) {
			this.output = output;
		}
		
		@Override
		public void onChannelDataIn(Channel channel, ByteBuffer buffer) {
			recordOutput(buffer);
		}

		@Override
		public void onChannelExtendedData(Channel channel, ByteBuffer buffer, int type) {
			recordOutput(buffer);
		}
		
		private synchronized void recordOutput(ByteBuffer buffer) {
			byte[] tmp = new byte[buffer.remaining()];
			buffer.get(tmp);
			try {
				output.append(new String(tmp, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}

	}
	
	public Set<String> getAuthenticationMethods() {
		return sshContext.getAuthenticationClient().getSupportedAuthentications();
	}
	
	public boolean authenticate(ClientAuthenticator authenticator, long timeout) throws IOException, SshException {
		
		if(Log.isDebugEnabled()) {
			Log.debug("Authenticating with {}", authenticator.getName());
		}
		sshContext.getAuthenticationClient().addAuthentication(authenticator);
		authenticator.waitFor(timeout);
		if(Log.isDebugEnabled()) {
			Log.debug("Authentication {}", authenticator.isCancelled() ? "was cancelled" : authenticator.isSuccess() ? "succeeded" : "failed");
		}
		if(authenticator.isCancelled())
			throw new SshException("Authentication cancelled.", SshException.CANCELLED_CONNECTION);
		return authenticator.isDone() && authenticator.isSuccess();
	}

	public boolean isAuthenticated() {
		return con.getAuthenticatedFuture().isDone() && con.getAuthenticatedFuture().isSuccess();
	}

	public <T extends Task> void runTask(T task, long timeout) throws IOException {
		doTask(task, timeout);
	}
	
	public <T extends Task> void runTask(T task) throws IOException {
		doTask(task, 0L);
	}

	public String[] getRemotePublicKeys() {
		return remotePublicKeys;
	}

	public String getRemoteIdentification() {
		return con.getRemoteIdentification();
	}
	
	public String getLocalIdentification() {
		return con.getLocalIdentification();
	}
	
	public String getHost() {
		return hostname;
	}

	public SshPublicKey getHostKey() {
		return con.getHostKey();
	}

	public SessionChannelNG openSessionChannel() throws SshException {
		return openSessionChannel(60000L, false);
	}
	
	public SessionChannelNG openSessionChannel(long timeout) throws SshException {
		return openSessionChannel(timeout, false);
	}
	
	public SessionChannelNG openSessionChannel(boolean autoConsume) throws SshException {
		return openSessionChannel(60000L, autoConsume);
	}
	
	public SessionChannelNG openSessionChannel(long timeout, boolean autoConsume) throws SshException {
		
		SessionChannelNG session = new SessionChannelNG(con, autoConsume);
		con.openChannel(session);
		session.getOpenFuture().waitFor(timeout);
		if(session.getOpenFuture().isSuccess()) {
			return session;
		}
		throw new SshException(String.format("Session was not opened after %d ms timeout threshold", timeout), SshException.SOCKET_TIMEOUT);
	}
	
	public SshClient openRemoteClient(String hostname, int port, String username) throws SshException, IOException, UnauthorizedException {
		
		int localPort = startLocalForwarding("127.0.0.1", 0, hostname, port);
		try {
			return new SshClient("127.0.0.1", localPort, username);
		} finally {
			stopLocalForwarding("127.0.0.1", localPort);
		}
	}

	public int getPort() {
		return port;
	}
}