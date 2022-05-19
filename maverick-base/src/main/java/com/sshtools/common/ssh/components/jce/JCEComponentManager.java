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

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.ComponentFactory;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.Digest;
import com.sshtools.common.ssh.components.SshCipher;
import com.sshtools.common.ssh.components.SshDsaPrivateKey;
import com.sshtools.common.ssh.components.SshDsaPublicKey;
import com.sshtools.common.ssh.components.SshHmac;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;
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

	public JCEComponentManager() {
		
		if (System.getProperty("maverick.enableBCProvider", "true").equalsIgnoreCase("false") || JCEProvider.isBCDisabled()) {
			if(Log.isDebugEnabled()) {
				Log.debug("Automatic configuration of BouncyCastle is disabled");
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

	protected void initializeDigestFactory(ComponentFactory<Digest> digests) {

		if (testDigest(JCEAlgorithms.JCE_MD5, MD5Digest.class))
			digests.add(JCEAlgorithms.JCE_MD5, MD5Digest.class);

		if (testDigest(JCEAlgorithms.JCE_SHA1, SHA1Digest.class))
			digests.add(JCEAlgorithms.JCE_SHA1, SHA1Digest.class);

		if (testDigest("SHA1", SHA1Digest.class))
			digests.add("SHA1", SHA1Digest.class);

		if (testDigest("SHA-256", SHA256Digest.class)) {
			digests.add("SHA-256", SHA256Digest.class);
			digests.add("SHA256", SHA256Digest.class);
		}
		if (testDigest("SHA-384", SHA384Digest.class)) {
			digests.add("SHA-384", SHA384Digest.class);
			digests.add("SHA384", SHA384Digest.class);
		}

		if (testDigest("SHA-512", SHA512Digest.class)) {
			digests.add("SHA-512", SHA512Digest.class);
			digests.add("SHA512", SHA512Digest.class);
		}
		
		loadExternalComponents("digest.properties", digests);
	}

	protected void initializeHmacFactory(ComponentFactory<SshHmac> hmacs) {

		if (testHMac("hmac-sha256", HmacSha256.class)) {
			hmacs.add("hmac-sha256", HmacSha256.class);
			hmacs.add("hmac-sha2-256", HmacSha256.class);
			hmacs.add("hmac-sha256@ssh.com", HmacSha256_at_ssh_dot_com.class);
			hmacs.add("hmac-sha2-256-etm@openssh.com", HmacSha256ETM.class);
		}

		if (testHMac("hmac-sha2-256-96", HmacSha256_96.class)) {
			hmacs.add("hmac-sha2-256-96", HmacSha256_96.class);
		}

		if (testHMac("hmac-sha512", HmacSha512.class)) {
			hmacs.add("hmac-sha512", HmacSha512.class);
			hmacs.add("hmac-sha2-512", HmacSha512.class);
			hmacs.add("hmac-sha512@ssh.com", HmacSha512.class);
			hmacs.add("hmac-sha2-512-etm@openssh.com", HmacSha512ETM.class);
		}

		if (testHMac("hmac-sha2-512-96", HmacSha512_96.class)) {
			hmacs.add("hmac-sha2-512-96", HmacSha512_96.class);
		}
		
		if (testHMac("hmac-sha1", HmacSha1.class)) {
			hmacs.add("hmac-sha1", HmacSha1.class);
			hmacs.add("hmac-sha1-etm@openssh.com", HmacSha1ETM.class);
		}
	
		if (testHMac("hmac-sha1-96", HmacSha196.class)) {
			hmacs.add("hmac-sha1-96", HmacSha196.class);
		}
		
		loadExternalComponents("hmac.properties", hmacs);

	}

	protected void initializePublicKeyFactory(ComponentFactory<SshPublicKey> publickeys) {

		testPublicKey("ssh-ed448", SshEd448PublicKeyJCE.class, publickeys);
		testPublicKey("ssh-ed25519", SshEd25519PublicKeyJCE.class, publickeys);
		
		testPublicKey("rsa-sha2-256", Ssh2RsaPublicKeySHA256.class, publickeys);
		testPublicKey("rsa-sha2-512", Ssh2RsaPublicKeySHA512.class, publickeys);
		
		testPublicKey("ecdsa-sha2-nistp256", Ssh2EcdsaSha2Nist256PublicKey.class, publickeys);
		testPublicKey("ecdsa-sha2-nistp384", Ssh2EcdsaSha2Nist384PublicKey.class, publickeys);
		testPublicKey("ecdsa-sha2-nistp521", Ssh2EcdsaSha2Nist521PublicKey.class, publickeys);
		
		testPublicKey("ssh-rsa-cert-v01@openssh.com", OpenSshRsaCertificate.class, publickeys);
		testPublicKey("ecdsa-sha2-nistp256-cert-v01@openssh.com", OpenSshEcdsaSha2Nist256Certificate.class, publickeys);
		testPublicKey("ecdsa-sha2-nistp384-cert-v01@openssh.com", OpenSshEcdsaSha2Nist384Certificate.class, publickeys);
		testPublicKey("ecdsa-sha2-nistp521-cert-v01@openssh.com", OpenSshEcdsaSha2Nist521Certificate.class, publickeys);
		testPublicKey("ssh-ed25519-cert-v01@openssh.com", OpenSshEd25519Certificate.class, publickeys);
					
		loadExternalComponents("publickey.properties", publickeys);
		
		testPublicKey("ssh-rsa", Ssh2RsaPublicKey.class, publickeys);
		testPublicKey("ssh-dss", Ssh2DsaPublicKey.class, publickeys);

	}

	private void testPublicKey(String name, Class<? extends SshPublicKey> pub,
			ComponentFactory<SshPublicKey> publickeys) {

		if(System.getProperties().containsKey(String.format("disable.%s",  name))) {
			if(Log.isDebugEnabled()) {
				Log.debug("   {} WILL NOT be supported because it has been explicitly disabled by a system property", name);
			}
			return;
		}
		
		try {
			SshPublicKey key = pub.newInstance();
			String provider = key.test();
			if(Log.isDebugEnabled())
				Log.debug("   " + name + " will be supported using JCE Provider " + provider);
			publickeys.add(name, pub);
		} catch (Throwable e) {
			if(Log.isDebugEnabled())
				Log.debug("   " + name + " will not be supported: " + e.getMessage());
		}
	}

	protected void initializeSsh2CipherFactory(ComponentFactory<SshCipher> ciphers) {

		if (testJCECipher("chacha20-poly1305@openssh.com", ChaCha20Poly1305.class)) {
			ciphers.add("chacha20-poly1305@openssh.com", ChaCha20Poly1305.class);
		}
		
		if (testJCECipher("aes128-ctr", AES128Ctr.class)) {
			ciphers.add("aes128-ctr", AES128Ctr.class);
		}

		if (testJCECipher("aes192-ctr", AES192Ctr.class)) {
			ciphers.add("aes192-ctr", AES192Ctr.class);
		}

		if (testJCECipher("aes256-ctr", AES256Ctr.class)) {
			ciphers.add("aes256-ctr", AES256Ctr.class);
		}

		if (testJCECipher("3des-ctr", TripleDesCtr.class)) {
			ciphers.add("3des-ctr", TripleDesCtr.class);
		}

		if (testJCECipher("aes128-gcm@openssh.com", AES128Gcm.class)) {
			ciphers.add("aes128-gcm@openssh.com", AES128Gcm.class);
		}

		if (testJCECipher("aes256-gcm@openssh.com", AES256Gcm.class)) {
			ciphers.add("aes256-gcm@openssh.com", AES256Gcm.class);
		}

		loadExternalComponents("cipher.properties", ciphers);

	}

	

	public boolean testJCECipher(String name, Class<? extends SshCipher> cls) {
		
		if(System.getProperties().containsKey(String.format("disable.%s",  name))) {
			if(Log.isDebugEnabled()) {
				Log.debug("   {} WILL NOT be supported because it has been explicitly disabled by a system property", name);
			}
			return false;
		}
		
		SshCipher c = null;
		try {
			c = (SshCipher) cls.newInstance();
			byte[] tmp = new byte[1024];
			getSecureRandom().nextBytes(tmp);
			c.init(SshCipher.ENCRYPT_MODE, tmp, tmp);

			if (c instanceof AbstractJCECipher)
				if(Log.isDebugEnabled())
					Log.debug("   " + name + " will be supported using JCE Provider "
							+ ((AbstractJCECipher) c).getProvider());

			return true;
		} catch (Throwable e) {
			if(Log.isDebugEnabled()) {
				Log.debug("   " + name + " WILL NOT be supported: " + e.getMessage());
			}
			return false;
		}
	}

	public static boolean testDigest(String name, Class<? extends Digest> cls) {
		
		if(System.getProperties().containsKey(String.format("disable.%s",  name))) {
			if(Log.isDebugEnabled()) {
				Log.debug("   {} WILL NOT be supported because it has been explicitly disabled by a system property", name);
			}
			return false;
		}
		
		Digest c = null;
		try {
			c = (Digest) cls.newInstance();

			if (c instanceof AbstractDigest)
				if(Log.isDebugEnabled())
					Log.debug("   " + name + " will be supported using JCE Provider "
							+ ((AbstractDigest) c).getProvider());

			return true;
		} catch (Throwable e) {
			if(Log.isDebugEnabled()) {
				if(c!=null && ((AbstractDigest) c).getProvider()!=null) {
					Log.debug("   " + name + " WILL NOT be supported from JCE Provider " + ((AbstractDigest) c).getProvider() + ": " + e.getMessage());
				} else {
					Log.debug("   " + name + " WILL NOT be supported: " + e.getMessage());
				}
			}
			return false;
		}
	}

	private boolean testHMac(String name, Class<? extends SshHmac> cls) {
		
		if(System.getProperties().containsKey(String.format("disable.%s",  name))) {
			if(Log.isDebugEnabled()) {
				Log.debug("   {} WILL NOT be supported because it has been explicitly disabled by a system property", name);
			}
			return false;
		}
		
		SshHmac c = null;
		try {
			c = (SshHmac) cls.newInstance();
			byte[] tmp = new byte[1024];
			c.init(tmp);

			if (c instanceof AbstractHmac)
				if(Log.isDebugEnabled())
					Log.debug(
							"   " + name + " will be supported using JCE Provider " + ((AbstractHmac) c).getProvider());

			return true;
		} catch (Throwable e) {
			if(Log.isDebugEnabled()) {
				Log.debug("   " + name + " WILL NOT be supported: " + e.getMessage());
			}
			return false;
		}
	}

	public static ComponentManager getDefaultInstance() {
		return getInstance();
	}

}
