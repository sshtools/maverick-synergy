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
import java.util.concurrent.ConcurrentHashMap;

import com.sshtools.common.auth.AuthenticationMechanismFactory;
import com.sshtools.common.auth.DefaultAuthenticationMechanismFactory;
import com.sshtools.common.command.ExecutableCommand;
import com.sshtools.common.logger.Log;
import com.sshtools.common.nio.ConnectRequestFuture;
import com.sshtools.common.nio.ProtocolEngine;
import com.sshtools.common.nio.SshEngine;
import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.publickey.SshKeyPairGenerator;
import com.sshtools.common.publickey.SshPrivateKeyFile;
import com.sshtools.common.publickey.SshPrivateKeyFileFactory;
import com.sshtools.common.publickey.SshPublicKeyFile;
import com.sshtools.common.publickey.SshPublicKeyFileFactory;
import com.sshtools.common.ssh.ChannelFactory;
import com.sshtools.common.ssh.ConnectionManager;
import com.sshtools.common.ssh.ConnectionStateListener;
import com.sshtools.common.ssh.ForwardingManager;
import com.sshtools.common.ssh.GlobalRequestHandler;
import com.sshtools.common.ssh.SshContext;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.SshKeyExchange;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.server.components.jce.DiffieHellmanEcdhNistp256;
import com.sshtools.server.components.jce.DiffieHellmanEcdhNistp384;
import com.sshtools.server.components.jce.DiffieHellmanEcdhNistp521;
import com.sshtools.server.components.jce.DiffieHellmanGroup14Sha1JCE;
import com.sshtools.server.components.jce.DiffieHellmanGroup14Sha256JCE;
import com.sshtools.server.components.jce.DiffieHellmanGroup15Sha512JCE;
import com.sshtools.server.components.jce.DiffieHellmanGroup16Sha512JCE;
import com.sshtools.server.components.jce.DiffieHellmanGroup17Sha512JCE;
import com.sshtools.server.components.jce.DiffieHellmanGroup18Sha512JCE;
import com.sshtools.server.components.jce.DiffieHellmanGroup1Sha1JCE;
import com.sshtools.server.components.jce.DiffieHellmanGroupExchangeSha1JCE;
import com.sshtools.server.components.jce.DiffieHellmanGroupExchangeSha256JCE;
import com.sshtools.server.components.jce.Rsa1024SHA1KeyExchange;
import com.sshtools.server.components.jce.Rsa2048SHA2KeyExchange;

public class SshServerContext extends SshContext {

	
	Map<String, SshKeyPair> hostkeys = new ConcurrentHashMap<String, SshKeyPair>(8, 0.9f, 1);

	Map<String, Class<? extends ExecutableCommand>> commands 
		= new ConcurrentHashMap<String, Class<? extends ExecutableCommand>>(8, 0.9f, 1);
	
	int maximumConnections = -1;
	boolean allowKeyExchangeForDeniedConnection = false;
	String tooManyConnectionsText = "Too many connections";

	ForwardingManager<SshServerContext> forwardingManager;
	ConnectionManager<SshServerContext> connectionManager;
	static ForwardingManager<SshServerContext> globalForwardingManager = new ForwardingManager<>();
	static ConnectionManager<SshServerContext> globalConnectionManager = new ConnectionManager<>("server");
		
	Collection<ConnectionStateListener<SshServerContext>> stateListeners = new ArrayList<ConnectionStateListener<SshServerContext>>();
	
	ChannelFactory<SshServerContext> channelFactory = new DefaultServerChannelFactory();
	
	Map<String, GlobalRequestHandler<SshServerContext>> globalRequestHandlers = Collections
			.synchronizedMap(new HashMap<String, GlobalRequestHandler<SshServerContext>>());

	int maxDHGroupSize = 2048;
	
	
	public SshServerContext(SshEngine engine, ComponentManager componentManager) throws IOException {
		super(engine, componentManager);
		setAuthenicationMechanismFactory(new DefaultAuthenticationMechanismFactory<>());
	}

