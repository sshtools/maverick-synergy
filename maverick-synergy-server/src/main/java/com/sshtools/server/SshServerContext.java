/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */

package com.sshtools.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import com.sshtools.common.auth.AuthenticationMechanismFactory;
import com.sshtools.common.auth.DefaultAuthenticationMechanismFactory;
import com.sshtools.common.auth.DefaultPublicKeyAuthenticationVerifier;
import com.sshtools.common.auth.PublicKeyAuthenticationVerifier;
import com.sshtools.common.logger.Log;
import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.publickey.SshKeyPairGenerator;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.publickey.SshPrivateKeyFile;
import com.sshtools.common.publickey.SshPrivateKeyFileFactory;
import com.sshtools.common.publickey.SshPublicKeyFile;
import com.sshtools.common.publickey.SshPublicKeyFileFactory;
import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.ComponentFactory;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.SshCertificate;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.common.ssh.components.jce.Ssh2RsaPublicKey;
import com.sshtools.server.components.SshKeyExchangeServer;
import com.sshtools.server.components.SshKeyExchangeServerFactory;
import com.sshtools.synergy.nio.ConnectRequestFuture;
import com.sshtools.synergy.nio.ProtocolEngine;
import com.sshtools.synergy.nio.SshEngine;
import com.sshtools.synergy.ssh.ChannelFactory;
import com.sshtools.synergy.ssh.ConnectionManager;
import com.sshtools.synergy.ssh.ForwardingManager;
import com.sshtools.synergy.ssh.GlobalRequestHandler;
import com.sshtools.synergy.ssh.SshContext;
import com.sshtools.synergy.ssh.components.SshKeyExchange;

public class SshServerContext extends SshContext {

	Map<String, SshKeyPair> hostkeys = new ConcurrentHashMap<String, SshKeyPair>(8, 0.9f, 1);
	
	boolean ensureGracefulDisconnect = false;

	ForwardingManager<SshServerContext> forwardingManager;
	ConnectionManager<SshServerContext> connectionManager;
	static ForwardingManager<SshServerContext> globalForwardingManager = new ForwardingManager<>();
	static ConnectionManager<SshServerContext> globalConnectionManager = new ConnectionManager<>("server");
		
	Collection<ServerConnectionStateListener> stateListeners = new ArrayList<>();
	
	ChannelFactory<SshServerContext> channelFactory = new DefaultServerChannelFactory();
	
	Map<String, GlobalRequestHandler<SshServerContext>> globalRequestHandlers = Collections
			.synchronizedMap(new HashMap<String, GlobalRequestHandler<SshServerContext>>());

	int maxDHGroupSize = 2048;

	private boolean forceServerPreferences = false;
	
	private static ComponentFactory<SshKeyExchange<SshServerContext>> verifiedKeyExchanges;
	
	static {
		globalForwardingManager.setForwardingFactory((h, p) -> RemoteForwardingChannelFactoryImpl.INSTANCE);
	}
	
	public SshServerContext(SshEngine engine) throws IOException, SshException {
		this(engine, SecurityLevel.STRONG);
	}
	
	public SshServerContext(SshEngine engine, ComponentManager componentManager) throws IOException, SshException {
		this(engine, componentManager, SecurityLevel.STRONG);
	}
	
	public SshServerContext(SshEngine engine, ComponentManager componentManager, SecurityLevel securityLevel) throws IOException, SshException {
		super(engine, componentManager, securityLevel);
		setAuthenicationMechanismFactory(new DefaultAuthenticationMechanismFactory<>());
		setPolicy(PublicKeyAuthenticationVerifier.class, new DefaultPublicKeyAuthenticationVerifier());
	}

	public SshServerContext(SshEngine engine, SecurityLevel securityLevel) throws IOException, SshException {
		this(engine, ComponentManager.getDefaultInstance(), securityLevel);
	}
	
	public ConnectionManager<SshServerContext> getConnectionManager() {
		return Objects.isNull(connectionManager) ? globalConnectionManager : connectionManager;
	}
	
	public void setConnectionManager(ConnectionManager<SshServerContext> connectionManager) {
		this.connectionManager = connectionManager;
	}

	public ProtocolEngine createEngine(ConnectRequestFuture connectFuture) throws IOException {
		return new TransportProtocolServer(this, connectFuture);
    }
	
	public void addStateListener(ServerConnectionStateListener stateListener) {
		this.stateListeners.add(stateListener);
	}

