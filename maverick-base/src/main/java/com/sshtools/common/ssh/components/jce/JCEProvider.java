package com.sshtools.common.ssh.components.jce;

/*-
 * #%L
 * Base API
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

import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.util.Hashtable;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;

import com.sshtools.common.logger.Log;

public class JCEProvider implements JCEAlgorithms {

	
	
	static Provider defaultProvider = null;
	static Provider bcProvider = null;
	static Hashtable<String,Provider> specficProviders = new Hashtable<String,Provider>();
	static String secureRandomAlgorithm = null;
	static Boolean bcEnabled = null;
	static String ecdsaAlgorithmName = "EC";
	static String rsaOAEPSHA256WithMG1Padding = "RSA/None/OAEPWithSHA256AndMGF1Padding";
	static String rsaOAEPSHA1WithMG1Padding = "RSA/None/OAEPWithSHA1AndMGF1Padding";
	
	static boolean enableSC = false;
	
	static SecureRandom secureRandom;
	
	static enum BC_FLAVOR {
		BC,
		BCFIPS,
		SC
	};
	
	/**
	 * Initialize the default JCE provider used by the API. 
	 * @param provider
	 */
	public static void initializeDefaultProvider(Provider provider) {
		JCEProvider.defaultProvider = provider;
	}
	
	/**
	 * Initialize the default JCE provider used by the API. 
	 * @param provider
	 * @throws NoSuchProviderException 
	 */
	public static void initializeDefaultProvider(String name) throws NoSuchProviderException {
		Provider provider = Security.getProvider(name);
		if(provider==null) {
			throw new NoSuchProviderException();
		}
		initializeDefaultProvider(provider);
	}
	
	/**
	 * Initialize a provider for a specific algorithm.
	 * @param jceAlgorithm
	 * @param provider
	 */
	public static void initializeProviderForAlgorithm(String jceAlgorithm, Provider provider) {
		if("executable".equals(System.getProperty("org.graalvm.nativeimage.kind", ""))) {
			Log.warn("Leaving provider configuration as running a native build.");
		}
		else
			specficProviders.put(jceAlgorithm, provider);
	}
	
	/**
	 * Initialize a provider for a specific algorithm.
	 * @param jceAlgorithm
	 * @param provider
	 * @throws NoSuchProviderException 
	 */
	public static void initializeProviderForAlgorithm(String jceAlgorithm, String name) throws NoSuchProviderException {
		Provider provider = Security.getProvider(name);
		if(provider==null) {
			throw new NoSuchProviderException();
		}
		initializeProviderForAlgorithm(jceAlgorithm, provider);
		
	}

	/**
	 * Get the algorithm used for secure random number generation.
	 * @return String
	 */
	public static String getSecureRandomAlgorithm() {
		return secureRandomAlgorithm;
	}

	/**
	 * Set the algorithm used for secure random number generation.
	 * @param secureRandomAlgorithm
	 */
	public static void setSecureRandomAlgorithm(String secureRandomAlgorithm) {
		JCEProvider.secureRandomAlgorithm = secureRandomAlgorithm;
	}

	/**
	 * Get the provider for a specific algorithm.
	 * @param jceAlgorithm
	 * @return Provider
	 */
	public static Provider getProviderForAlgorithm(String jceAlgorithm) {
		if(specficProviders.containsKey(jceAlgorithm)) {
			return (Provider) specficProviders.get(jceAlgorithm);
		}
		
		return defaultProvider;
	}	
	
	/**
	 * Get the secure random implementation for the API.
	 * @return SecureRandom
	 * @throws NoSuchAlgorithmException
	 */
	public static SecureRandom getSecureRandom() {

		if(secureRandom==null) {
			try {
				if(JCEProvider.getSecureRandomAlgorithm()==null) {
					secureRandom = new SecureRandom();
				} else {
					return secureRandom = JCEProvider.getProviderForAlgorithm(JCEProvider.getSecureRandomAlgorithm())==null ?
						SecureRandom.getInstance(JCEProvider.getSecureRandomAlgorithm()) :
							SecureRandom.getInstance(JCEProvider.getSecureRandomAlgorithm(),
									JCEProvider.getProviderForAlgorithm(JCEProvider.getSecureRandomAlgorithm()));
					}
			} catch (NoSuchAlgorithmException e) {
				return secureRandom = new SecureRandom();
			}
		}
		
		return secureRandom;
	}	
	
	public static Provider getDefaultProvider() {
		return defaultProvider;
	}

	static void setBCProvider(Provider provider) {
		bcProvider = provider;
	}
	
	public static boolean hasBCProvider() {
		return bcProvider!=null && bcEnabled;
	}
	
	
	public static Provider getBCProvider() {
		if(bcProvider==null) {
			configureBC();
		}
		return bcProvider;
	}

	public static void enableSpongyCastle(boolean makeDefault) {
		enableSC = true;
		enableBouncyCastle(makeDefault);
	}
	
	public static void enableBouncyCastle(boolean makeDefault) {
		if("executable".equals(System.getProperty("org.graalvm.nativeimage.kind", ""))) {
			Log.warn("Leaving provider configuration as running a native build.");
			return;
		}
		
		BC_FLAVOR bcFlavor = configureBC();
		
		if(bcProvider==null) {
			throw new IllegalStateException("Bouncycastle JCE provider cannot be found on the classpath");
		}
		
		bcEnabled = true;
		boolean add = true;
		for(Provider p : Security.getProviders()) {
			if(p.getName().equals(bcProvider.getName())) {
				add = false;
				break;
			}
		}
		
		if(add) {
			if(Log.isInfoEnabled()) {
				Log.info("Adding Bouncycastle {} provider to Security Providers", bcProvider.getName());
			}
			if(bcFlavor==BC_FLAVOR.SC) {
				Security.insertProviderAt(bcProvider, 1);
			} else {
				Security.addProvider(bcProvider);
			}
		}

		if(!bcFlavor.equals(BC_FLAVOR.SC)) {
			JCEProvider.setECDSAAlgorithmName("ECDSA");
		}
		
		JCEProvider.setRSAOAEPSHA256AlgorithmName("RSA/None/OAEPWithSHA256AndMGF1Padding");
		JCEProvider.setRSAOAEPSHA1AlgorithmName("RSA/None/OAEPWithSHA1AndMGF1Padding");
		
		if(makeDefault) {
			if(Log.isInfoEnabled()) {
				Log.info("Configuring Bouncycastle {} provider as default for all algorithms", bcProvider.getName());
			}
			initializeDefaultProvider(bcProvider);
		} else {
			if(Log.isInfoEnabled()) {
				Log.info("Configuring DH support with Bouncycastle {} provider", bcProvider.getName());
			}
			initializeProviderForAlgorithm(JCEAlgorithms.JCE_DH, bcProvider);
			initializeProviderForAlgorithm(JCEAlgorithms.JCE_DH_KEY_AGREEMENT, bcProvider);
			initializeProviderForAlgorithm(JCEAlgorithms.JCE_DH_KEY_FACTORY, bcProvider);
			initializeProviderForAlgorithm(JCEAlgorithms.JCE_DH_KEY_GENERATOR, bcProvider);
		}
	}

	private static BC_FLAVOR configureBC() {
	
		try {
			if(enableSC) {
				@SuppressWarnings("unchecked")
				Class<Provider> cls = (Class<Provider>) Class.forName("org.spongycastle.jce.provider.BouncyCastleProvider");
				bcProvider = (Provider) cls.getConstructor().newInstance();
				return BC_FLAVOR.SC;
			}
		} catch (Throwable e) {
		}
		
		try {
			@SuppressWarnings("unchecked")
			Class<Provider> cls = (Class<Provider>) Class.forName("org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider");
			bcProvider = (Provider) cls.getConstructor().newInstance();
			return BC_FLAVOR.BCFIPS;
		} catch(Throwable t) {
			try {
				@SuppressWarnings("unchecked")
				Class<Provider> cls = (Class<Provider>) Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider");
				bcProvider = (Provider) cls.getConstructor().newInstance();
				return BC_FLAVOR.BC;
			} catch(Throwable f) {
				throw new IllegalStateException("Bouncycastle, BCFIPS or SpongyCastle is not installed");
			}
		}
	}
	
	public static KeyFactory getDHKeyFactory() throws NoSuchAlgorithmException {
		try {
			return JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_DH_KEY_FACTORY)==null ? 
			 		  KeyFactory.getInstance(JCEAlgorithms.JCE_DH) : 
			 			 KeyFactory.getInstance(JCEAlgorithms.JCE_DH, JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_DH_KEY_FACTORY));
		} catch (NoSuchAlgorithmException e) {
			return KeyFactory.getInstance(JCEAlgorithms.JCE_DH);
		} 
	}
	
	public static KeyAgreement getDHKeyAgreement() throws NoSuchAlgorithmException {
		try {
			return JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_DH_KEY_GENERATOR)==null ? 
			  		  KeyAgreement.getInstance(JCEAlgorithms.JCE_DH) : 
			  			  KeyAgreement.getInstance(JCEAlgorithms.JCE_DH, JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_DH_KEY_GENERATOR));
		} catch (NoSuchAlgorithmException e) {
			return KeyAgreement.getInstance(JCEAlgorithms.JCE_DH);
		}

	}
	
	public static KeyPairGenerator getDHKeyGenerator() throws NoSuchAlgorithmException {
		try {
			return JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_DH_KEY_AGREEMENT)==null ? 
			  		  KeyPairGenerator.getInstance(JCEAlgorithms.JCE_DH) : 
			  	      KeyPairGenerator.getInstance(JCEAlgorithms.JCE_DH, 
			  	    		  JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_DH_KEY_AGREEMENT));
		} catch (NoSuchAlgorithmException e) {
			return KeyPairGenerator.getInstance(JCEAlgorithms.JCE_DH);
		}
	}
	
	
	public static void disableBouncyCastle() {
		
		try {
			KeyPairGenerator.getInstance("ECDSA");
			JCEProvider.setECDSAAlgorithmName("ECDSA");
		} catch (NoSuchAlgorithmException e) {
			try {
				KeyPairGenerator.getInstance("EC");
				JCEProvider.setECDSAAlgorithmName("EC");
			} catch (NoSuchAlgorithmException e1) {
				Log.error("Unable to determine correct Elliptic Curve algorithm name ");
			}
		}
		
		try {
			Cipher.getInstance("RSA/NONE/OAEPWithSHA-256AndMGF1Padding");
			JCEProvider.setRSAOAEPSHA256AlgorithmName("RSA/None/OAEPWithSHA-256AndMGF1Padding");
		} catch(NoSuchAlgorithmException | NoSuchPaddingException e) {
			try {
			Cipher.getInstance("RSA/v/OAEPWithSHA256AndMGF1Padding");
			JCEProvider.setRSAOAEPSHA256AlgorithmName("RSA/None/OAEPWithSHA256AndMGF1Padding");
			} catch(NoSuchAlgorithmException | NoSuchPaddingException e1) {
				Log.error("Unable to determine correct JCE algorithm name for RSA/None/OAEPWithSHA256AndMGF1Padding");
			}
		}

		try {
			Cipher.getInstance("RSA/v/OAEPWithSHA-1AndMGF1Padding");
			JCEProvider.setRSAOAEPSHA256AlgorithmName("RSA/None/OAEPWithSHA-1AndMGF1Padding");
		} catch(NoSuchAlgorithmException | NoSuchPaddingException e) {
			try {
			Cipher.getInstance("RSA/NONE/OAEPWithSHA1AndMGF1Padding");
			JCEProvider.setRSAOAEPSHA256AlgorithmName("RSA/None/OAEPWithSHA1AndMGF1Padding");
			} catch(NoSuchAlgorithmException | NoSuchPaddingException e1) {
				Log.error("Unable to determine correct JCE algorithm name for RSA/None/OAEPWithSHA1AndMGF1Padding");
			}
		}
		
		if(JCEProvider.isBCEnabled()) {
			if(Log.isInfoEnabled()) {
				Log.info("Disabling support for Bouncycastle {} provider", bcProvider.getName());
			}
			Security.removeProvider(bcProvider.getName());
			initializeDefaultProvider((Provider)null);
			specficProviders.remove(JCEAlgorithms.JCE_DH);
		}
		bcEnabled = false;
	}

	public static boolean isBCEnabled() {
		if(bcProvider==null) {
			return false;
		}
		return bcProvider!=null && bcEnabled != null && bcEnabled;
	}

	public static String getECDSAAlgorithmName() {
		return ecdsaAlgorithmName;
	}
	
	public static void setECDSAAlgorithmName(String ecdsaAlgorithmName) {
		JCEProvider.ecdsaAlgorithmName = ecdsaAlgorithmName;
	}

	public static boolean isBCDisabled() {
		return bcEnabled!=null && !bcEnabled;
	}
	
	public static void setRSAOAEPSHA256AlgorithmName(String rsaOAEPWithMG1Padding) {
		JCEProvider.rsaOAEPSHA256WithMG1Padding = rsaOAEPWithMG1Padding;
	}
	
	public static String getRSAOAEPSHA256AlgorithmName() {
		return rsaOAEPSHA256WithMG1Padding;
	}

	public static String getRSAOAEPSHA1AlgorithmName() {
		return rsaOAEPSHA1WithMG1Padding;
	}
	
	public static void setRSAOAEPSHA1AlgorithmName(String rsaOAEPWithMG1Padding) {
		JCEProvider.rsaOAEPSHA1WithMG1Padding = rsaOAEPWithMG1Padding;
	}
	
	public static KeyFactory getKeyFactory(String alg) throws NoSuchAlgorithmException {
		if(JCEProvider.getProviderForAlgorithm(alg)==null) {
			if(Log.isTraceEnabled()) {
				Log.trace("Getting key factory algorithm {} from default provider", alg);
			}
			return KeyFactory.getInstance(alg);
		} else {
			if(Log.isTraceEnabled()) {
				Log.trace("Getting key factory algorithm {} from provider {}", alg, JCEProvider.getProviderForAlgorithm(alg));
			}
		    return KeyFactory.getInstance(alg, JCEProvider.getProviderForAlgorithm(alg));
		}
	}

	public static Signature getSignature(String alg) throws NoSuchAlgorithmException {
		if(JCEProvider.getProviderForAlgorithm(alg)==null) {
			if(Log.isTraceEnabled()) {
				Log.trace("Getting signature algorithm {} from default provider", alg);
			}
			return Signature.getInstance(alg);
		} else {
			if(Log.isTraceEnabled()) {
				Log.trace("Getting signature algorithm {} from provider {}", alg, JCEProvider.getProviderForAlgorithm(alg));
			}
		    return Signature.getInstance(alg, JCEProvider.getProviderForAlgorithm(alg));
		}		
	}
}
