package com.sshtools.common.ssh.components.jce;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.ServiceLoader;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.ComponentFactory;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.Digest;
import com.sshtools.common.ssh.components.DigestFactory;
import com.sshtools.common.ssh.components.SshCipher;
import com.sshtools.common.ssh.components.SshCipherFactory;
import com.sshtools.common.ssh.components.SshDsaPrivateKey;
import com.sshtools.common.ssh.components.SshDsaPublicKey;
import com.sshtools.common.ssh.components.SshHmac;
import com.sshtools.common.ssh.components.SshHmacFactory;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.SshPublicKeyFactory;
import com.sshtools.common.ssh.components.SshRsaPrivateCrtKey;
import com.sshtools.common.ssh.components.SshRsaPrivateKey;
import com.sshtools.common.ssh.components.SshRsaPublicKey;
import com.sshtools.common.ssh.components.SshSecureRandomGenerator;

/**
 * A component manager for the Java runtime JCE provider. By default all
 * algorithms will be selected from the default provider i.e no provider is
 * specified in calls to JCE methods to create components. You can initialize a
 * default provider to be used on all calls with the following code:
 * 
 * <blockquote>
 * 
 * <pre>
 * JCEComponentManager.initializeDefaultProvider(new BouncyCastleProvider());
 * </pre>
 * 
 * </blockquote>
 * 
 * Alternatively you can also assign a specific provider for an  individual
 * algorithm, all algorithms used by the API are included as static constants in
 * this class.
 * 
 * <blockquote>
 * 
 * <pre>
 * JCEComponentManager.initializeProviderForAlgorithm(JCEComponentManager.JCE_DSA, new BouncyCastleProvider());
 * </pre>
 * 
 * </blockquote>
 * 
 * @author Lee David Painter
 */
public class JCEComponentManager extends ComponentManager implements JCEAlgorithms {

	
	SecureRND rnd;
	ClassLoader classLoader = JCEComponentManager.class.getClassLoader();
	
	public JCEComponentManager() {
		if("executable".equals(System.getProperty("org.graalvm.nativeimage.kind", ""))) {
			Log.info("Leaving provider configuration as running a native build.");
		}
		else {
			if (System.getProperty("maverick.enableBCProvider", "true").equalsIgnoreCase("false") || JCEProvider.isBCDisabled()) {
				if(Log.isInfoEnabled()) {
					Log.info("Automatic configuration of BouncyCastle is disabled");
				}
				JCEProvider.disableBouncyCastle();
				return;
			}
			
			try {
				JCEProvider.enableBouncyCastle(false);
			} catch(IllegalStateException ex) {
				Log.error("Bouncycastle JCE not found in classpath");
			}
		}
	}

	/**
	 * Initialize the default JCE provider used by the API.
	 * 
	 * @param provider
	 */
	public static void initializeDefaultProvider(Provider provider) {
		JCEProvider.initializeDefaultProvider(provider);
	}

	/**
	 * Initialize a provider for a specific algorithm.
	 * 
	 * @param jceAlgorithm
	 * @param provider
	 */
	public static void initializeProviderForAlgorithm(String jceAlgorithm, Provider provider) {
		JCEProvider.initializeProviderForAlgorithm(jceAlgorithm, provider);
	}

	/**
	 * Get the algorithm used for secure random number generation.
	 * 
	 * @return String
	 */
	public static String getSecureRandomAlgorithm() {
		return JCEProvider.getSecureRandomAlgorithm();
	}

	/**
	 * Set the algorithm used for secure random number generation.
	 * 
	 * @param secureRandomAlgorithm
	 */
	public static void setSecureRandomAlgorithm(String secureRandomAlgorithm) {
		JCEProvider.setSecureRandomAlgorithm(secureRandomAlgorithm);
	}

	/**
	 * Get the provider for a specific algorithm.
	 * 
	 * @param jceAlgorithm
	 * @return Provider
	 */
	public static Provider getProviderForAlgorithm(String jceAlgorithm) {
		return JCEProvider.getProviderForAlgorithm(jceAlgorithm);
	}