	public Collection<ServerConnectionStateListener> getStateListeners() {
		return stateListeners;
	}
	
	public void addGlobalRequestHandler(GlobalRequestHandler<SshServerContext> handler) {
		for (int i = 0; i < handler.supportedRequests().length; i++) {
			globalRequestHandlers.put(handler.supportedRequests()[i], handler);
		}
	}
	
	@Override
	public GlobalRequestHandler<SshServerContext> getGlobalRequestHandler(String name) {
		return (GlobalRequestHandler<SshServerContext>) globalRequestHandlers.get(name);
	}
	
	
	/**
	 * Get the currently preferred public key algorithm.
	 * 
	 * @return the preferred public key
	 */
	public String getPreferredPublicKey() {
		if (hostkeys.containsKey(prefPublicKey)) {
			return prefPublicKey;
		} else {

			if (hostkeys.entrySet().isEmpty())
				throw new RuntimeException("No host keys loaded!!");
			else {
				Map.Entry<String, SshKeyPair> e = hostkeys.entrySet()
						.iterator().next();
				return e.getKey();
			}
		}
	}

	/**
	 * Returns a comma delimited string containing installed public key types.
	 * 
	 * @return String
	 */
	public String getSupportedPublicKeys() {

		String list = "";

		if (hostkeys.keySet().contains(prefPublicKey)) {
			list += prefPublicKey;

		}
		String type;
		for (Iterator<String> it = hostkeys.keySet().iterator(); it.hasNext();) {
			type = it.next();
			if (!type.equals(prefPublicKey)) {
				list += (list.length() == 0 ? "" : ",") + type;
			}
		}
		return list;
	}
	
	/**
	 * <p>
	 * Set the preferred public key algorithm.
	 * </p>
	 * 
	 * <p>
	 * <em>This value must be one of the installed public key algorithm names.
	 * You will be able to obtain these from the public keys that you installed using ??????
	 * and an example of how to do this is provided in the
	 * {@link ConfigurationContext#addHostKey(SshKeyPair)} method description.</em>
	 * </p>
	 * 
	 * @param name
	 * @throws IOException
	 * @throws SshException
	 */
	public void setPreferredPublicKey(String name) throws IOException,
			SshException {
		if (publicKeys.contains(name)) {
			prefPublicKey = name;
			setPublicKeyPreferredPosition(name, 0);
		} else {
			throw new IOException(name + " is not supported");
		}
	}

	/**
	 * Get all the hosts keys.
	 * 
	 * @return SshPublicKey[]
	 */
	public SshKeyPair[] getHostKeys() {
		SshKeyPair[] keys = new SshKeyPair[hostkeys.size()];
		hostkeys.values().toArray(keys);
		return keys;
	}

	/**
	 * Get the host key for a given algorithm.
	 * 
	 * @param algorithm
	 * @return SshKeyPair
	 * @throws IOException
	 */
	public SshKeyPair getHostKey(String algorithm) throws IOException {
		if (!hostkeys.containsKey(algorithm)) {
			throw new IOException("The server does not have a " + algorithm
					+ " key configured");
		}
		return (SshKeyPair) hostkeys.get(algorithm);
	}
	
