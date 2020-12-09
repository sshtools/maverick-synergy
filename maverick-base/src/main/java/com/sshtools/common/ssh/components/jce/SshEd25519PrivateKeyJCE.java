package com.sshtools.common.ssh.components.jce;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.edec.EdECObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;

import com.sshtools.common.util.IOUtils;
import com.sshtools.common.util.Utils;


public class SshEd25519PrivateKeyJCE implements SshEd25519PrivateKey {

	PrivateKey key;
	
	public SshEd25519PrivateKeyJCE(byte[] sk, byte[] pk) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException, NoSuchProviderException {
		loadPrivateKey(sk, pk);
	}
	
	private void loadPrivateKey(byte[] sk, byte[] pk) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
		KeyFactory keyFactory = KeyFactory.getInstance(JCEAlgorithms.ED25519, "BC");
		PrivateKeyInfo privKeyInfo = new PrivateKeyInfo(
				new AlgorithmIdentifier(EdECObjectIdentifiers.id_Ed25519), 
				new DEROctetString(sk),
				null,
				pk);
		PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(privKeyInfo.getEncoded());
		key = keyFactory.generatePrivate(pkcs8KeySpec);
	}
	
	public SshEd25519PrivateKeyJCE(PrivateKey prv) {
		key = prv;
	}

	@Override
	public byte[] sign(byte[] data) throws IOException {
		return sign(data, getAlgorithm());
	}
	
	@Override
	public byte[] sign(byte[] data, String signingAlgorithm) throws IOException {
		try {
			Signature sgr = Signature.getInstance(JCEAlgorithms.ED25519, "BC");
			sgr.initSign(key);
			sgr.update(data);
			return sgr.sign();
		} catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException | NoSuchProviderException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	@Override
	public String getAlgorithm() {
		return SshEd25519PublicKeyJCE.ALGORITHM_NAME;
	}

	@Override
	public PrivateKey getJCEPrivateKey() {
		return key;
	}

	public byte[] getSeed() {
		ASN1InputStream asn = new ASN1InputStream(key.getEncoded());
		try {
			DLSequence id = (DLSequence) asn.readObject();
			DEROctetString encoded = (DEROctetString) id.getObjectAt(2).toASN1Primitive();
			ASN1InputStream in = new ASN1InputStream(new ByteArrayInputStream(encoded.getOctets()));
			try {
				DEROctetString obj = (DEROctetString) in.readObject();
				return obj.getOctets();
			} finally {
				IOUtils.closeStream(in);
			}
		} catch (IOException e) {
			throw new IllegalStateException("Unable to parse ASN output of JCE key",e);
		} finally {
			IOUtils.closeStream(asn);
		}
	}

	@Override
	public int hashCode() {
		return new String(Utils.bytesToHex(getSeed())).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof SshEd25519PrivateKeyJCE)) {
			return false;
		}
		return getJCEPrivateKey().equals(((SshEd25519PrivateKeyJCE)obj).getJCEPrivateKey());
	}
}