	/**
	 * Get the secure random implementation for the API.
	 * 
	 * @return SecureRandom
	 * @throws NoSuchAlgorithmException
	 */
	public static SecureRandom getSecureRandom() {
		return JCEProvider.getSecureRandom();
	}

	public SshDsaPrivateKey createDsaPrivateKey(BigInteger p, BigInteger q, BigInteger g, BigInteger x, BigInteger y)
			throws SshException {
		return new Ssh2DsaPrivateKey(p, q, g, x, y);
	}

	public SshDsaPublicKey createDsaPublicKey(BigInteger p, BigInteger q, BigInteger g, BigInteger y)
			throws SshException {
		try {
			return new Ssh2DsaPublicKey(p, q, g, y);
		} catch (Throwable e) {
			throw new SshException(e);
		}
	}

	public SshDsaPublicKey createDsaPublicKey() {
		throw new UnsupportedOperationException();
	}

	public SshRsaPrivateCrtKey createRsaPrivateCrtKey(BigInteger modulus, BigInteger publicExponent,
			BigInteger privateExponent, BigInteger primeP, BigInteger primeQ, BigInteger crtCoefficient)
			throws SshException {

		try {
			BigInteger primeExponentP = primeP.subtract(BigInteger.ONE);
			primeExponentP = privateExponent.mod(primeExponentP);

			BigInteger primeExponentQ = primeQ.subtract(BigInteger.ONE);
			primeExponentQ = privateExponent.mod(primeExponentQ);

			return new Ssh2RsaPrivateCrtKey(modulus, publicExponent, privateExponent, primeP, primeQ, primeExponentP,
					primeExponentQ, crtCoefficient);
		} catch (Throwable e) {
			throw new SshException(e);
		}
	}

	public SshRsaPrivateCrtKey createRsaPrivateCrtKey(BigInteger modulus, BigInteger publicExponent,
			BigInteger privateExponent, BigInteger primeP, BigInteger primeQ, BigInteger primeExponentP,
			BigInteger primeExponentQ, BigInteger crtCoefficient) throws SshException {
		try {
			return new Ssh2RsaPrivateCrtKey(modulus, publicExponent, privateExponent, primeP, primeQ, primeExponentP,
					primeExponentQ, crtCoefficient);
		} catch (Throwable e) {
			throw new SshException(e);
		}
	}

	public SshRsaPrivateKey createRsaPrivateKey(BigInteger modulus, BigInteger privateExponent) throws SshException {
		try {
			return new Ssh2RsaPrivateKey(modulus, privateExponent);
		} catch (Throwable t) {
			throw new SshException(t);
		}
	}

	public SshRsaPublicKey createRsaPublicKey(BigInteger modulus, BigInteger publicExponent)
			throws SshException {
		try {
			return new Ssh2RsaPublicKey(modulus, publicExponent);
		} catch (Throwable e) {
			throw new SshException(e);
		}
	}

	public SshRsaPublicKey createSsh2RsaPublicKey() throws SshException {
		return new Ssh2RsaPublicKey();
	}

	public SshKeyPair generateDsaKeyPair(int bits) throws SshException {

		try {

			KeyPairGenerator keyGen = JCEProvider.getProviderForAlgorithm(JCE_DSA) == null
					? KeyPairGenerator.getInstance(JCE_DSA)
					: KeyPairGenerator.getInstance(JCE_DSA, JCEProvider.getProviderForAlgorithm(JCE_DSA));
			keyGen.initialize(bits);
			KeyPair keypair = keyGen.genKeyPair();
			PrivateKey privateKey = keypair.getPrivate();
			PublicKey publicKey = keypair.getPublic();

			SshKeyPair pair = new SshKeyPair();

			pair.setPrivateKey(new Ssh2DsaPrivateKey((DSAPrivateKey) privateKey, (DSAPublicKey) publicKey));
			pair.setPublicKey(new Ssh2DsaPublicKey((DSAPublicKey) publicKey));
			return pair;
		} catch (java.security.NoSuchAlgorithmException e) {
			throw new SshException(e);
		}
	}