	/**
	 * <p>
	 * Add a host key to the configuration.
	 * </p>
	 * 
	 * <p>
	 * A host key provides a mechanism for a client to authenticate the server.
	 * If the client knows the public key of the server it can validate the
	 * signature that the server generated using its private key with the known
	 * public key of the server. In order for your server to operate you need to
	 * generate and install at least one host key.
	 * </p>
	 * 
	 * <p>
	 * It is now recommended to use the
	 * {@link SshDaemon#loadOrGenerateHostKey(File, String, int, ConfigurationContext)}
	 * method for generating and loading a host key.
	 * 
	 * @param keyPair
	 * @throws IOException
	 */
	public void addHostKey(SshKeyPair keyPair) throws IOException {
		if(keyPair instanceof SshCertificate) {
			
			SshKeyPair converted = new SshKeyPair();
			converted.setPrivateKey(keyPair.getPrivateKey());
			converted.setPublicKey(((SshCertificate)keyPair).getCertificate());
			
			if(hostkeys.containsKey(converted.getPublicKey().getAlgorithm())) {
				Log.warn("The server already has a " + keyPair.getPublicKey().getAlgorithm() + " certificate configured.");
			}
			
			hostkeys.put(converted.getPublicKey().getAlgorithm(), converted);
		} else {

			if(hostkeys.containsKey(keyPair.getPublicKey().getAlgorithm())) {
				Log.warn("The server already has a " + keyPair.getPublicKey().getAlgorithm() + " key configured.");
			}
			
			hostkeys.put(keyPair.getPublicKey().getAlgorithm(), keyPair);
			if(keyPair.getPublicKey() instanceof Ssh2RsaPublicKey && keyPair.getPublicKey().getAlgorithm().equals(SshContext.PUBLIC_KEY_SSHRSA)) {
				if(supportedPublicKeys().contains(SshContext.PUBLIC_KEY_RSA_SHA256) 
						&& !hostkeys.containsKey(SshContext.PUBLIC_KEY_RSA_SHA256)) {
					hostkeys.put(SshContext.PUBLIC_KEY_RSA_SHA256, SshKeyUtils.makeRSAWithSHA256Signature(keyPair));
				}
			}
			if(keyPair.getPublicKey() instanceof Ssh2RsaPublicKey && keyPair.getPublicKey().getAlgorithm().equals(SshContext.PUBLIC_KEY_SSHRSA)) {
				if(supportedPublicKeys().contains(SshContext.PUBLIC_KEY_RSA_SHA512)
						&& !hostkeys.containsKey(SshContext.PUBLIC_KEY_RSA_SHA512)) {
					hostkeys.put(SshContext.PUBLIC_KEY_RSA_SHA512, SshKeyUtils.makeRSAWithSHA512Signature(keyPair));
				}
			}
		}
	}
	
	/**
	 * Add a collection of host keys.
	 * @param keys
	 * @throws IOException
	 */
	public void addHostKeys(Collection<SshKeyPair> keys) throws IOException {
		for(SshKeyPair key : keys) {
			addHostKey(key);
		}
	}
	
	public void generateTemporaryHostKey(String algorithm, int bitlength) throws IOException, SshException {
		addHostKey(generateKey(algorithm, bitlength));
	}

	public ChannelFactory<SshServerContext> getChannelFactory() {
		return channelFactory;
	}

	public void setChannelFactory(ChannelFactory<SshServerContext> channelFactory) {
		this.channelFactory = channelFactory;
	}
	
	public ForwardingManager<SshServerContext> getForwardingManager() {
		return forwardingManager == null ? globalForwardingManager : forwardingManager;
	}
	
	public void setForwardingManager(ForwardingManager<SshServerContext> forwardingManager) {
		this.forwardingManager = forwardingManager;
	}

	/**
	 * Determine if the server has a host key configured.
	 * 
	 * @param algorithm
	 * @return boolean
	 */
	public boolean hasPublicKey(String algorithm) {
		return hostkeys.containsKey(algorithm);
	}

	/**
	 * Load a host key from file, if the file does not exist then generate the
	 * key.
	 * @param key
	 * @param type
	 * @param bitlength
	 * @return
	 * @throws IOException
	 * @throws InvalidPassphraseException
	 * @throws SshException
	 */
	public SshKeyPair loadOrGenerateHostKey(File key, String type, int bitlength)
			throws IOException, InvalidPassphraseException, SshException {
		return loadOrGenerateHostKey(key, type, bitlength,
				SshPublicKeyFileFactory.SECSH_FORMAT, "");
	}

	/**
	 * Load a host key from a file, if it does not exist, generate it.
	 * @param key
	 * @param type
	 * @param bitlength
	 * @param passPhrase
	 * @return
	 * @throws IOException
	 * @throws InvalidPassphraseException
	 * @throws SshException
	 */
	public SshKeyPair loadOrGenerateHostKey(File key, String type, int bitlength,
			String passPhrase) throws IOException, InvalidPassphraseException,
			SshException {
		return loadOrGenerateHostKey(key, type, bitlength,
				SshPublicKeyFileFactory.SECSH_FORMAT, passPhrase);
	}

	/**
	 * Load a host key from an InputStream.
	 * @param in
	 * @param type
	 * @param bitlength
	 * @throws IOException
	 * @throws InvalidPassphraseException
	 * @throws SshException
	 */
	public void loadHostKey(InputStream in)
			throws IOException, InvalidPassphraseException, SshException {
		loadHostKey(in, "");
	}

