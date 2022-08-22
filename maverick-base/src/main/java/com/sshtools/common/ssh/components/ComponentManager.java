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

package com.sshtools.common.ssh.components;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;

/**
 * <p>
 * An abstract class that manages the components used by the SSH API. All
 * algorithm implementations are obtained through a single provider. One
 * concrete implementation is provided, the
 * {@link com.sshtools.common.ssh.components.jce.JCEComponentManager} that uses the
 * Java runtime JCE provider(s) algorithm implementations.
 * </p>
 * 
 * @author Lee David Painter
 * 
 */
public abstract class ComponentManager {

	private static Map<Class<? extends Component>, Map<String, Boolean>> defaultEnabled = new HashMap<>();
	
	private Set<String> disabledAlgorithms = new HashSet<String>();
	
	protected ComponentManager() {
		disabledAlgorithms.add("ssh-dss");
	}
	
	public void disableAlgorithm(String algorithm) {
		disabledAlgorithms.add(algorithm);
	}
	
	public boolean isDisabled(String algorithm) {
		return disabledAlgorithms.contains(algorithm);
	}
	
	public void enableAlgorithm(String algorithm) {
		disabledAlgorithms.remove(algorithm);
	}
	
	public static boolean isEnableNoneCipher() {
		return isDefaultEnabled(SshCipher.class, "none").orElse(false);
	}

	public static void setEnableNoneCipher(boolean enableNoneCipher) {
		setDefaultEnabled(SshCipher.class, "none", enableNoneCipher);
	}

	public static boolean isEnableNoneMac() {
		return isDefaultEnabled(SshHmac.class, "none").orElse(false);
	}

	public static void setEnableNoneMac(boolean enableNoneCipher) {
		setDefaultEnabled(SshHmac.class, "none", enableNoneCipher);
	}
	