	public SshKeyPair generateRsaKeyPair(int bits, int version) throws SshException {
		try {

			KeyPairGenerator keyGen = JCEProvider.getProviderForAlgorithm(JCE_RSA) == null
					? KeyPairGenerator.getInstance(JCE_RSA)
					: KeyPairGenerator.getInstance(JCE_RSA, JCEProvider.getProviderForAlgorithm(JCE_RSA));
			keyGen.initialize(bits);
			KeyPair keypair = keyGen.genKeyPair();
			PrivateKey privateKey = keypair.getPrivate();
			PublicKey publicKey = keypair.getPublic();

			SshKeyPair pair = new SshKeyPair();
			if (!(privateKey instanceof RSAPrivateCrtKey)) {
				throw new SshException("RSA key generation requires RSAPrivateCrtKey as private key type.",
						SshException.JCE_ERROR);
			}
			pair.setPrivateKey(new Ssh2RsaPrivateCrtKey((RSAPrivateCrtKey) privateKey));
			pair.setPublicKey(new Ssh2RsaPublicKey((RSAPublicKey) publicKey));

			return pair;
		} catch (NoSuchAlgorithmException e) {
			throw new SshException(e);
		}
	}
	
	public SshKeyPair generateEd25519KeyPair() throws SshException {
		
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance(JCEAlgorithms.ED25519);
			KeyPair kp = keyGen.generateKeyPair();

			SshKeyPair pair = new SshKeyPair();
			pair.setPrivateKey(new SshEd25519PrivateKeyJCE(kp.getPrivate()));
			pair.setPublicKey(new SshEd25519PublicKeyJCE(kp.getPublic()));
			
			return pair;
		} catch (NoSuchAlgorithmException e) {
			throw new SshException(e);
		}
	}
	
	public SshKeyPair generateEd448KeyPair() throws SshException {
		
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance(JCEAlgorithms.ED448);
			KeyPair kp = keyGen.generateKeyPair();

			SshKeyPair pair = new SshKeyPair();
			pair.setPrivateKey(new SshEd448PrivateKeyJCE(kp.getPrivate()));
			pair.setPublicKey(new SshEd448PublicKeyJCE(kp.getPublic()));
			
			return pair;
		} catch (NoSuchAlgorithmException e) {
			throw new SshException(e);
		}
	}

	public SshKeyPair generateEcdsaKeyPair(int bits) throws SshException {

		String curve;

		switch (bits) {
		case 256:
			curve = "secp256r1";
			break;
		case 384:
			curve = "secp384r1";
			break;
		case 521:
			curve = "secp521r1";
			break;
		default:
			throw new SshException("Unsupported size " + bits + " for ECDSA key (256,384,521 supported)",
					SshException.BAD_API_USAGE);
		}

		try {
			ECGenParameterSpec ecGenSpec = new ECGenParameterSpec(curve);

			KeyPairGenerator g = KeyPairGenerator.getInstance(JCEProvider.getECDSAAlgorithmName());

			g.initialize(ecGenSpec, JCEProvider.getSecureRandom());

			KeyPair pair = g.generateKeyPair();

			SshKeyPair p = new SshKeyPair();
			p.setPrivateKey(new Ssh2EcdsaSha2NistPrivateKey((ECPrivateKey) pair.getPrivate(), curve));
			p.setPublicKey(new Ssh2EcdsaSha2NistPublicKey((ECPublicKey) pair.getPublic(), curve));

			return p;
		} catch (Exception e) {
			throw new SshException(e);
		}
	}

	public SshSecureRandomGenerator getRND() throws SshException {
		try {
			return rnd == null ? new SecureRND() : rnd;
		} catch (NoSuchAlgorithmException e) {
			throw new SshException(e);
		}
	}

	@SuppressWarnings("unchecked")
	protected void initializeDigestFactory(ComponentFactory<Digest> digests) {
		
		for(var digest : ServiceLoader.load(DigestFactory.class, 
				JCEComponentManager.getDefaultInstance().getClassLoader())) {
			if(testDigest(digest)) {
				digests.add(digest);
			}
		}
	}

	@SuppressWarnings("unchecked")
	protected void initializeHmacFactory(ComponentFactory<SshHmac> hmacs) {
		for(var hmac : ServiceLoader.load(SshHmacFactory.class, 
				JCEComponentManager.getDefaultInstance().getClassLoader())) {
			if(testHMac(hmac)) {
				hmacs.add(hmac);
			}
		}
	}

	@SuppressWarnings("unchecked")
	protected void initializePublicKeyFactory(ComponentFactory<SshPublicKey> publickeys) {
		
		for(var pubkey : ServiceLoader.load(SshPublicKeyFactory.class, 
				JCEComponentManager.getDefaultInstance().getClassLoader())) {
			if(testPublicKey(pubkey)) {
				publickeys.add(pubkey);
			}
		}
	}

	private boolean testPublicKey(SshPublicKeyFactory<SshPublicKey> cls) {
		var name  = cls.getKeys() [0];
		
		try {
			if(!isEnabled(cls, SshPublicKey.class, name))
				return false;
			SshPublicKey key = cls.create();
			String provider = key.test();
			if(Log.isInfoEnabled())
				Log.info("   " + name + " will be supported using JCE Provider " + provider);
			return true;
		} catch (Throwable e) {
			if(Log.isInfoEnabled())
				Log.info("   " + name + " will not be supported: " + e.getMessage());
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	protected void initializeSsh2CipherFactory(ComponentFactory<SshCipher> ciphers) {
		
		for(var cipher : ServiceLoader.load(SshCipherFactory.class, 
				JCEComponentManager.getDefaultInstance().getClassLoader())) {
			if(testJCECipher(cipher)) {
				ciphers.add(cipher);
			}
		}
	}

	private boolean testJCECipher(SshCipherFactory<SshCipher> cls) {
		
		var name  = cls.getKeys() [0];
		
		SshCipher c = null;
		try {
			if(!isEnabled(cls, SshCipher.class, name))
				return false;
			c = cls.create();
			byte[] tmp = new byte[1024];
			getSecureRandom().nextBytes(tmp);
			c.init(SshCipher.ENCRYPT_MODE, tmp, tmp);

			if (c instanceof AbstractJCECipher)
				if(Log.isInfoEnabled())
					Log.info("   " + name + " will be supported using JCE Provider "
							+ ((AbstractJCECipher) c).getProvider());

			return true;
		} catch (Throwable e) {
			if(Log.isInfoEnabled()) {
				Log.info("   " + name + " WILL NOT be supported: " + e.getMessage());
			}
			return false;
		}
	}

	private boolean testDigest(DigestFactory<Digest> cls) {
		
		var name  = cls.getKeys() [0];
		Digest c = null;
		try {
			if(!isEnabled(cls, Digest.class, name))
				return false;
			
			c = cls.create();

			if (c instanceof AbstractDigest)
				if(Log.isInfoEnabled())
					Log.info("   " + name + " will be supported using JCE Provider "
							+ ((AbstractDigest) c).getProvider());

			return true;
		} catch (Throwable e) {
			if(Log.isInfoEnabled()) {
				if(c!=null && ((AbstractDigest) c).getProvider()!=null) {
					Log.info("   " + name + " WILL NOT be supported from JCE Provider " + ((AbstractDigest) c).getProvider() + ": " + e.getMessage());
				} else {
					Log.info("   " + name + " WILL NOT be supported: " + e.getMessage());
				}
			}
			return false;
		}
	}

	private boolean testHMac(SshHmacFactory<SshHmac> cls) {
		
		var name  = cls.getKeys() [0];
		
		SshHmac c = null;
		try {
			if(!isEnabled(cls, SshHmac.class, name))
				return false;
			
			c = cls.create();
			byte[] tmp = new byte[1024];
			c.init(tmp);

			if (c instanceof AbstractHmac)
				if(Log.isInfoEnabled())
					Log.info(
							"   " + name + " will be supported using JCE Provider " + ((AbstractHmac) c).getProvider());

			return true;
		} catch (Throwable e) {
			if(Log.isInfoEnabled()) {
				Log.info("   " + name + " WILL NOT be supported: " + e.getMessage());
			}
			return false;
		}
	}

	public ClassLoader getClassLoader() {
		return classLoader;
	}

	public void setClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	public static ComponentManager getDefaultInstance() {
		return getInstance();
	}

}