	/**
	 * Load a host key from file, if the file does not exist then generate the
	 * key.
	 * @param key
	 * @param type
	 * @param bitlength
	 * @param publicKeyFormat
	 * @param passPhrase
	 * @return
	 * @throws IOException
	 * @throws InvalidPassphraseException
	 * @throws SshException
	 */
	public SshKeyPair loadOrGenerateHostKey(File key, String type, int bitlength,
			int publicKeyFormat, String passPhrase)
			throws IOException, InvalidPassphraseException, SshException {

		SshKeyPair pair;
		if (!key.exists()) {
			pair = generateKeyFiles(key, type, bitlength, 
					publicKeyFormat);
		} else {
			pair = loadKey(key, passPhrase);
		}
		
		addHostKey(pair);
		return pair;
	}

	/**
	 * Load a host key from an InputStream.
	 * @param in
	 * @param type
	 * @param bitlength
	 * @param privateKeyFormat
	 * @param publicKeyFormat
	 * @param passPhrase
	 * @throws IOException
	 * @throws InvalidPassphraseException
	 * @throws SshException
	 */
	public void loadHostKey(InputStream in, String passPhrase)
			throws IOException, InvalidPassphraseException, SshException {

		addHostKey(loadKey(in, passPhrase));
	}

	/**
	 * Load a key pair from a File
	 * @param key
	 * @param passphrase
	 * @return
	 * @throws IOException
	 * @throws InvalidPassphraseException
	 */
	public SshKeyPair loadKey(File key, String passphrase) throws IOException,
			InvalidPassphraseException {
		return loadKey(new FileInputStream(key), passphrase);

	}

	/**
	 * Load a key pair from an InputStream.
	 * @param in
	 * @param passphrase
	 * @return
	 * @throws IOException
	 * @throws InvalidPassphraseException
	 */
	public SshKeyPair loadKey(InputStream in, String passphrase)
			throws IOException, InvalidPassphraseException {
		SshKeyPair pair = SshPrivateKeyFileFactory.parse(in).toKeyPair(
				passphrase);
		in.close();
		return pair;
	}

	/**
	 * Generate a public and private key pair, save them to keyFilename and
	 * keyFilename.pub, return the key pair
	 * 
	 * @param keyFilename
	 * @param type
	 * @param bitlength
	 * @param privateKeyFormat
	 * @param publicKeyFormat
	 * @return SshKeyPair generated.
	 * @throws IOException
	 * @throws SshException
	 */
	public static SshKeyPair generateKeyFiles(File keyFilename, String type,
			int bitlength, int publicKeyFormat)
			throws IOException, SshException {
		
		SshKeyPair pair = generateKey(type, bitlength);

		SshPrivateKeyFile prvfile = SshPrivateKeyFileFactory.create(pair, "");
		FileOutputStream fout = new FileOutputStream(keyFilename);
		fout.write(prvfile.getFormattedKey());
		fout.close();

		SshPublicKeyFile pubfile = SshPublicKeyFileFactory.create(
				pair.getPublicKey(), type + " host key", publicKeyFormat);
		fout = new FileOutputStream(keyFilename.getAbsolutePath() + ".pub");
		fout.write(pubfile.getFormattedKey());
		fout.close();

		return pair;
	}
	
	/**
	 * Generate a key pair.
	 * @param type
	 * @param bitLength
	 * @return
	 * @throws IOException
	 * @throws SshException
	 */
	public static SshKeyPair generateKey(String type, int bitLength) throws IOException, SshException {
		return SshKeyPairGenerator.generateKeyPair(type, bitLength);
	}

	/**
	 * This method loads an OpenSSH certificate file for use as a host key.
	 * @param keyFile
	 * @param passphrase
	 * @param certFile
	 * @throws IOException
	 * @throws InvalidPassphraseException
	 */
	public void loadSshCertificate(File keyFile, String passphrase,
			File certFile) throws IOException, InvalidPassphraseException {

		SshKeyPair pair = loadKey(keyFile, passphrase);
		pair.setPublicKey(SshPublicKeyFileFactory.parse(
				new FileInputStream(certFile)).toPublicKey());
		addHostKey(pair);
	}
	
	public void loadSshCertificate(SshCertificate cert) throws IOException, InvalidPassphraseException {
		addHostKey(cert);
	}

	/**
	 * Set the {@link AuthenticationMechanismFactory} for this context.
	 * @param authFactory
	 */
	public void setAuthenicationMechanismFactory(
			AuthenticationMechanismFactory<SshServerContext> authFactory) {
		setPolicy(AuthenticationMechanismFactory.class, authFactory);
	}
	