	public static <C extends Component> Optional<Boolean> isDefaultEnabled(Class<C> factoryType, String key) {
		var m = ComponentManager.defaultEnabled.get(factoryType);
		if(m == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(m.get(key));
	}

	public static <C extends Component> void setDefaultEnabled(Class<C> componentType, String key, boolean enabled) {
		var m = ComponentManager.defaultEnabled.get(componentType);
		if(m == null) {
			m = new HashMap<>();
			ComponentManager.defaultEnabled.put(componentType, m);
		}
		m.put(key, enabled);
	}

	protected static ComponentManager instance;

	ComponentFactory<SshCipher> ssh2ciphersSC;
	ComponentFactory<SshCipher> ssh2ciphersCS;
	ComponentFactory<SshHmac> hmacsCS;
	ComponentFactory<SshHmac> hmacsSC;
	ComponentFactory<SshPublicKey> publickeys;
	ComponentFactory<Digest> digests;
	
	static Object lock = new Object();

	/**
	 * Get the installed component manager. Don't want to initialize this at
	 * class load time, so use a singleton instead. Initialized on the first
	 * call to getInstance.
	 * 
	 * @return ComponentManager
	 */
	public static ComponentManager getInstance() {
		
		synchronized (lock) {
			if (instance != null) {
				return instance;
			}
			try {
				instance = new JCEComponentManager();
				instance.init();
				return instance;
			} catch (Throwable e) {
				throw new RuntimeException(
						"Unable to locate a cryptographic provider", e);
			}
		}
	}

	public static ComponentManager getDefaultInstance() {
		return getInstance();
	}
	
	public static void reset() {
		synchronized (lock) {
			instance = null;
			getInstance();
		}
	}
	
	public <C extends Component, F extends ComponentInstanceFactory<C>> Iterable<F> loadComponents(Class<C> componentType, Class<F> factoryClass) {
		return ServiceLoader.load(factoryClass).stream().map(f -> f.get()).filter(f-> isEnabled(f, componentType, f.getKeys()[0])).collect(Collectors.toList());
	}

	public <C extends Component> boolean isEnabled(ComponentInstanceFactory<C> cls, Class<C> type, String name) {
		if(System.getProperties().containsKey(String.format("disable.%s",  name))) {
			if(Log.isDebugEnabled()) {
				Log.debug("   {} WILL NOT be supported because it has been explicitly disabled by a system property", name);
			}
			return false;
		}
		var enabled = isDefaultEnabled(type, name);
		if(enabled.isEmpty() && !cls.isEnabledByDefault()) {
			if(Log.isDebugEnabled()) {
				Log.debug("   {} WILL NOT be supported because it has been disabled by default by the vendor. It may be re-enabled programatically.", name);
			}
			return false;
		}
		if(enabled.isPresent() && !enabled.get()) {
			if(Log.isDebugEnabled()) {
				Log.debug("   {} WILL NOT be supported because it has been disabled programatically.", name);
			}
			return false;
		}
		return true;
	}
	
	protected void init() throws SshException {

		if(Log.isInfoEnabled())
			Log.info("Initializing SSH2 server->client ciphers");

		ssh2ciphersSC = new ComponentFactory<>(this);
		initializeSsh2CipherFactory(ssh2ciphersSC);

		if(Log.isInfoEnabled())
			Log.info("Initializing SSH2 client->server ciphers");

		ssh2ciphersCS = new ComponentFactory<>(this);
		initializeSsh2CipherFactory(ssh2ciphersCS);

		if(Log.isInfoEnabled())
			Log.info("Initializing SSH2 server->client HMACs");

		hmacsSC = new ComponentFactory<SshHmac>(this);
		initializeHmacFactory(hmacsSC);

		if(Log.isInfoEnabled())
			Log.info("Initializing SSH2 client->server HMACs");

		hmacsCS = new ComponentFactory<SshHmac>(this);
		initializeHmacFactory(hmacsCS);

		if(Log.isInfoEnabled())
			Log.info("Initializing public keys");

		publickeys = new ComponentFactory<SshPublicKey>(this);
		initializePublicKeyFactory(publickeys);

		if(Log.isInfoEnabled())
			Log.info("Initializing digests");

		digests = new ComponentFactory<Digest>(this);
		initializeDigestFactory(digests);

		if(Log.isInfoEnabled())
			Log.info("Initializing Secure Random Number Generator");
		getRND().nextInt();
	}

	/**
	 * Initialize the SSH2 cipher factory. These ciphers are exclusively used by
	 * the SSH2 implementation.
	 * 
	 * @param ciphers
	 */
	protected abstract void initializeSsh2CipherFactory(ComponentFactory<SshCipher> ciphers);

	/**
	 * Initialize the SSH2 HMAC factory.
	 * 
	 * @param hmacs
	 */
	protected abstract void initializeHmacFactory(ComponentFactory<SshHmac> hmacs);

	/**
	 * Initialize the public key factory.
	 * 
	 * @param publickeys
	 */
	protected abstract void initializePublicKeyFactory(
			ComponentFactory<SshPublicKey> publickeys);

	/**
	 * Initialize the digest factory.
	 * 
	 * @param digests
	 */
	protected abstract void initializeDigestFactory(ComponentFactory<Digest> digests);

	/**
	 * Overide the installed component manager with an alternative
	 * implementation.
	 * 
	 * @param instance
	 */
	public static void setInstance(ComponentManager instance) {
		synchronized (lock) {
			ComponentManager.instance = instance;
		}
	}

	/**
	 * The supported SSH2 ciphers.
	 * 
	 * @return AbstractComponentFactory
	 */
	@SuppressWarnings("unchecked")
	public ComponentFactory<SshCipher> supportedSsh2CiphersSC() {
		return (ComponentFactory<SshCipher>) ssh2ciphersSC.clone();
	}

	/**
	 * The supported SSH2 ciphers.
	 * 
	 * @return AbstractComponentFactory
	 */
	@SuppressWarnings("unchecked")
	public ComponentFactory<SshCipher> supportedSsh2CiphersCS() {
		return (ComponentFactory<SshCipher>) ssh2ciphersCS.clone();
	}

	/**
	 * The supported SSH2 Hmacs.
	 * 
	 * @return AbstractComponentFactory
	 */
	@SuppressWarnings("unchecked")
	public ComponentFactory<SshHmac> supportedHMacsSC() {
		return (ComponentFactory<SshHmac>) hmacsSC.clone();
	}

	/**
	 * The supported SSH2 Hmacs.
	 * 
	 * @return AbstractComponentFactory
	 */
	@SuppressWarnings("unchecked")
	public ComponentFactory<SshHmac> supportedHMacsCS() {
		return (ComponentFactory<SshHmac>) hmacsCS.clone();
	}
	
	/**
	 * The supported public keys
	 * 
	 * @return AbstractComponentFactory
	 */
	@SuppressWarnings("unchecked")
	public ComponentFactory<SshPublicKey> supportedPublicKeys() {
		return (ComponentFactory<SshPublicKey>) publickeys.clone();
	}

	/**
	 * The supported digests
	 * 
	 * @return AbstractComponentFactory
	 */
	@SuppressWarnings("unchecked")
	public ComponentFactory<Digest> supportedDigests() {
		return (ComponentFactory<Digest>) digests.clone();
	}

	/**
	 * Generate an RSA public/private pair.
	 * 
	 * @param bits
	 * @param version
	 * @return SshKeyPair
	 * @throws SshException
	 */
	public abstract SshKeyPair generateRsaKeyPair(int bits, int version)
			throws SshException;

	/**
	 * Generate a new ECDSA key pair.
	 * 
	 * @param bits
	 * @return
	 * @throws SshException
	 */
	public abstract SshKeyPair generateEcdsaKeyPair(int bits)
			throws SshException;

	
	
	public abstract SshKeyPair generateEd25519KeyPair() throws SshException;
	
	/**
	 * Create an instance of an RSA public key.
	 * 
	 * @param modulus
	 * @param publicExponent
	 * @param version
	 * @return SshRsaPublicKey
	 * @throws SshException
	 */
	public abstract SshRsaPublicKey createRsaPublicKey(BigInteger modulus,
			BigInteger publicExponent) throws SshException;

	/**
	 * Create an instance of an SSH2 RSA public key.
	 * 
	 * @return SshRsaPublicKey
	 * @throws SshException
	 */
	public abstract SshRsaPublicKey createSsh2RsaPublicKey()
			throws SshException;

	/**
	 * Create an instance of an RSA private key.
	 * 
	 * @param modulus
	 * @param privateExponent
	 * @return SshRsaPrivateKey
	 * @throws SshException
	 */
	public abstract SshRsaPrivateKey createRsaPrivateKey(BigInteger modulus,
			BigInteger privateExponent) throws SshException;

	/**
	 * Create an instance of an RSA co-effecient private key.
	 * 
	 * @param modulus
	 * @param publicExponent
	 * @param privateExponent
	 * @param primeP
	 * @param primeQ
	 * @param crtCoefficient
	 * @return SshRsaPrivateCrtKey
	 * @throws SshException
	 */
	public abstract SshRsaPrivateCrtKey createRsaPrivateCrtKey(
			BigInteger modulus, BigInteger publicExponent,
			BigInteger privateExponent, BigInteger primeP, BigInteger primeQ,
			BigInteger crtCoefficient) throws SshException;

	/**
	 * Create an instance of an RSA co-efficent private key.
	 * 
	 * @param modulus
	 * @param publicExponent
	 * @param privateExponent
	 * @param primeP
	 * @param primeQ
	 * @param primeExponentP
	 * @param primeExponentQ
	 * @param crtCoefficient
	 * @return SshRsaPrivateCrtKey
	 * @throws SshException
	 */
	public abstract SshRsaPrivateCrtKey createRsaPrivateCrtKey(
			BigInteger modulus, BigInteger publicExponent,
			BigInteger privateExponent, BigInteger primeP, BigInteger primeQ,
			BigInteger primeExponentP, BigInteger primeExponentQ,
			BigInteger crtCoefficient) throws SshException;

	/**
	 * Generate a new DSA public/private key pair.
	 * 
	 * @param bits
	 * @return SshKeyPair
	 * @throws SshException
	 */
	public abstract SshKeyPair generateDsaKeyPair(int bits) throws SshException;

	/**
	 * Create an instance of a DSA public key.
	 * 
	 * @param p
	 * @param q
	 * @param g
	 * @param y
	 * @return SshDsaPublicKey
	 * @throws SshException
	 */
	public abstract SshDsaPublicKey createDsaPublicKey(BigInteger p,
			BigInteger q, BigInteger g, BigInteger y) throws SshException;

	/**
	 * Create an uninitialized instance of a DSA public key
	 * 
	 * @return SshDsaPublicKey
	 */
	public abstract SshDsaPublicKey createDsaPublicKey();

	/**
	 * Create an instance of a DSA private key.
	 * 
	 * @param p
	 * @param q
	 * @param g
	 * @param x
	 * @param y
	 * @return SshDsaPrivateKey
	 * @throws SshException
	 */
	public abstract SshDsaPrivateKey createDsaPrivateKey(BigInteger p,
			BigInteger q, BigInteger g, BigInteger x, BigInteger y)
			throws SshException;

	/**
	 * Get the secure random number generator.
	 * 
	 * @return SshSecureRandomGenerator
	 * @throws SshException
	 */
	public abstract SshSecureRandomGenerator getRND() throws SshException;

	public Digest getDigest(String name) throws SshException {
		return digests.getInstance(name);
	}

	public void setMinimumSecurityLevel(SecurityLevel securityLevel) throws SshException {

		if(Log.isInfoEnabled()) {
			Log.info("Configuring {} Security", securityLevel.name());
		}
		
		setMinimumSecurityLevel(securityLevel, ssh2ciphersCS, "Client->Server Ciphers");
		setMinimumSecurityLevel(securityLevel, ssh2ciphersSC, "Server->Client Ciphers");
		setMinimumSecurityLevel(securityLevel, hmacsCS, "Client->Server Macs");
		setMinimumSecurityLevel(securityLevel, hmacsSC, "Server->Client Macs");
		setMinimumSecurityLevel(securityLevel, publickeys, "Public Keys");
//		if (clientKeyexchanges.hasComponents()) {
//			setMinimumSecurityLevel(securityLevel, clientKeyexchanges, "Client->Server KEX");
//		}
//		if (serverKeyexchanges.hasComponents()) {
//			setMinimumSecurityLevel(securityLevel, serverKeyexchanges, "Server->Client KEX");
//		}
		
	}

	private void setMinimumSecurityLevel(SecurityLevel securityLevel, ComponentFactory<?> componentFactory, String name) throws SshException {
		
		if(Log.isInfoEnabled()) {
			Log.info("Configuring {}", name);
		}
		componentFactory.configureSecurityLevel(securityLevel);
		
		if(Log.isInfoEnabled()) {
			Log.info(componentFactory.list(""));
		}
	}
}