	public SshServerContext(SshEngine engine) throws IOException {
		this(engine, ComponentManager.getDefaultInstance());
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
	
	public void addStateListener(ConnectionStateListener<SshServerContext> stateListener) {
		this.stateListeners.add(stateListener);
	}

	public Collection<ConnectionStateListener<SshServerContext>> getStateListeners() {
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
	 * Set the maximum number of connections allowed at any one time.
	 * 
	 * @param maximumConnections
	 *            int
	 */
	public void setMaximumConnections(int maximumConnections) {
		this.maximumConnections = maximumConnections;
	}

	/**
	 * Get the maximum number of connections allowed at any one time.
	 * 
	 * @return int
	 */
	public int getMaximumConnections() {
		return maximumConnections;
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
		if (hostkeys.containsKey(keyPair.getPublicKey().getAlgorithm())) {
			throw new IOException("The server already has a "
					+ keyPair.getPublicKey().getAlgorithm() + " key configured");
		}
		hostkeys.put(keyPair.getPublicKey().getAlgorithm(), keyPair);
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
	 * Add an {@link com.maverick.sshd.ExecutableCommand} to the configuration.
	 * If a request to execute a command with the name <em>name</em> is received
	 * an instance of the class is created to handle the command execution.
	 * 
	 * @param name
	 *            String
	 * @param cls
	 *            Class
	 */
	public void addCommand(String name, Class<? extends ExecutableCommand> cls) {
		commands.put(name.toLowerCase(), cls);
	}

	/**
	 * Determine whether a command is configured.
	 * 
	 * @param name
	 *            String
	 * @return boolean
	 */
	public boolean containsCommand(String name) {
		return commands.containsKey(name.toLowerCase());
	}

	/**
	 * Get the Class implementation for a given command.
	 * 
	 * @param name
	 *            String
	 * @return Class
	 */
	public Class<? extends ExecutableCommand> getCommand(String name) {
		return commands.get(name.toLowerCase());
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
	 * 
	 * @param key
	 *            the key file
	 * @param type
	 *            the type of key; acceptable values are
	 *            SshKeyPairGenerator.SSH2_RSA or SshKeyPairGenerator.SSH2_DSA
	 * @param bitlength
	 *            the bit length of the key
	 * @throws IOException
	 * @throws SshException
	 */
	public SshKeyPair loadOrGenerateHostKey(File key, String type, int bitlength)
			throws IOException, InvalidPassphraseException, SshException {
		return loadOrGenerateHostKey(key, type, bitlength,
				SshPublicKeyFileFactory.SECSH_FORMAT, "");
	}

	public SshKeyPair loadOrGenerateHostKey(File key, String type, int bitlength,
			String passPhrase) throws IOException, InvalidPassphraseException,
			SshException {
		return loadOrGenerateHostKey(key, type, bitlength,
				SshPublicKeyFileFactory.SECSH_FORMAT, passPhrase);
	}

	public void loadHostKey(InputStream in, String type, int bitlength)
			throws IOException, InvalidPassphraseException, SshException {
		loadHostKey(in, type, bitlength,
				SshPrivateKeyFileFactory.OPENSSH_FORMAT,
				SshPublicKeyFileFactory.SECSH_FORMAT, "");
	}

	public void loadHostKey(InputStream in, String type, int bitlength,
			String passPhrase) throws IOException, InvalidPassphraseException,
			SshException {
		loadHostKey(in, type, bitlength,
				SshPrivateKeyFileFactory.OPENSSH_FORMAT,
				SshPublicKeyFileFactory.SECSH_FORMAT, passPhrase);
	}

	/**
	 * Load a host key from file, if the file does not exist then generate the
	 * key.
	 * 
	 * @param key
	 *            the key file
	 * @param type
	 *            the type of key; acceptable values are
	 *            SshKeyPairGenerator.SSH2_RSA or SshKeyPairGenerator.SSH2_DSA
	 * @param bitlength
	 *            the bit length of the key
	 * @param privateKeyFormat
	 *            the format of the private key, {@see
	 *            com.sshtools.publickey.SshPrivateKeyFileFactory}
	 * @param publicKeyFormat
	 *            the format of the public key, {see
	 *            com.sshtools.publickey.SshPublicKeyFileFactory}
	 * @param passPhrase
	 *            the passPhrase of an existing host key
	 * @throws IOException
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

	public void loadHostKey(InputStream in, String type, int bitlength,
			int privateKeyFormat, int publicKeyFormat, String passPhrase)
			throws IOException, InvalidPassphraseException, SshException {

		addHostKey(loadKey(in, passPhrase));
	}

	public SshKeyPair loadKey(File key, String passphrase) throws IOException,
			InvalidPassphraseException {
		return loadKey(new FileInputStream(key), passphrase);

	}

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
	 * Generate a key pair
	 * @param type
	 * @param bitLength
	 * @return
	 * @throws IOException
	 * @throws SshException
	 */
	public static SshKeyPair generateKey(String type, int bitLength) throws IOException, SshException {
		return SshKeyPairGenerator.generateKeyPair(type, bitLength);
	}
	
	public void setAllowDeniedKEX(boolean allowKeyExchangeForDeniedConnection) {
		this.allowKeyExchangeForDeniedConnection = allowKeyExchangeForDeniedConnection;
	}

	public boolean getAllowDeniedKEX() {
		return allowKeyExchangeForDeniedConnection;
	}

	public String getTooManyConnectionsText() {
		return tooManyConnectionsText;
	}

	public void setTooManyConnectionsText(String tooManyConnectionsText) {
		this.tooManyConnectionsText = tooManyConnectionsText;
	}

	public void loadSshCertificate(File keyFile, String passphrase,
			File certFile) throws IOException, InvalidPassphraseException {

		SshKeyPair pair = loadKey(keyFile, passphrase);
		pair.setPublicKey(SshPublicKeyFileFactory.parse(
				new FileInputStream(certFile)).toPublicKey());
		addHostKey(pair);
	}


	public void setAuthenicationMechanismFactory(
			AuthenticationMechanismFactory<SshServerContext> authFactory) {
		setPolicy(AuthenticationMechanismFactory.class, authFactory);
	}

	public boolean isEnsureGracefulDisconnect() {
		return allowKeyExchangeForDeniedConnection;
	}

	@Override
	protected void configureKeyExchanges() {
		
		JCEComponentManager.getDefaultInstance().loadExternalComponents("/kex-server.properties", keyExchanges);
		
		if(testServerKeyExchangeAlgorithm(
				DiffieHellmanGroupExchangeSha256JCE.DIFFIE_HELLMAN_GROUP_EXCHANGE_SHA256, DiffieHellmanGroupExchangeSha256JCE.class)) {
			keyExchanges.add(DiffieHellmanGroupExchangeSha256JCE.DIFFIE_HELLMAN_GROUP_EXCHANGE_SHA256, DiffieHellmanGroupExchangeSha256JCE.class);
		}

		if(testServerKeyExchangeAlgorithm(
				DiffieHellmanGroup14Sha256JCE.DIFFIE_HELLMAN_GROUP14_SHA256, DiffieHellmanGroup14Sha256JCE.class)) {
			keyExchanges.add(DiffieHellmanGroup14Sha256JCE.DIFFIE_HELLMAN_GROUP14_SHA256, DiffieHellmanGroup14Sha256JCE.class);
		}
		if(testServerKeyExchangeAlgorithm(
				DiffieHellmanGroup15Sha512JCE.DIFFIE_HELLMAN_GROUP15_SHA512, DiffieHellmanGroup15Sha512JCE.class)) {
			keyExchanges.add(DiffieHellmanGroup15Sha512JCE.DIFFIE_HELLMAN_GROUP15_SHA512, DiffieHellmanGroup15Sha512JCE.class);
		}
		if(testServerKeyExchangeAlgorithm(
				DiffieHellmanGroup16Sha512JCE.DIFFIE_HELLMAN_GROUP16_SHA512, DiffieHellmanGroup16Sha512JCE.class)) {
			keyExchanges.add(DiffieHellmanGroup16Sha512JCE.DIFFIE_HELLMAN_GROUP16_SHA512, DiffieHellmanGroup16Sha512JCE.class);
		}
		if(testServerKeyExchangeAlgorithm(
				DiffieHellmanGroup17Sha512JCE.DIFFIE_HELLMAN_GROUP17_SHA512, DiffieHellmanGroup17Sha512JCE.class)) {
			keyExchanges.add(DiffieHellmanGroup17Sha512JCE.DIFFIE_HELLMAN_GROUP17_SHA512, DiffieHellmanGroup17Sha512JCE.class);
		}
		if(testServerKeyExchangeAlgorithm(
				DiffieHellmanGroup18Sha512JCE.DIFFIE_HELLMAN_GROUP18_SHA512, DiffieHellmanGroup18Sha512JCE.class)) {
			keyExchanges.add(DiffieHellmanGroup18Sha512JCE.DIFFIE_HELLMAN_GROUP18_SHA512, DiffieHellmanGroup18Sha512JCE.class);
		}

		if(testServerKeyExchangeAlgorithm(
				DiffieHellmanGroup14Sha1JCE.DIFFIE_HELLMAN_GROUP14_SHA1, DiffieHellmanGroup14Sha1JCE.class)) {
			keyExchanges.add(DiffieHellmanGroup14Sha1JCE.DIFFIE_HELLMAN_GROUP14_SHA1, DiffieHellmanGroup14Sha1JCE.class);
		}

		if(testServerKeyExchangeAlgorithm(
				DiffieHellmanEcdhNistp521.DIFFIE_HELLMAN_ECDH_NISTP_521, DiffieHellmanEcdhNistp521.class)) {
			keyExchanges.add(DiffieHellmanEcdhNistp521.DIFFIE_HELLMAN_ECDH_NISTP_521, DiffieHellmanEcdhNistp521.class);
		}
		
		if(testServerKeyExchangeAlgorithm(
				DiffieHellmanEcdhNistp384.DIFFIE_HELLMAN_ECDH_NISTP_384, DiffieHellmanEcdhNistp384.class)) {
			keyExchanges.add(DiffieHellmanEcdhNistp384.DIFFIE_HELLMAN_ECDH_NISTP_384, DiffieHellmanEcdhNistp384.class);
		}
		
		if(testServerKeyExchangeAlgorithm(
				DiffieHellmanEcdhNistp256.DIFFIE_HELLMAN_ECDH_NISTP_256, DiffieHellmanEcdhNistp256.class)) {
			keyExchanges.add(DiffieHellmanEcdhNistp256.DIFFIE_HELLMAN_ECDH_NISTP_256, DiffieHellmanEcdhNistp256.class);
		}
		
		if(testServerKeyExchangeAlgorithm(
				Rsa2048SHA2KeyExchange.RSA_2048_SHA2, Rsa2048SHA2KeyExchange.class)) {
			keyExchanges.add(Rsa2048SHA2KeyExchange.RSA_2048_SHA2, Rsa2048SHA2KeyExchange.class);
		}
		
		if(testServerKeyExchangeAlgorithm(
				Rsa1024SHA1KeyExchange.RSA_1024_SHA1, Rsa1024SHA1KeyExchange.class)) {
			keyExchanges.add(Rsa1024SHA1KeyExchange.RSA_1024_SHA1, Rsa1024SHA1KeyExchange.class);
		}
		
		if(testServerKeyExchangeAlgorithm(
				DiffieHellmanGroupExchangeSha1JCE.DIFFIE_HELLMAN_GROUP_EXCHANGE_SHA1,  DiffieHellmanGroupExchangeSha1JCE.class)) {
			keyExchanges.add(DiffieHellmanGroupExchangeSha1JCE.DIFFIE_HELLMAN_GROUP_EXCHANGE_SHA1,  DiffieHellmanGroupExchangeSha1JCE.class);
		}
		
		if(testServerKeyExchangeAlgorithm(
				DiffieHellmanGroup1Sha1JCE.DIFFIE_HELLMAN_GROUP1_SHA1, DiffieHellmanGroup1Sha1JCE.class)) {
			keyExchanges.add(DiffieHellmanGroup1Sha1JCE.DIFFIE_HELLMAN_GROUP1_SHA1, DiffieHellmanGroup1Sha1JCE.class);
		}
		
	}
	
	public boolean testServerKeyExchangeAlgorithm(String name, Class<? extends SshKeyExchange<? extends SshContext>> cls) {

		SshKeyExchange<? extends SshContext> c = null;
		try {

			c = cls.newInstance();

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
	
	public void setMaxDHGroupExchangeSize(int maxDHGroupSize) {
		this.maxDHGroupSize  = maxDHGroupSize;
	}

	public int getMaxDHGroupExchangeKeySize() {
		return maxDHGroupSize;
	}

}
