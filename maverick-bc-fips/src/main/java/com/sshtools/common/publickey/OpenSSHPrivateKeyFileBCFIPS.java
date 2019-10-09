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
/* HEADER */
package com.sshtools.common.publickey;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMEncryptorBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.jcajce.JcePKCSPBEInputDecryptorProviderBuilder;

import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.jce.ECUtils;
import com.sshtools.common.ssh.components.jce.JCEProvider;
import com.sshtools.common.ssh.components.jce.Ssh2DsaPrivateKey;
import com.sshtools.common.ssh.components.jce.Ssh2DsaPublicKey;
import com.sshtools.common.ssh.components.jce.Ssh2EcdsaSha2NistPrivateKey;
import com.sshtools.common.ssh.components.jce.Ssh2EcdsaSha2NistPublicKey;
import com.sshtools.common.ssh.components.jce.Ssh2RsaPrivateCrtKey;
import com.sshtools.common.ssh.components.jce.Ssh2RsaPublicKey;

public class OpenSSHPrivateKeyFileBCFIPS
   implements SshPrivateKeyFile {

  byte[] formattedkey;

public OpenSSHPrivateKeyFileBCFIPS(byte[] formattedkey)
     throws IOException {
    if(!isFormatted(formattedkey)) {
      throw new IOException(
         "Formatted key data is not a valid OpenSSH key format");
    }
    this.formattedkey = formattedkey;
    
    /**
     * Force use of PEM reader so we don't fool ourselves 
     * into thinking this file type is supported when PEM
     * reader is not present. 
     */
   	try {
		toKeyPair(null);
	} catch (InvalidPassphraseException e) {
	}
  }

  public OpenSSHPrivateKeyFileBCFIPS(SshKeyPair pair, String passphrase)
     throws IOException {
    formattedkey = encryptKey(pair, passphrase);
  }

  /* (non-Javadoc)
   * @see com.sshtools.publickey.SshPrivateKeyFile#isPassphraseProtected()
   */
  public boolean isPassphraseProtected() {
    try {
      Reader r = new StringReader(new String(formattedkey, "US-ASCII"));
      com.sshtools.common.publickey.PEMReader pem = new com.sshtools.common.publickey.PEMReader(r);

      return pem.getHeader().containsKey("DEK-Info") || pem.getType().startsWith("ENCRYPTED");
    }
    catch(IOException e) {
      return true;
    }
  }

  public String getType() {
    return "OpenSSH";
  }

  public boolean supportsPassphraseChange() {
    return true;
  }

  public SshKeyPair toKeyPair(final String passphrase)
     throws IOException, InvalidPassphraseException {

    Reader r = new StringReader(new String(formattedkey, "US-ASCII"));
    PEMParser pem = new PEMParser(r);

    try {
	    Object obj = pem.readObject();
	    if(obj==null) {
			throw new IOException("Invalid key file");
		}
	    
	    SshKeyPair pair = new SshKeyPair();
	    
	    if(obj instanceof PKCS8EncryptedPrivateKeyInfo) {
	    	if(passphrase==null || passphrase.equals("")) {
	    		throw new InvalidPassphraseException();
	    	}
	    	PKCS8EncryptedPrivateKeyInfo encPrivKeyInfo = (PKCS8EncryptedPrivateKeyInfo) obj;
	    	InputDecryptorProvider pkcs8Prov = new JcePKCSPBEInputDecryptorProviderBuilder()
		    	    .setProvider(JCEProvider.getBCProvider().getName()).build(passphrase.toCharArray());
		    JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(JCEProvider.getBCProvider().getName());
		    obj = converter.getPrivateKey(encPrivKeyInfo.decryptPrivateKeyInfo(pkcs8Prov));
	    }
	    
	    if(obj instanceof PEMEncryptedKeyPair) {
	    	if(passphrase==null || passphrase.equals("")) {
	    		throw new InvalidPassphraseException();
	    	}
	        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(JCEProvider.getBCProvider().getName());
	        obj = converter.getKeyPair(((PEMEncryptedKeyPair)obj).decryptKeyPair(new JcePEMDecryptorProviderBuilder().setProvider(JCEProvider.getBCProvider().getName()).build(passphrase.toCharArray())));
	    }
	    
	    if(obj instanceof PEMKeyPair) {
	    	obj = new JcaPEMKeyConverter().setProvider(JCEProvider.getBCProvider().getName()).getKeyPair((PEMKeyPair)obj);
	    } else if(obj instanceof PrivateKeyInfo) {
	    	obj = new JcaPEMKeyConverter().setProvider(JCEProvider.getBCProvider().getName()).getPrivateKey((PrivateKeyInfo)obj);
	    }
	    
	    if(obj instanceof KeyPair) {
	    	
	    	KeyPair p = (KeyPair) obj;
		    if(p.getPrivate() instanceof ECPrivateKey) {
		    	ECPrivateKey prv = (ECPrivateKey) p.getPrivate();
		    	String curve = ECUtils.getNameFromEncodedKey(prv);
		    	pair.setPrivateKey(new Ssh2EcdsaSha2NistPrivateKey(prv,
		    			curve));
		    	pair.setPublicKey(new Ssh2EcdsaSha2NistPublicKey((ECPublicKey)p.getPublic(),
		    			curve));
		        return pair;
		    } else if(p.getPrivate() instanceof RSAPrivateCrtKey) {
		    	pair.setPrivateKey(new Ssh2RsaPrivateCrtKey((RSAPrivateCrtKey)p.getPrivate()));
		    	pair.setPublicKey(new Ssh2RsaPublicKey((RSAPublicKey)p.getPublic()));
		    	return pair;
		    } else if(p.getPrivate() instanceof DSAPrivateKey) {
		    	pair.setPrivateKey(new Ssh2DsaPrivateKey((DSAPrivateKey)p.getPrivate(), (DSAPublicKey)p.getPublic()));
		    	pair.setPublicKey(new Ssh2DsaPublicKey((DSAPublicKey)p.getPublic()));    
		    	return pair;
		    }
	    } else if(obj instanceof DSAPrivateKey) {
	    	DSAPrivateKey d = (DSAPrivateKey) obj;
	    	try {
	    		Ssh2DsaPrivateKey dsa = new Ssh2DsaPrivateKey(d);
				pair.setPrivateKey(dsa);
				pair.setPublicKey(dsa.getPublicKey());
			} catch (Exception e) {
				throw new IOException("Failed to generate DSA public key from private key: " + e.getMessage());
			}
	    	return pair;
	    } else if(obj instanceof RSAPrivateCrtKey) {
	    	RSAPrivateCrtKey tmp = (RSAPrivateCrtKey) obj;
	    	try {
				Ssh2RsaPrivateCrtKey rsa = new Ssh2RsaPrivateCrtKey(tmp);
				pair.setPrivateKey(rsa);
				pair.setPublicKey(new Ssh2RsaPublicKey(tmp.getModulus(), tmp.getPublicExponent()));
			} catch (Exception e) {
				throw new IOException("Failed to generate RSA public key from private key: " + e.getMessage());
			}
	    	return pair;
	    }
	    throw new IOException("Unsupported type");

    } catch(InvalidPassphraseException | IOException e) {
    	throw e;
    } catch(Throwable ex) { 
    	return new OpenSSHPrivateKeyFile(formattedkey).toKeyPair(passphrase);
    } finally {
    	pem.close();
    }
  }

 
  public byte[] encryptKey(SshKeyPair pair, String passphrase)
     throws IOException {

	  ByteArrayOutputStream bout = new ByteArrayOutputStream();
	  JcaPEMWriter pem = new JcaPEMWriter(new OutputStreamWriter(bout));
	  
	  try {
		
		  PrivateKey privateKey;
		  PublicKey publicKey;
		  
		  if(pair.getPrivateKey() instanceof Ssh2DsaPrivateKey) {
			  privateKey = ((Ssh2DsaPrivateKey)pair.getPrivateKey()).getJCEPrivateKey();
			  publicKey = ((Ssh2DsaPublicKey)pair.getPublicKey()).getJCEPublicKey();
		  } else if(pair.getPrivateKey() instanceof Ssh2RsaPrivateCrtKey) {
			  privateKey = ((Ssh2RsaPrivateCrtKey)pair.getPrivateKey()).getJCEPrivateKey();
			  publicKey = ((Ssh2RsaPublicKey)pair.getPublicKey()).getJCEPublicKey();		  
		  } else if(pair.getPrivateKey() instanceof Ssh2EcdsaSha2NistPrivateKey) {
			  privateKey = ((Ssh2EcdsaSha2NistPrivateKey)pair.getPrivateKey()).getJCEPrivateKey();
			  publicKey = ((Ssh2EcdsaSha2NistPublicKey)pair.getPublicKey()).getJCEPublicKey();	
		  } else {
			  throw new IOException(pair.getPrivateKey().getClass().getName() + " is not supported in OpenSSH private key files");
		  }
		  
		  KeyPair kp = new KeyPair(publicKey, privateKey);
		  
		  if(passphrase!=null && !"".equals(passphrase)) {
			  pem.writeObject(kp, new JcePEMEncryptorBuilder("AES-128-CBC").setProvider(JCEProvider.getBCProvider().getName()).build(passphrase.toCharArray()));
		  } else {
			  pem.writeObject(kp);
		  }
		  
		  pem.flush();
		  
		  return bout.toByteArray();
	} finally {
		pem.close();
		bout.close();
	}
  }

  /* (non-Javadoc)
   * @see com.sshtools.publickey.SshPrivateKeyFile#changePassphrase(java.lang.String, java.lang.String)
   */
  public void changePassphrase(String oldpassphrase, String newpassphrase)
     throws IOException, InvalidPassphraseException {
    SshKeyPair pair = toKeyPair(oldpassphrase);
    formattedkey = encryptKey(pair, newpassphrase);
  }

  public byte[] getFormattedKey() {
    return formattedkey;
  }

  public static boolean isFormatted(byte[] formattedkey) {
    try {
      Reader r = new StringReader(new String(formattedkey, "UTF-8"));
      @SuppressWarnings("unused")
      com.sshtools.common.publickey.PEMReader pem = 
    	  new com.sshtools.common.publickey.PEMReader(r);
      return true;
    }
    catch(IOException e) {
      return false;
    }
  }

}
