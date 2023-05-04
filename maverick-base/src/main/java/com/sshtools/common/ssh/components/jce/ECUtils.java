package com.sshtools.common.ssh.components.jce;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.EllipticCurve;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import com.sshtools.common.util.SimpleASNWriter;

public class ECUtils {

	public static byte[] toByteArray(ECPoint e, EllipticCurve curve) {
		byte[] x = e.getAffineX().toByteArray();
		byte[] y = e.getAffineY().toByteArray();
		int i, xoff = 0, yoff = 0;
		for (i = 0; i < x.length - 1; i++)
			if (x[i] != 0) {
				xoff = i;
				break;
			}
		for (i = 0; i < y.length - 1; i++)
			if (y[i] != 0) {
				yoff = i;
				break;
			}
		int len = (curve.getField().getFieldSize() + 7) / 8;
		if ((x.length - xoff) > len || (y.length - yoff) > len)
			return null;
		byte[] ret = new byte[len * 2 + 1];
		ret[0] = 4;
		System.arraycopy(x, xoff, ret, 1 + len - (x.length - xoff), x.length
				- xoff);
		System.arraycopy(y, yoff, ret, ret.length - (y.length - yoff), y.length
				- yoff);
		return ret;
	}

	public static ECPoint fromByteArray(byte[] b, EllipticCurve curve) {
		int len = (curve.getField().getFieldSize() + 7) / 8;
		if (b.length != 2 * len + 1 || b[0] != 4)
			return null;
		byte[] x = new byte[len];
		byte[] y = new byte[len];
		System.arraycopy(b, 1, x, 0, len);
		System.arraycopy(b, len + 1, y, 0, len);
		return new ECPoint(new BigInteger(1,x), new BigInteger(1,y));
	}

	public static byte[] ensureLeadingZero(byte[] x) {
		if(x[0]!=0) {
			byte[] tmp = new byte[x.length+1];
			System.arraycopy(x, 0, tmp, 1, x.length);
			return tmp;
		}
		return x;
	}
	
	public static String getNameFromEncodedKey(PrivateKey prv) {
		
		byte[] encoded = prv.getEncoded();
		byte[] secp256r1 = new byte[] { 0x2A, (byte) 0x86, 0x48, (byte) 0xCE, 0x3D, 0x03, 0x01, 0x07};
		if(contains(encoded, secp256r1)) {
			return "secp256r1";
		}
		byte[] secp384r1 = new byte[] { 0x2B, (byte) 0x81, 0x04, 0x00, 0x22};
		if(contains(encoded, secp384r1)) {
			return "secp384r1";
		}
		byte[] secp521r1 = new byte[] { 0x2B, (byte) 0x81, 0x04, 0x00, 0x23};
		if(contains(encoded, secp521r1)) {
			return "secp521r1";
		}
		throw new IllegalArgumentException("Unable to determine EC curve type.");
		
	}
	
	private static boolean contains(byte[] source, byte[] find) {
		
		int i;
		for(i=0;i<source.length;i++) {
			if(source[i]==find[0]) {
				boolean matched = true;
				int numBytes = 0;
				for(int x=0;x<find.length && x+i < source.length;x++) {
					if(source[i+x]!=find[x]) {
						matched = false;
						break;
					}
					numBytes++;
				}
				if(matched && numBytes==find.length) {
					return true;
				}
			}
		}
		return false;
	}
	private static byte[] createHeadForNamedCurve(String name, byte[] encoded)
	        throws NoSuchAlgorithmException,
	        InvalidAlgorithmParameterException, IOException {
		

		SimpleASNWriter seq1 = new SimpleASNWriter();
		seq1.writeByte(0x06);
		seq1.writeData(new byte[] {0x2A, (byte)0x86, 0x48, (byte)0xCE, 0x3D, 0x02, 0x01});
		
		switch(name) {
		case "secp256r1":
		case "nistp256":
			seq1.writeByte(0x06);
			seq1.writeData(new byte[] { 0x2A, (byte) 0x86, 0x48, (byte) 0xCE, 0x3D, 0x03, 0x01, 0x07});
			break;
		case "secp384r1":
		case "nistp384":
			seq1.writeByte(0x06);
			seq1.writeData(new byte[] { 0x2B, (byte) 0x81, 0x04, 0x00, 0x22});
			break;
		case "secp521r1":
		case "nistp521":
			seq1.writeByte(0x06);
			seq1.writeData(new byte[] { 0x2B, (byte) 0x81, 0x04, 0x00, 0x23});
			break;
		default:
			throw new IllegalStateException(String.format("Unsupported named curve %s", name));
		}
		
		
		SimpleASNWriter seq2 = new SimpleASNWriter();
		
		seq2.writeByte(0x30);
		seq2.writeData(seq1.toByteArray());
		
		seq2.writeByte(0x03);
		
		byte[] k = new byte[encoded.length+1];
		if(encoded[0]!=0) {
			System.arraycopy(encoded, 0, k, 1, encoded.length);
			seq2.writeData(k);
		} else {
			seq2.writeData(encoded);
		}
		
		SimpleASNWriter seq = new SimpleASNWriter();
		seq.writeByte(0x30);
		seq.writeData(seq2.toByteArray());

	    return seq.toByteArray();
	}
	
