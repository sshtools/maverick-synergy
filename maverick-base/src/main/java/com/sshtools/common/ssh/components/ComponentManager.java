package com.sshtools.common.ssh.components;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.common.util.IOUtil;

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

	private static boolean PerContextAlgorithmPreferences = false;
	private static boolean enableNoneCipher = false;
	private static boolean enableNoneMac = false;
	
	Set<String> disabledAlgorithms = new HashSet<String>();
	Map<String,Map<String,Class<?>>> cachedExternalComponents = new HashMap<>();
	
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
		return enableNoneCipher;
	}

	public static void setEnableNoneCipher(boolean enableNoneCipher) {
		ComponentManager.enableNoneCipher = enableNoneCipher;
	}

	public static boolean isEnableNoneMac() {
		return enableNoneMac;
	}

	public static void setEnableNoneMac(boolean enableNoneCipher) {
		ComponentManager.enableNoneMac = enableNoneCipher;
	}

	
	public static void setPerContextAlgorithmPreferences(boolean enable) {
		PerContextAlgorithmPreferences = enable;
	}

	public static boolean getPerContextAlgorithmPreferences() {
		return PerContextAlgorithmPreferences;
	}

	protected static ComponentManager instance;

	ComponentFactory<SshCipher> ssh1ciphersSC;
	ComponentFactory<SshCipher> ssh2ciphersSC;
	ComponentFactory<SshCipher> ssh1ciphersCS;
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
	
	protected void init() throws SshException {

		if(Log.isInfoEnabled())
			Log.info("Initializing SSH2 server->client ciphers");

		ssh2ciphersSC = new ComponentFactory<SshCipher>(this);
		initializeSsh2CipherFactory(ssh2ciphersSC);

		if (enableNoneCipher) {
			ssh2ciphersSC.add("none", NoneCipher.class);
			if(Log.isInfoEnabled())
				Log.info("   none will be a supported cipher");
		}

		if(Log.isInfoEnabled())
			Log.info("Initializing SSH2 client->server ciphers");

		ssh2ciphersCS = new ComponentFactory<SshCipher>(this);
		initializeSsh2CipherFactory(ssh2ciphersCS);

		if (enableNoneCipher) {
			ssh2ciphersCS.add("none", NoneCipher.class);
			if(Log.isInfoEnabled())
				Log.info("   none will be a supported cipher");
		}

		if(Log.isInfoEnabled())
			Log.info("Initializing SSH2 server->client HMACs");

		hmacsSC = new ComponentFactory<SshHmac>(this);
		initializeHmacFactory(hmacsSC);

		if (enableNoneMac) {
			hmacsSC.add("none", NoneHmac.class);
			if(Log.isInfoEnabled())
				Log.info("   none will be a supported hmac");
		}
		
		if(Log.isInfoEnabled())
			Log.info("Initializing SSH2 client->server HMACs");

		hmacsCS = new ComponentFactory<SshHmac>(this);
		initializeHmacFactory(hmacsCS);

		if (enableNoneMac) {
			hmacsCS.add("none", NoneHmac.class);
			if(Log.isInfoEnabled())
				Log.info("   none will be a supported hmac");
		}
		
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
	 * The supported SSH1 ciphers.
	 * 
	 * @return AbstractComponentFactory
	 */
	@SuppressWarnings("unchecked")
	public ComponentFactory<SshCipher> supportedSsh1CiphersSC() {
		if (PerContextAlgorithmPreferences) {
			return (ComponentFactory<SshCipher>) ssh1ciphersSC.clone();
		}
		return ssh1ciphersSC;
	}

	/**
	 * The supported SSH1 ciphers.
	 * 
	 * @return AbstractComponentFactory
	 */
	@SuppressWarnings("unchecked")
	public ComponentFactory<SshCipher> supportedSsh1CiphersCS() {
		if (PerContextAlgorithmPreferences) {
			return (ComponentFactory<SshCipher>) ssh1ciphersCS.clone();
		}
		return ssh1ciphersCS;
	}

	/**
	 * The supported SSH2 ciphers.
	 * 
	 * @return AbstractComponentFactory
	 */
	@SuppressWarnings("unchecked")
	public ComponentFactory<SshCipher> supportedSsh2CiphersSC() {
		if (PerContextAlgorithmPreferences) {
			return (ComponentFactory<SshCipher>) ssh2ciphersSC.clone();
		}
		return ssh2ciphersSC;
	}

	/**
	 * The supported SSH2 ciphers.
	 * 
	 * @return AbstractComponentFactory
	 */
	@SuppressWarnings("unchecked")
	public ComponentFactory<SshCipher> supportedSsh2CiphersCS() {
		if (PerContextAlgorithmPreferences) {
			return (ComponentFactory<SshCipher>) ssh2ciphersCS.clone();
		}
		return ssh2ciphersCS;
	}

	/**
	 * The supported SSH2 Hmacs.
	 * 
	 * @return AbstractComponentFactory
	 */
	@SuppressWarnings("unchecked")
	public ComponentFactory<SshHmac> supportedHMacsSC() {
		if (PerContextAlgorithmPreferences) {
			return (ComponentFactory<SshHmac>) hmacsSC.clone();
		}
		return hmacsSC;
	}

	/**
	 * The supported SSH2 Hmacs.
	 * 
	 * @return AbstractComponentFactory
	 */
	@SuppressWarnings("unchecked")
	public ComponentFactory<SshHmac> supportedHMacsCS() {
		if (PerContextAlgorithmPreferences) {
			return (ComponentFactory<SshHmac>) hmacsCS.clone();
		}
		return hmacsCS;
	}
	
	/**
	 * The supported public keys
	 * 
	 * @return AbstractComponentFactory
	 */
	@SuppressWarnings("unchecked")
	public ComponentFactory<SshPublicKey> supportedPublicKeys() {
		if (PerContextAlgorithmPreferences) {
			return (ComponentFactory<SshPublicKey>) publickeys.clone();
		}
		return publickeys;
	}

	/**
	 * The supported digests
	 * 
	 * @return AbstractComponentFactory
	 */
	@SuppressWarnings("unchecked")
	public ComponentFactory<Digest> supportedDigests() {
		if (PerContextAlgorithmPreferences) {
			return (ComponentFactory<Digest>) digests.clone();
		}
		return digests;
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

	@SuppressWarnings("unchecked")
	public <T> void loadExternalComponents(String componentFile, ComponentFactory<T> componentFactory) {
	
		Map<String,Class<?>> cachedComponents = cachedExternalComponents.get(componentFile);
		
		if(Objects.isNull(cachedComponents)) {
			
			cachedComponents = new HashMap<>();
			
			try {
				Enumeration<URL> e = getClass().getClassLoader().getResources(componentFile);
				
				while(e.hasMoreElements()) {
					InputStream in = null;
					
					try {
						in = e.nextElement().openStream();
								
						if(Objects.isNull(in)) {
							Log.info("No further components to add");
							return;
						}
						
						Properties properties = new Properties();
						properties.load(in);
						
						for(Object alg : properties.keySet()) {
							String clz = properties.getProperty(alg.toString());
							try {
								Class<T> componenetClz = (Class<T>) Class.forName(clz);
								cachedComponents.put(alg.toString(), componenetClz);
							} catch (ClassNotFoundException ex) {
								Log.error("Cannot find class %s for algorithm %s", clz, alg);
							}
						}
						
						cachedExternalComponents.put(componentFile, cachedComponents);
					} catch(IOException ex) {
						Log.error("Error processing %s", ex, componentFile);
					} finally {
						IOUtil.closeStream(in);
					}
				}
				
			} catch(IOException ex) {
				Log.error("Error processing %s", ex, componentFile);
			}
		}
		
		for(Map.Entry<String,Class<?>> e : cachedComponents.entrySet()) {
			componentFactory.add(e.getKey(), (Class<? extends T>) e.getValue());
		}
	}
}
