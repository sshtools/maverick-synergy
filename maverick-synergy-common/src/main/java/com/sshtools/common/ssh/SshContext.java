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
package com.sshtools.common.ssh;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.sshtools.common.forwarding.ForwardingPolicy;
import com.sshtools.common.logger.Log;
import com.sshtools.common.nio.ConnectRequestFuture;
import com.sshtools.common.nio.DefaultSocketConnectionFactory;
import com.sshtools.common.nio.ProtocolContext;
import com.sshtools.common.nio.ProtocolEngine;
import com.sshtools.common.nio.SocketConnectionFactory;
import com.sshtools.common.nio.SshEngine;
import com.sshtools.common.nio.SshEngineContext;
import com.sshtools.common.ssh.components.ComponentFactory;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.SshCipher;
import com.sshtools.common.ssh.components.SshHmac;
import com.sshtools.common.ssh.components.SshKeyExchange;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.common.ssh.compression.NoneCompression;
import com.sshtools.common.ssh.compression.SshCompression;
import com.sshtools.common.util.ByteBufferPool;

/**
 * This class defines an SSH context for listening interfaces on the
 * {@link com.sshtools.common.nio.SshEngine}.
 */
public abstract class SshContext extends ProtocolContext implements
		ExecutorServiceProvider, Context {

	public static final String CIPHER_TRIPLEDES_CBC = "3des-cbc";

	public static final String CIPHER_TRIPLEDES_CTR = "3des-ctr";

	public static final String CIPHER_BLOWFISH_CBC = "blowfish-cbc";

	public static final String CIPHER_AES128_CBC = "aes128-cbc";

	public static final String CIPHER_AES192_CBC = "aes192-cbc";

	public static final String CIPHER_AES256_CBC = "aes256-cbc";

	public static final String CIPHER_AES128_CTR = "aes128-ctr";

	public static final String CIPHER_AES192_CTR = "aes192-ctr";

	public static final String CIPHER_AES256_CTR = "aes256-ctr";

	public static final String CIPHER_ARCFOUR = "arcfour";

	public static final String CIPHER_ARCFOUR_128 = "arcfour128";

	public static final String CIPHER_ARCFOUR_256 = "arcfour256";

	public static final String CIPHER_AES_GCM_128 = "aes128-gcm@openssh.com";
	
	public static final String CIPHER_AES_GCM_256 = "aes256-gcm@openssh.com";
	
	/** SHA1 message authentication **/
	public static final String HMAC_SHA1 = "hmac-sha1";
	public static final String HMAC_SHA1_ETM = "hmac-sha1-etm@openssh.com";
	
	/** SHA1 96 bit message authentication **/
	public static final String HMAC_SHA1_96 = "hmac-sha1-96";

	/** MD5 message authentication **/
	public static final String HMAC_MD5 = "hmac-md5";
	public static final String HMAC_MD5_ETM = "hmac-md5-etm@openssh.com";
	
	/** MD5 96 bit message authentication **/
	public static final String HMAC_MD5_96 = "hmac-md5-96";

	public static final String HMAC_SHA256 = "hmac-sha256";
	public static final String HMAC_SHA256_ETM = "hmac-sha2-256-etm@openssh.com";
	
	public static final String HMAC_SHA256_96 = "hmac-sha2-256-96";

	public static final String HMAC_SHA512 = "hmac-sha2-512";
	public static final String HMAC_SHA512_ETM = "hmac-sha2-512-etm@openssh.com";
	
	public static final String HMAC_SHA512_96 = "hmac-sha2-512-96";
	
	public static final String HMAC_RIPEMD160 = "hmac-ripemd160";
	public static final String HMAC_RIPEMD160_ETM = "hmac-ripemd160-etm@openssh.com";
	
	/** Compression off **/
	public static final String COMPRESSION_NONE = "none";

	/** ZLIB compression **/
	public static final String COMPRESSION_ZLIB = "zlib";

	/**
	 * Constant for the algorithm name "diffie-hellman-group1-sha1".
	 */
	public static final String KEX_DIFFIE_HELLMAN_GROUP1_SHA1 = "diffie-hellman-group1-sha1";

	/**
	 * Constant for the algorithm name "diffie-hellman-group-exchange-sha1".
	 */
	public static final String KEX_DIFFIE_HELLMAN_GROUP_EXCHANGE_SHA1 = "diffie-hellman-group-exchange-sha1";

	public static final String KEX_DIFFIE_HELLMAN_GROUP_EXCHANGE_SHA256 = "diffie-hellman-group-exchange-sha256";

	/** Constant for the algorithm name "diffie-hellman-group14-sha1". */
	public static final String KEX_DIFFIE_HELLMAN_GROUP14_SHA1 = "diffie-hellman-group14-sha1";

	/** Constant for the algorithm name "diffie-hellman-group14-sha256". */
	public static final String KEX_DIFFIE_HELLMAN_GROUP14_SHA256 = "diffie-hellman-group14-sha256";
	
	/** Constant for the algorithm name "diffie-hellman-group15-sha512". */
	public static final String KEX_DIFFIE_HELLMAN_GROUP15_SHA512 = "diffie-hellman-group15-sha512";
	
	/** Constant for the algorithm name "diffie-hellman-group16-sha512". */
	public static final String KEX_DIFFIE_HELLMAN_GROUP16_SHA512 = "diffie-hellman-group16-sha512";
	
	/** Constant for the algorithm name "diffie-hellman-group17-sha512". */
	public static final String KEX_DIFFIE_HELLMAN_GROUP17_SHA512 = "diffie-hellman-group17-sha512";
	
	/** Constant for the algorithm name "diffie-hellman-group18-sha512". */
	public static final String KEX_DIFFIE_HELLMAN_GROUP18_SHA512 = "diffie-hellman-group18-sha512";
	
	public static final String KEX_DIFFIE_HELLMAN_ECDH_NISTP_256 = "ecdh-sha2-nistp256";
	public static final String KEX_DIFFIE_HELLMAN_ECDH_NISTP_384 = "ecdh-sha2-nistp384";
	public static final String KEX_DIFFIE_HELLMAN_ECDH_NISTP_521 = "ecdh-sha2-nistp521";
	
	/** SSH2 DSA Public Key **/
	public static final String PUBLIC_KEY_SSHDSS = "ssh-dss";

	/** SSH2 RSA Public Key **/
	public static final String PUBLIC_KEY_SSHRSA = "ssh-rsa";
	
	/** ECDSA 256 bit Public Key **/
	public static final String PUBLIC_KEY_ECDSA_SHA2_NISPTP_256 = "ecdsa-sha2-nistp256";
	
	/** ECDSA 384 bit Public Key **/
	public static final String PUBLIC_KEY_ECDSA_SHA2_NISPTP_384 = "ecdsa-sha2-nistp384";
	
	/** ECDSA 521 bit Public Key **/
	public static final String PUBLIC_KEY_ECDSA_SHA2_NISPTP_521 = "ecdsa-sha2-nistp521";

	/** Identifier for password authentication **/
	public static final String PASSWORD_AUTHENTICATION = "password";

	/** Identifier for public key authentication **/
	public static final String PUBLICKEY_AUTHENTICATION = "publickey";

	public static final String KEYBOARD_INTERACTIVE_AUTHENTICATION = "keyboard-interactive";

	protected int maximumSocketsBacklogPerRemotelyForwardedConnection = 50;
	protected SocketConnectionFactory socketConnectionFactory = new DefaultSocketConnectionFactory();
	
	protected ComponentFactory<SshCompression> compressionsCS;
	protected ComponentFactory<SshCompression> compressionsSC;
	protected ComponentFactory<SshCipher> ciphersCS;
	protected ComponentFactory<SshCipher> ciphersSC;
	protected ComponentFactory<SshKeyExchange<? extends SshContext>> keyExchanges;
	protected ComponentFactory<SshHmac> macCS;
	protected ComponentFactory<SshHmac> macSC;
	protected ComponentFactory<SshPublicKey> publicKeys;

	protected String prefCipherCS = CIPHER_AES256_CTR;
	protected String prefCipherSC = CIPHER_AES256_CTR;

	protected String prefMacCS = HMAC_SHA256;
	protected String prefMacSC = HMAC_SHA256;

	protected String prefCompressionCS = COMPRESSION_NONE;
	protected String prefCompressionSC = COMPRESSION_NONE;

	protected String prefKeyExchange = KEX_DIFFIE_HELLMAN_GROUP_EXCHANGE_SHA256;
	protected String prefPublicKey = PUBLIC_KEY_ECDSA_SHA2_NISPTP_256;

	protected int maxChannels = 100;

	protected int compressionLevel = 6;
	protected int maximumPacketLength = 131072 + 256; // Add overhead to support clients
											// using 128k file blocks
	protected long MAX_NUM_PACKETS_BEFORE_REKEY = 2147483647;
	protected long MAX_NUM_BYTES_BEFORE_REKEY = 1073741824;
	
	protected SshEngine daemon;

	protected String softwareVersionComments = "MaverickSynergy";

	protected boolean killTunnelsOnRemoteForwardingCancel = false;
	
	protected boolean sendIgnorePacketOnIdle = false;
	protected int idleConnectionTimeout = 0;
	protected int idleAuthenticationTimeoutSeconds = 30;
	protected int keepAliveInterval = 30;
	protected int keepAliveDataMaxLength = 128;

	protected static ExecutorService executor;
	
	protected Locale locale = Locale.getDefault();
	protected ByteBufferPool byteBufferPool = null;
	
	protected int minDHGroupExchangeKeySize = 2048;
	protected int preferredDHGroupExchangeKeySize = 2048;
	protected int maxDHGroupExchangeKeySize = 8192;
	
	List<ExecutorOperationListener> listeners = new ArrayList<ExecutorOperationListener>();
	
	protected ComponentManager componentManager;
	
	boolean httpRedirect;
	String httpRedirectUrl;
	
	Map<Class<?>,Object> policies = new HashMap<>();
	
	/** Constructs a default context but does not set the daemon 
	 * @param componentManager */
	public SshContext(ComponentManager componentManager) throws IOException {
		
		this.componentManager = componentManager;
		
		keyExchanges =  new ComponentFactory<SshKeyExchange<?>>(componentManager);
		
		ciphersCS = ComponentManager.getDefaultInstance().supportedSsh2CiphersCS();
		ciphersSC = ComponentManager.getDefaultInstance().supportedSsh2CiphersSC();
		macCS = ComponentManager.getDefaultInstance().supportedHMacsCS();
		macSC = ComponentManager.getDefaultInstance().supportedHMacsSC();
		publicKeys = ComponentManager.getDefaultInstance().supportedPublicKeys();

		try {

			compressionsCS = new ComponentFactory<SshCompression>(componentManager);
			compressionsCS.add(COMPRESSION_NONE, NoneCompression.class);

			JCEComponentManager.getDefaultInstance().loadExternalComponents("zip.properties", compressionsCS);
			
			compressionsSC = new ComponentFactory<SshCompression>(componentManager);
			compressionsSC.add(COMPRESSION_NONE, NoneCompression.class);
			
			JCEComponentManager.getDefaultInstance().loadExternalComponents("zip.properties", compressionsSC);

		} catch (Throwable t) {
			throw new IOException(t.getMessage() != null ? t.getMessage() : t
					.getClass().getName());
		}

		configureKeyExchanges();
		
		this.keepAlive = true;
		this.tcpNoDelay = true;

	}

	/** Initialise the SshContext by setting the daemon */
	public void init(SshEngine daemon) {
		this.daemon = daemon;
	}

	/**
	 * Constructs a default context
	 * @param componentManager 
	 * 
	 * @throws IOException
	 */
	public SshContext(SshEngine daemon, ComponentManager componentManager) throws IOException {
		this(componentManager);
		init(daemon);
	}
	
	public abstract ConnectionManager<? extends SshContext> getConnectionManager();
	
	public abstract ProtocolEngine createEngine(ConnectRequestFuture connectFuture) throws IOException;

	public abstract String getSupportedPublicKeys();
	
	protected abstract void configureKeyExchanges();
	
	public abstract String getPreferredPublicKey();
	
	public abstract ChannelFactory<? extends SshContext> getChannelFactory();
	
	@SuppressWarnings("unchecked")
	public <P> P getPolicy(Class<P> clz) {
		try {
			if(!policies.containsKey(clz)) {
				policies.put(clz, clz.newInstance());
			}
			
			return (P) policies.get(clz);
		} catch (InstantiationException | IllegalAccessException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public <P> P getPolicy(Class<P> clz, P defaultValue) {

		if(!policies.containsKey(clz)) {
			policies.put(clz, defaultValue);
		}
		
		return (P) policies.get(clz);

	}
	
	@Override
	public <P> void setPolicy(Class<P> clz, P policy) {
		policies.put(clz, policy);
	}
	
	@Override
	public boolean hasPolicy(Class<?> clz) {
		return policies.containsKey(clz);
	}
	
	public ComponentManager getComponentManager() {
		return componentManager;
	}
	
	public synchronized void addOperationListener(ExecutorOperationListener listener) {
		listeners.add(listener);
	}
	
	public synchronized void removeOperationListener(ExecutorOperationListener listener) {
		listeners.remove(listener);
	}
	
	@Override
	public synchronized List<ExecutorOperationListener> getExecutorListeners() {
		return new ArrayList<ExecutorOperationListener>(listeners);
	}
	
	public void setSocketConnectionFactory(SocketConnectionFactory socketConnectionFactory) {
		this.socketConnectionFactory = socketConnectionFactory;
	}
	
	public SocketConnectionFactory getSocketConnectionFactory() {
		return socketConnectionFactory;
	}

	public abstract GlobalRequestHandler<? extends SshContext> getGlobalRequestHandler(String requestname);
	
	/**
	 * Get the instance of the SSHD for this context.
	 * 
	 * @return Daemon
	 */
	public SshEngine getEngine() {
		return daemon;
	}


	/**
	 * Set the maximum number of open channels allowed by each client (defaults
	 * to 100).
	 * 
	 * @param maxChannels
	 */
	public void setChannelLimit(int maxChannels) {
		this.maxChannels = maxChannels;
	}

	/**
	 * Get the maximum number of open channels allowed by each client.
	 * 
	 * @return int
	 */
	public int getChannelLimit() {
		return maxChannels;
	}




	/**
	 * <p>
	 * Returns a factory implementation that enables configuration of the
	 * available ciphers.
	 * </p>
	 * 
	 * <p>
	 * The standard default ciphers installed are 3DES and Blowfish, however the
	 * J2SSH Maverick API on which this server is based also supports a number
	 * of optional ciphers AES, CAST and Twofish. These can be installed by
	 * adding the <em>sshtools-cipher.jar</em> to your class path and using the
	 * following code within your SSHD
	 * {@link SshDaemon#configure(ConfigurationContext)} method. <blockquote>
	 * 
	 * <pre>
	 * // import the cipher package
	 * import com.sshtools.cipher.*;
	 * 
	 * // Add AES
	 * context.supportedCiphers().add(AES128Cbc.AES128_CBC, AES128Cbc.class);
	 * context.supportedCiphers().add(AES192Cbc.AES192_CBC, AES192Cbc.class);
	 * context.supportedCiphers().add(AES256Cbc.AES256_CBC, AES256Cbc.class);
	 * 
	 * // Add Twofish - note the 256 bit cipher has two different entries to maintain backwards compatibility
	 * context.supportedCiphers().add(Twofish128Cbc.TWOFISH128_CBC, Twofish128Cbc.class);
	 * context.supportedCiphers().add(Twofish192Cbc.TWOFISH192_CBC, Twofish192Cbc.class);
	 * context.supportedCiphers().add(Twofish256Cbc.TWOFISH256_CBC, Twofish256Cbc.class);
	 * context.supportedCiphers().add(Twofish256Cbc.TWOFISH_CBC, Twofish256Cbc.class);
	 * 
	 * // Add CAST
	 *  context.supportedCiphers().add(CAST128Cbc.CAST128_CBC, CAST128Cbc.class);
	 * </pre>
	 * 
	 * </blockquote>
	 * 
	 * @return the component factory
	 */
	public ComponentFactory<SshCipher> supportedCiphersCS() {
		return ciphersCS;
	}

	public ComponentFactory<SshCipher> supportedCiphersSC() {
		return ciphersSC;
	}

	/**
	 * Get the currently preferred cipher for the Client->Server stream.
	 * 
	 * @return the preferred Client-Server cipher
	 */
	public String getPreferredCipherCS() {
		return prefCipherCS;
	}

	/**
	 * <p>
	 * Set the preferred cipher for the Client->Server stream.
	 * </p>
	 * 
	 * <p>
	 * Use the static fields available within this class (or the
	 * com.sshtools.cipher classes) to identify the correct cipher. <blockquote>
	 * 
	 * </pre>
	 * 
	 * context.setPreferredCipherCS(ConfigurationContext.CIPHER_BLOWFISH_CBC);
	 * 
	 * </pre>
	 * 
	 * </blockquote> <br>
	 * <em>The default cipher is 3DES</em>
	 * </p>
	 * 
	 * @param name
	 * @throws IOException
	 *             if the cipher is not supported
	 * @throws SshException
	 */
	public void setPreferredCipherCS(String name) throws IOException,
			SshException {
		if (ciphersCS.contains(name)) {
			prefCipherCS = name;
			setCipherPreferredPositionCS(name, 0);
		} else {
			throw new IOException(name + " is not supported");
		}
	}



	/**
	 * Get the currently preferred cipher for the Server->Client stream.
	 * 
	 * @return the preferred Server-Client cipher
	 */
	public String getPreferredCipherSC() {
		return prefCipherSC;
	}

	/**
	 * Get the software/version/comments field that is to be used in the SSH
	 * protocols negotiation procedure.
	 * 
	 * @return String
	 */
	public String getSoftwareVersionComments() {
		return softwareVersionComments;
	}

	/**
	 * Set the current implementations software/version/comments field that is
	 * used during the SSH protocols negotiation procedure. This value MUST
	 * consist of printable US-ASCII characters with the exception of whitespace
	 * and the minus sign (-) and be no longer than 200 characters.
	 * 
	 * @param softwareVersionComments
	 */
	public void setSoftwareVersionComments(String softwareVersionComments) {
		this.softwareVersionComments = softwareVersionComments;
	}

	/**
	 * <p>
	 * Set the preferred cipher for the Server->Client stream.
	 * </p>
	 * 
	 * <p>
	 * Use the static fields available within this class (or the
	 * com.sshtools.cipher classes) to identify the correct cipher. <blockquote>
	 * 
	 * </pre>
	 * 
	 * context.setPreferredCipherSC(ConfigurationContext.CIPHER_BLOWFISH_CBC);
	 * 
	 * </pre>
	 * 
	 * </blockquote> <br>
	 * <em>The default cipher is 3DES</em>
	 * </p>
	 * 
	 * @param name
	 * @throws IOException
	 * @throws SshException
	 */
	public void setPreferredCipherSC(String name) throws IOException,
			SshException {
		if (ciphersSC.contains(name)) {
			prefCipherSC = name;
			setCipherPreferredPositionSC(name, 0);
		} else {
			throw new IOException(name + " is not supported");
		}
	}

	/**
	 * <p>
	 * Get the supported message authentication algorithms.
	 * </p>
	 * 
	 * <p>
	 * <em>There are no optional MAC algorithms currently available and this method is
	 * supplied in preperation for future enhancements.</em>
	 * </p>
	 * 
	 * @return the component factory
	 */
	public ComponentFactory<SshHmac> supportedMacsCS() {
		return macCS;
	}

	public ComponentFactory<SshHmac> supportedMacsSC() {
		return macSC;
	}

	/**
	 * Get the currently preferred mac for the Client->Server stream.
	 * 
	 * @return the preferred Client-Server mac
	 */
	public String getPreferredMacCS() {
		return prefMacCS;
	}

	/**
	 * <p>
	 * Set the preferred MAC for the Client->Server stream.
	 * </p>
	 * 
	 * <p>
	 * Use the static fields available within this class to identify the correct
	 * MAC. <blockquote>
	 * 
	 * </pre>
	 * 
	 * context.setPreferredMacCS(ConfigurationContext.HMAC_MD5);
	 * 
	 * </pre>
	 * 
	 * </blockquote> <br>
	 * <em>The default MAC is HMAC_SHA1</em>
	 * </p>
	 * 
	 * @param name
	 * @throws IOException
	 * @throws SshException
	 */
	public void setPreferredMacCS(String name) throws IOException, SshException {
		if (macCS.contains(name)) {
			prefMacCS = name;
			setMacPreferredPositionCS(name, 0);
		} else {
			throw new IOException(name + " is not supported");
		}
	}

	/**
	 * Get the currently supported mac for the Server-Client stream.
	 * 
	 * @return the preferred Server-Client mac
	 */
	public String getPreferredMacSC() {
		return prefMacSC;
	}

	/**
	 * When the user cancels a remote forwarding should active tunnels be
	 * dropped?
	 * 
	 * @param killTunnelsOnRemoteForwardingCancel
	 *            boolean
	 */
	public void setRemoteForwardingCancelKillsTunnels(
			boolean killTunnelsOnRemoteForwardingCancel) {
		this.killTunnelsOnRemoteForwardingCancel = killTunnelsOnRemoteForwardingCancel;
	}

	/**
	 * Determines whether the cancellation of a remote forwarding drops
	 * currently active tunnels
	 * 
	 * @return boolean
	 */
	public boolean getRemoteForwardingCancelKillsTunnels() {
		return killTunnelsOnRemoteForwardingCancel;
	}



	/**
	 * <p>
	 * Set the preferred mac for the Server->Client stream.
	 * </p>
	 * 
	 * <p>
	 * Use the static fields available within this class to identify the correct
	 * MAC. <blockquote>
	 * 
	 * </pre>
	 * 
	 * context.setPreferredMacCS(ConfigurationContext.HMAC_MD5);
	 * 
	 * </pre>
	 * 
	 * </blockquote> <br>
	 * <em>The default MAC is HMAC_SHA1</em>
	 * </p>
	 * 
	 * @param name
	 * @throws IOException
	 * @throws SshException
	 */
	public void setPreferredMacSC(String name) throws IOException, SshException {
		if (macSC.contains(name)) {
			prefMacSC = name;
			setMacPreferredPositionSC(name, 0);
		} else {
			throw new IOException(name + " is not supported");
		}
	}

	/**
	 * <p>
	 * Get the supported compression algorithms.
	 * </p>
	 * 
	 * <p>
	 * <em>There are
	 * no optional compression algorithms currently available and this method is
	 * supplied in preperation for future enhancements.</em>
	 * </p>
	 * 
	 * @return the component factory
	 */
	public ComponentFactory<SshCompression> supportedCompressionsCS() {
		return compressionsCS;
	}

	public ComponentFactory<SshCompression> supportedCompressionsSC() {
		return compressionsSC;
	}

	/**
	 * Get the currently preferred compression for the Client->Server stream.
	 * 
	 * @return the preferred Client-Server compression
	 */
	public String getPreferredCompressionCS() {
		return prefCompressionCS;
	}

	/**
	 * <p>
	 * Set the preferred compression for the Client->Server stream.
	 * </p>
	 * 
	 * <p>
	 * <em>It is recommended that you do not set the preferred compression
	 * so that the client has control over the compression selection.</em>
	 * </p>
	 * 
	 * @param name
	 * @throws IOException
	 */
	public void setPreferredCompressionCS(String name) throws IOException {
		if (compressionsCS.contains(name)) {
			prefCompressionCS = name;
		} else {
			throw new IOException(name + " is not supported");
		}
	}

	/**
	 * Get the currently preferred compression for the Server->Client stream.
	 * 
	 * @return the preferred Server->Client compression
	 */
	public String getPreferredCompressionSC() {
		return prefCompressionSC;
	}

	/**
	 * <p>
	 * Set the preferred compression for the Server->Client stream.
	 * </p>
	 * 
	 * <p>
	 * <em>It is recommended that you do not set the preferred compression
	 * so that the client has control over the compression selection.</em>
	 * </p>
	 * 
	 * @param name
	 * @throws IOException
	 */
	public void setPreferredCompressionSC(String name) throws IOException {
		if (compressionsSC.contains(name)) {
			prefCompressionSC = name;
		} else {
			throw new IOException(name + " is not supported");
		}
	}

	/**
	 * <p>
	 * Get the supported key exchange methods.
	 * </p>
	 * 
	 * <p>
	 * <em>There are
	 * no optional key exchange algorithms currently available and this method is
	 * supplied in preperation for future enhancements.</em>
	 * </p>
	 * 
	 * @return the component factory
	 */
	public ComponentFactory<SshKeyExchange<? extends SshContext>> supportedKeyExchanges() {
		return keyExchanges;
	}

	/**
	 * Get the currently preferred key exchange method.
	 * 
	 * @return the preferred key exhcange
	 */
	public String getPreferredKeyExchange() {
		return prefKeyExchange;
	}

	/**
	 * <p>
	 * Set the preferred key exchange method.
	 * </p>
	 * 
	 * <p>
	 * <em>There is only one supported key exchange algorithm and as such this
	 * method is supplied in preperation for future enhancements.</em>
	 * </p>
	 * 
	 * @param name
	 * @throws IOException
	 * @throws SshException
	 */
	public void setPreferredKeyExchange(String name) throws IOException,
			SshException {
		if (keyExchanges.contains(name)) {
			prefKeyExchange = name;
			setKeyExchangePreferredPosition(name, 0);
		} else {
			throw new IOException(name + " is not supported");
		}
	}






	/**
	 * Set the compression level to use if compression is enabled
	 * 
	 * @param compressionLevel
	 *            int
	 */
	public void setCompressionLevel(int compressionLevel) {
		this.compressionLevel = compressionLevel;
	}

	/**
	 * Get the current compression level
	 * 
	 * @return int
	 */
	public int getCompressionLevel() {
		return compressionLevel;
	}

	public int getMaximumSocketsBacklogPerRemotelyForwardedConnection() {
		return maximumSocketsBacklogPerRemotelyForwardedConnection;
	}

	public void setMaximumSocketsBacklogPerRemotelyForwardedConnection(
			int maximumSocketsBacklogPerRemotelyForwardedConnection) {
		this.maximumSocketsBacklogPerRemotelyForwardedConnection = maximumSocketsBacklogPerRemotelyForwardedConnection;
	}

	/**
	 * Get the ciphers for the Server->Client stream.
	 * 
	 * @return the Server-Client ciphers in order of preference
	 */
	public String getCiphersSC() {
		return ciphersSC.list(prefCipherSC);
	}

	public String getCiphersCS() {
		return ciphersCS.list(prefCipherCS);
	}

	/**
	 * Get the ciphers for the Client->Server stream.
	 * 
	 * @return the Client-Server ciphers in order of preference
	 */
	public String getMacsCS() {
		return macCS.list(prefMacCS);
	}

	/**
	 * Get the ciphers for the Server->Client stream.
	 * 
	 * @return the Server-Client ciphers in order of preference
	 */
	public String getMacsSC() {
		return macSC.list(prefMacSC);
	}

	/**
	 * Get the ciphers for the Server->Client stream.
	 * 
	 * @return the Server-Client ciphers in order of preference
	 */
	public String getPublicKeys() {
		return publicKeys.list(prefPublicKey);
	}

	/**
	 * Get the ciphers for the Server->Client stream.
	 * 
	 * @return the Server-Client ciphers in order of preference
	 */
	public String getKeyExchanges() {
		return keyExchanges.list(prefKeyExchange);
	}

	/**
	 * Set the preferred SC cipher order
	 * 
	 * @param order
	 *            , list of indices to be moved to the top.
	 * @throws SshException
	 */
	public void setPreferredCipherSC(int[] order) throws SshException {
		prefCipherSC = ciphersSC.createNewOrdering(order);
	}
	
	/**
	 * Set the preferred SC cipher order
	 * @param order
	 * @throws SshException
	 */
	public void setPreferredCipherSC(String[] order) throws SshException {
		prefCipherSC = ciphersSC.order(order);
	}

	/**
	 * Set the preferred SC cipher order
	 * 
	 * @param order
	 *            , list of indices to be moved to the top.
	 * @throws SshException
	 */
	public void setPreferredCipherCS(int[] order) throws SshException {
		prefCipherCS = ciphersCS.createNewOrdering(order);
	}

	/**
	 * Set the preferred CS cipher order
	 * @param order
	 * @throws SshException
	 */
	public void setPreferredCipherCS(String[] order) throws SshException {
		prefCipherCS = ciphersCS.order(order);
	}
	
	/**
	 * Set the preferred SC Mac order
	 * 
	 * @param order
	 *            , list of indices to be moved to the top.
	 * @throws SshException
	 */
	public void setPreferredMacSC(int[] order) throws SshException {
		prefMacSC = macSC.createNewOrdering(order);
	}
	
	/**
	 * 
	 * @param order
	 * @throws SshException
	 */
	public void setPreferredMacSC(String[] order) throws SshException {
		prefMacSC = macSC.order(order);
	}

	/**
	 * 
	 * @param order
	 * @throws SshException
	 */
	public void setPreferredKeyExchange(String[] order) throws SshException {
		prefKeyExchange = keyExchanges.order(order);
	}
	
	/**
	 * Set the preferred CS Mac order
	 * 
	 * @param order
	 *            , list of indices to be moved to the top.
	 * @throws SshException
	 */
	public void setPreferredMacCS(int[] order) throws SshException {
		prefMacSC = macCS.createNewOrdering(order);
	}
	
	public void setPreferredMacCS(String[] order) throws SshException {
		prefMacCS = macCS.order(order);
	}

	public void setCipherPreferredPositionCS(String name, int position)
			throws SshException {
		prefCipherCS = ciphersCS.changePositionofAlgorithm(name, position);
	}

	public void setCipherPreferredPositionSC(String name, int position)
			throws SshException {
		prefCipherSC = ciphersSC.changePositionofAlgorithm(name, position);
	}

	public void setMacPreferredPositionSC(String name, int position)
			throws SshException {
		prefMacSC = macSC.changePositionofAlgorithm(name, position);
	}

	public void setMacPreferredPositionCS(String name, int position)
			throws SshException {
		prefMacCS = macCS.changePositionofAlgorithm(name, position);
	}

	public void setPublicKeyPreferredPosition(String name, int position)
			throws SshException {
		prefMacCS = publicKeys.changePositionofAlgorithm(name, position);
	}

	public void setKeyExchangePreferredPosition(String name, int position)
			throws SshException {
		prefMacCS = keyExchanges.changePositionofAlgorithm(name, position);
	}

	/**
	 * Set the maximum supported length of an SSH packet.
	 * 
	 * @param maximumPacketLength
	 *            int
	 */
	public void setMaximumPacketLength(int maximumPacketLength) {
		this.maximumPacketLength = maximumPacketLength;
	}

	/**
	 * Get the maximum supported length of an SSH packet.
	 * 
	 * @return int
	 */
	public int getMaximumPacketLength() {
		return maximumPacketLength;
	}

	/**
	 * This limit tells the server when to force a key exchange.
	 * 
	 * @param MAX_NUM_BYTES_BEFORE_REKEY
	 *            int
	 */
	public void setKeyExchangeTransferLimit(long MAX_NUM_BYTES_BEFORE_REKEY) {

		if (MAX_NUM_BYTES_BEFORE_REKEY < 1024000)
			throw new IllegalArgumentException(
					"The minimum number of bytes allowed between key exchange is 1MB (1024000 bytes)");
		this.MAX_NUM_BYTES_BEFORE_REKEY = MAX_NUM_BYTES_BEFORE_REKEY;
	}

	/**
	 * This tells the server how many packets to use before a key exchange.
	 * 
	 * @param MAX_NUM_PACKETS_BEFORE_REKEY
	 *            int
	 */
	public void setKeyExchangePacketLimit(int MAX_NUM_PACKETS_BEFORE_REKEY) {

		if (MAX_NUM_PACKETS_BEFORE_REKEY < 100)
			throw new IllegalArgumentException(
					"The minimum number of packets allowed between key exchanges is 100");
		this.MAX_NUM_PACKETS_BEFORE_REKEY = MAX_NUM_PACKETS_BEFORE_REKEY;
	}

	/**
	 * Get the number of bytes to transfer before a key exchange is forced.
	 * 
	 * @return int
	 */
	public long getKeyExchangeTransferLimit() {
		return MAX_NUM_BYTES_BEFORE_REKEY;
	}

	/**
	 * Get the number of packets to send before a key exchange is forced
	 * 
	 * @return int
	 */
	public long getKeyExchangePacketLimit() {
		return MAX_NUM_PACKETS_BEFORE_REKEY;
	}

	/**
	 * Should the connection be disconnected on session timeout?
	 * 
	 * @return
	 */
	public int getIdleConnectionTimeoutSeconds() {
		return idleConnectionTimeout;
	}

	/**
	 * Inform the context that the connection should be disconnected on session
	 * timeout
	 * 
	 * @param idleConnectionTimeout
	 */
	public void setIdleConnectionTimeoutSeconds(int idleConnectionTimeout) {
		this.idleConnectionTimeout = idleConnectionTimeout;
	}

	public ComponentFactory<SshPublicKey> supportedPublicKeys() {
		return publicKeys;
	}

	/**
	 * Get the current keep-alive interval (in seconds). The server sends an
	 * SSH_MSG_IGNORE every n seconds after no activity on a connection.
	 * 
	 * @return
	 */
	public int getKeepAliveInterval() {
		return keepAliveInterval;
	}

	/**
	 * Set the keep-alive interval (in seconds). The server sends an
	 * SSH_MSG_IGNORE message every n seconds after no activity on a connection.
	 * 
	 * @param keepAliveInterval
	 */
	public void setKeepAliveInterval(int keepAliveInterval) {
		this.keepAliveInterval = keepAliveInterval;
	}

	/**
	 * Get the maximum data length for the keep-alive packet.
	 * 
	 * @return
	 */
	public int getKeepAliveDataMaxLength() {
		return keepAliveDataMaxLength;
	}

	/**
	 * Set the maximum data length for the keep-alive packet. Default is 128,
	 * the actual number of bytes is random up to this maximum.
	 * 
	 * @param keepAliveDataMaxLength
	 */
	public void setKeepAliveDataMaxLength(int keepAliveDataMaxLength) {
		this.keepAliveDataMaxLength = keepAliveDataMaxLength;
	}
	


	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public Locale getLocale() {
		return locale;
	}

	public void enableFIPSMode() throws SshException {

		if(Log.isInfoEnabled())
			Log.info("Enabling FIPS mode");

		if (!keyExchanges.contains(SshContext.KEX_DIFFIE_HELLMAN_GROUP14_SHA1)) {
			throw new SshException(
					"Cannot enable FIPS mode because diffie-hellman-group14-sha1 "
							+ "keyexchange was not supported by this configuration. "
							+ "Install a JCE Provider that supports a prime size of 2048 bits (for example BouncyCastle provider)",
					SshException.BAD_API_USAGE);
		}

		Vector<String> allowed = new Vector<String>();
		allowed.addElement(SshContext.KEX_DIFFIE_HELLMAN_GROUP14_SHA1);

		String[] names = keyExchanges.toArray();
		for (int i = 0; i < names.length; i++) {
			if (!allowed.contains(names[i])) {
				if(Log.isInfoEnabled())
					Log.info("Removing key exchange " + names[i]);
				keyExchanges.remove(names[i]);
			}
		}

		keyExchanges.lockComponents();

		allowed.clear();

		allowed.addElement(SshContext.CIPHER_AES128_CBC);
		allowed.addElement(SshContext.CIPHER_AES192_CBC);
		allowed.addElement(SshContext.CIPHER_AES256_CBC);
		allowed.addElement(SshContext.CIPHER_TRIPLEDES_CBC);

		names = ciphersCS.toArray();
		for (int i = 0; i < names.length; i++) {
			if (!allowed.contains(names[i])) {
				if(Log.isInfoEnabled())
					Log.info("Removing cipher client->server " + names[i]);
				ciphersCS.remove(names[i]);
			}
		}

		ciphersCS.lockComponents();

		names = ciphersSC.toArray();
		for (int i = 0; i < names.length; i++) {
			if (!allowed.contains(names[i])) {
				if(Log.isInfoEnabled())
					Log.info("Removing cipher server->client " + names[i]);
				ciphersSC.remove(names[i]);
			}
		}

		ciphersSC.lockComponents();

		allowed.clear();

		allowed.addElement(SshContext.PUBLIC_KEY_SSHRSA);

		names = publicKeys.toArray();
		for (int i = 0; i < names.length; i++) {
			if (!allowed.contains(names[i])) {
				if(Log.isInfoEnabled())
					Log.info("Removing public key " + names[i]);
				publicKeys.remove(names[i]);
			}
		}

		publicKeys.lockComponents();

		allowed.clear();

		allowed.addElement(SshContext.HMAC_SHA1);
		allowed.addElement(SshContext.HMAC_SHA256);
		allowed.addElement("hmac-sha256@ssh.com");

		names = macCS.toArray();
		for (int i = 0; i < names.length; i++) {
			if (!allowed.contains(names[i])) {
				if(Log.isInfoEnabled())
					Log.info("Removing mac client->server " + names[i]);
				macCS.remove(names[i]);
			}
		}

		macCS.lockComponents();

		names = macSC.toArray();
		for (int i = 0; i < names.length; i++) {
			if (!allowed.contains(names[i])) {
				if(Log.isInfoEnabled())
					Log.info("Removing mac server->client " + names[i]);
				macSC.remove(names[i]);
			}
		}

		macCS.lockComponents();

	}

	public ExecutorService getExecutorService() {
		if (executor == null) {
			ExecutorService executor = Executors.newCachedThreadPool(new ThreadFactory() {
	            public Thread newThread(Runnable r) {
	                Thread t = Executors.defaultThreadFactory().newThread(r);
	                t.setDaemon(true);
	                return t;
	            }
	        });
			if(!Objects.isNull(daemon)) {
				daemon.addShutdownHook(new Runnable() {
					public void run() {
						shutdown();
					}
				});
			} else {
				Runtime.getRuntime().addShutdownHook(new Thread() {
					public void run() {
						shutdown();
					}
				});
			}
			SshContext.executor = executor;
		}
		return executor;
	}

	public void shutdown() {
		getExecutorService().shutdown();
		try {
			getExecutorService().awaitTermination(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		}
	}

	public synchronized ByteBufferPool getByteBufferPool() {
		if (byteBufferPool == null) {
			byteBufferPool = new ByteBufferPool(getMaximumPacketLength(),
					getEngine().getContext().isUsingDirectBuffers());
		}
		return byteBufferPool;
	}

	public SshEngineContext getDaemonContext() {
		return daemon.getContext();
	}

	public int getIdleAuthenticationTimeoutSeconds() {
		return idleAuthenticationTimeoutSeconds;
	}
	
	public void setIdleAuthenticationTimeoutSeconds(int idleAuthenticationTimeoutSeconds) {
		this.idleAuthenticationTimeoutSeconds = idleAuthenticationTimeoutSeconds;
	}

	public int getMinDHGroupExchangeKeySize() {
		return minDHGroupExchangeKeySize;
	}

	public void setMinDHGroupExchangeKeySize(int minDHGroupExchangeKeySize) {
		this.minDHGroupExchangeKeySize = minDHGroupExchangeKeySize;
	}

	public abstract ForwardingManager<? extends SshContext> getForwardingManager();
	
	protected String listPublicKeys(String... keys) {
		
		String list = "";
		for (String key : keys) {
			if (!key.equals(prefPublicKey)) {
				list += (list.length() == 0 ? "" : ",") + key;
			} else {
				list = prefPublicKey + (list.length() == 0 ? "" : ",") + list;
			}
		}
		return list;
	}
	
	public boolean isSendIgnorePacketOnIdle() {
		return sendIgnorePacketOnIdle;
	}
	
	public void setSendIgnorePacketOnIdle(boolean sendIgnorePacketOnIdle) {
		this.sendIgnorePacketOnIdle = sendIgnorePacketOnIdle;
	}

	public boolean isHttpRedirect() {
		return httpRedirect;
	}

	public void setHttpRedirect(boolean httpRedirect) {
		this.httpRedirect = httpRedirect;
	}

	public String getHttpRedirectUrl() {
		return httpRedirectUrl;
	}

	public void setHttpRedirectUrl(String httpRedirectUrl) {
		this.httpRedirectUrl = httpRedirectUrl;
	}

	public int getPreferredDHGroupExchangeKeySize() {
		return preferredDHGroupExchangeKeySize;
	}

	public void setPreferredDHGroupExchangeKeySize(int preferredDHGroupExchangeKeySize) {
		this.preferredDHGroupExchangeKeySize = preferredDHGroupExchangeKeySize;
	}

	public int getMaxDHGroupExchangeKeySize() {
		return maxDHGroupExchangeKeySize;
	}

	public void setMaxDHGroupExchangeKeySize(int maxDHGroupExchangeKeySize) {
		this.maxDHGroupExchangeKeySize = maxDHGroupExchangeKeySize;
	}
	
	public ForwardingPolicy getForwardingPolicy() {
		return getPolicy(ForwardingPolicy.class);
	}
}