	public static ECPublicKey convertKey(byte[] encodedKey) throws InvalidKeySpecException {
		
		KeyFactory eckf;
	    try {
	        eckf = JCEProvider.getProviderForAlgorithm(JCEProvider.getECDSAAlgorithmName()) == null ? KeyFactory
					.getInstance(JCEProvider.getECDSAAlgorithmName()) : KeyFactory
					.getInstance(JCEProvider.getECDSAAlgorithmName(), JCEProvider
							.getProviderForAlgorithm(JCEProvider.getECDSAAlgorithmName()));
	    } catch (NoSuchAlgorithmException e) {
	        throw new IllegalStateException("EC key factory not present in runtime");
	    }
	    X509EncodedKeySpec ecpks = new X509EncodedKeySpec(encodedKey);
	    return (ECPublicKey) eckf.generatePublic(ecpks);
	}
	
	public static ECPrivateKey decodePrivateKey(byte[] key, ECPublicKey pub) throws InvalidKeySpecException {
    
		BigInteger bi = new BigInteger(1, key);
		ECPrivateKeySpec spec = new ECPrivateKeySpec(bi, pub.getParams());
				
		KeyFactory eckf;
	    try {
	        eckf = JCEProvider
					.getProviderForAlgorithm(JCEProvider.getECDSAAlgorithmName()) == null ? KeyFactory
					.getInstance(JCEProvider.getECDSAAlgorithmName()) : KeyFactory
					.getInstance(JCEProvider.getECDSAAlgorithmName(), JCEProvider
							.getProviderForAlgorithm(JCEProvider.getECDSAAlgorithmName()));
	    } catch (NoSuchAlgorithmException e) {
	        throw new IllegalStateException("EC key factory not present in runtime");
	    }
	    return (ECPrivateKey) eckf.generatePrivate(spec);
	}

	public static byte[] stripLeadingZeros(byte[] b) {
		int count = 0;
		for(int i=0;i<b.length;i++) {
			if(b[i]!=0) {
				break;
			}
			count++;
		}
		byte[] tmp = new byte[b.length-count];
		System.arraycopy(b, count, tmp, 0, tmp.length);
		return tmp;
	}
	
//	private static int getHeadSize(String curve) throws IOException {
//		switch(curve) {
//		case "prime256v1":
//		case"secp256r1":
//			return 26;
//		case "secp384r1":
//			return 23;
//		case "secp521r1":
//			return 25;
//		default:
//			throw new IOException("Unsupported curve name " + curve);
//		}
//	}
	
	public static ECPublicKey decodeKey(byte[] encoded, String namedCurve) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException, InvalidAlgorithmParameterException { 
		return convertKey(createHeadForNamedCurve(namedCurve, encoded));
	}
	
	public static ECPublicKey decodeJCEKey(byte[] encoded) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException, InvalidAlgorithmParameterException { 
		KeyFactory eckf;
	    try {
	        eckf = JCEProvider
					.getProviderForAlgorithm(JCEProvider.getECDSAAlgorithmName()) == null ? KeyFactory
					.getInstance(JCEProvider.getECDSAAlgorithmName()) : KeyFactory
					.getInstance(JCEProvider.getECDSAAlgorithmName(), JCEProvider
							.getProviderForAlgorithm(JCEProvider.getECDSAAlgorithmName()));
					
	    } catch (NoSuchAlgorithmException e) {
	        throw new IllegalStateException("EC key factory not present in runtime");
	    }
	    X509EncodedKeySpec ecpks = new X509EncodedKeySpec(encoded);
	    return (ECPublicKey) eckf.generatePublic(ecpks);
	}

	public static byte[] getOidBytes(String curve) {
		switch(curve) {
		case "secp256r1":
			return new byte[] { 0x2A, (byte) 0x86, 0x48, (byte) 0xCE, 0x3D, 0x03, 0x01, 0x07};
		case "secp384r1":
			 return new byte[] { 0x2B, (byte) 0x81, 0x04, 0x00, 0x22};
		case "secp521r1":
			return new byte[] { 0x2B, (byte) 0x81, 0x04, 0x00, 0x23};
		default:
			throw new IllegalStateException(String.format("Unsupported named curve %s", curve));
	}
	}
	
}