	/**
	 * Get the {@link AuthenticationMechanismFactory} for this context.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public AuthenticationMechanismFactory<SshServerContext> getAuthenticationMechanismFactory() {
		return getPolicy(AuthenticationMechanismFactory.class, new DefaultAuthenticationMechanismFactory<SshServerContext>());
	}

	/**
	 * If a problem occurs, or a connection is denied, for example if maximum connections threshold
	 * has been reached, this setting determines if the connection is allowed to proceed through
	 * key exchange so that the user is returned a suitable error. If connections are terminated
	 * before key exchange completes its not always possible to send the correct error.
	 * @return
	 */
	public boolean isEnsureGracefulDisconnect() {
		return ensureGracefulDisconnect;
	}
	
	/**
	 * If a problem occurs, or a connection is denied, for example if maximum connections threshold
	 * has been reached, this setting determines if the connection is allowed to proceed through
	 * key exchange so that the user is returned a suitable error. If connections are terminated
	 * before key exchange completes its not always possible to send the correct error.
	 * @param ensureGracefulDisconnect
	 */
	public void setEnsureGracefulDisconnect(boolean ensureGracefulDisconnect) {
		this.ensureGracefulDisconnect = ensureGracefulDisconnect;
	}

	/**
	 * Build the key exchanges available in this context. This method caches validated key exchanges to prevent
	 * the need to test on every new context instance.
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected synchronized void configureKeyExchanges() {
		
		if(Objects.nonNull(verifiedKeyExchanges)) {
			keyExchanges = (ComponentFactory<SshKeyExchange<? extends SshContext>>)verifiedKeyExchanges.clone();
			return;
		}
		
		if(Log.isInfoEnabled()) {
			Log.info("Initializing server key exchanges");
		}
		
		verifiedKeyExchanges = new ComponentFactory<SshKeyExchange<SshServerContext>>(componentManager);
		for(var kex : ServiceLoader.load(SshKeyExchangeServerFactory.class)) {
			if(testServerKeyExchangeAlgorithm(kex))
				verifiedKeyExchanges.add(kex);
		}
		
		keyExchanges = (ComponentFactory<SshKeyExchange<? extends SshContext>>)verifiedKeyExchanges.clone();
		
	}

	private boolean testServerKeyExchangeAlgorithm(SshKeyExchangeServerFactory<? extends SshKeyExchangeServer> cls) {

		var name  = cls.getKeys() [0];
		
		SshKeyExchange<? extends SshContext> c = null;
		try {

			c = cls.create();

			if (!JCEComponentManager.getDefaultInstance().supportedDigests().contains(c.getHashAlgorithm()))
				throw new Exception("Hash algorithm " + c.getHashAlgorithm() + " is not supported");

			c.test();
			
		} catch (Exception e) {
			if(Log.isDebugEnabled())
				Log.debug("   " + name + " (server) will not be supported: " + e.getMessage());
			return false;
		} catch (Throwable e) {
			// a null pointer exception will be caught at the end of the keyex
			// call when transport.sendmessage is called, at this point the
			// algorithm has not thrown an exception so we ignore this excpected
			// exception.
		}

		if(Log.isDebugEnabled())
			Log.debug("   " + name + " (server) will be supported using JCE Provider " + c.getProvider());

		return true;
	}
	
	/**
	 * Set the maximum group size supported in <code>diffie-hellman-group-exchange</code> key exchange methods.
	 */
	public void setMaxDHGroupExchangeSize(int maxDHGroupSize) {
		this.maxDHGroupSize  = maxDHGroupSize;
	}

	/**
	 * Get the maximum group size supported in <code>diffie-hellman-group-exchange</code> key exchange methods.
	 */
	public int getMaxDHGroupExchangeKeySize() {
		return maxDHGroupSize;
	}
	
	/**
	 * Is the server wanting to control key exchange {@link #setServerControllerKeyExchange()}
	 */
	public boolean isForceServerPreferences() {
		return forceServerPreferences;
	}
	
	/**
	 * When <code>true</code> the server will delay it's key exchange initialisation
	 * until it has received the client's initialisation packet. This allows the 
	 * server to control what algorithms are selected during key exchange by limiting
	 * its own set of algorithms to only those it wants to support. In effect, allowing
	 * the server to control the output of key exchange.
	 * @param serverControlledKeyExchange
	 */
	public void setForceServerPreferences(boolean serverControlledKeyExchange) {
		this.forceServerPreferences = serverControlledKeyExchange;
	}
}
