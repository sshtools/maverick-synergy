/* HEADER */
package com.sshtools.common.ssh;

import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.Digest;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.util.Base64;

/**
 * Utility methods to generate an SSH public key fingerprint.
 *
 * @author Lee David Painter
 */
public class SshKeyFingerprint {

	public final static String MD5_FINGERPRINT = "MD5";
	public final static String SHA1_FINGERPRINT = "SHA-1";
	public final static String SHA256_FINGERPRINT = "SHA256";

	private static String defaultHashAlgoritm = SHA256_FINGERPRINT;

	static char VOWELS[] = { 'a', 'e', 'i', 'o', 'u', 'y' };
	static char CONSONANTS[] = { 'b', 'c', 'd', 'f', 'g', 'h', 'k', 'l', 'm', 'n', 'p', 'r', 's', 't', 'v', 'z', 'x' };

	static char[] HEX = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	/**
	 * Generate an SSH key fingerprint as defined in
	 * draft-ietf-secsh-fingerprint-00.txt.
	 * 
	 * @param encoded
	 * @return the key fingerprint, for example
	 *         "c1:b1:30:29:d7:b8:de:6c:97:77:10:d7:46:41:63:87"
	 */
	public static String getFingerprint(byte[] encoded) throws SshException {
		return getFingerprint(encoded, defaultHashAlgoritm);
	}

	public static void setDefaultHashAlgorithm(String defaultHashAlgorithm) {
		SshKeyFingerprint.defaultHashAlgoritm = defaultHashAlgorithm;
	}

	/**
	 * Generate an SSH key fingerprint with a specific algorithm.
	 * 
	 * @param encoded
	 * @param algorithm
	 * @return the key fingerprint, for example
	 *         "c1:b1:30:29:d7:b8:de:6c:97:77:10:d7:46:41:63:87"
	 */
	public static String getFingerprint(byte[] encoded, String algorithm) throws SshException {

		Digest md5 = (Digest) ComponentManager.getInstance().supportedDigests().getInstance(algorithm);

		md5.putBytes(encoded);

		byte[] digest = md5.doFinal();

		StringBuffer buf = new StringBuffer();
		buf.append(algorithm);
		buf.append(":");
		if (algorithm.equals(SHA256_FINGERPRINT)) {
			buf.append(Base64.encodeBytes(digest, true));
			while (buf.charAt(buf.length() - 1) == '=') {
				buf.delete(buf.length() - 1, buf.length());
			}
		} else {

			int ch;
			for (int i = 0; i < digest.length; i++) {
				ch = digest[i] & 0xFF;
				if (i > 0) {
					buf.append(':');
				}
				buf.append(HEX[(ch >>> 4) & 0x0F]);
				buf.append(HEX[ch & 0x0F]);
			}
		}
		return buf.toString();
	}

	public static String getFingerprint(SshPublicKey key) {
		try {
			return getFingerprint(key.getEncoded(), defaultHashAlgoritm);
		} catch (SshException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
	
	public static String getBubbleBabble(SshPublicKey key) {
		try {
			return getBubbleBabble(key.getEncoded());
		} catch (SshException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
	
	public static String getBubbleBabble(byte[] encoded) {

		try {
			Digest sha1 = (Digest) ComponentManager.getInstance().supportedDigests().getInstance(SHA1_FINGERPRINT);
			sha1.putBytes(encoded);
			encoded = sha1.doFinal();
			
			int r = (encoded.length / 2) + 1;
			int s = 1;

			StringBuilder b = new StringBuilder();
			b.append('x');
			for (int x = 0; x < r; x++) {
				if ((x + 1 < r) || (encoded.length % 2 != 0)) {
					b.append(VOWELS[((((encoded[2 * x]  & 0xFF) >> 6) & 3) + s) % 6]);
					b.append(CONSONANTS[((encoded[2 * x]  & 0xFF) >> 2) & 15]);
					b.append(VOWELS[(((encoded[2 * x] & 0xFF) & 3) + (s / 6)) % 6]);
					if ((x + 1) < r) {
						b.append(CONSONANTS[((encoded[(2 * x) + 1]  & 0xFF) >> 4) & 15]);
						b.append('-');
						b.append(CONSONANTS[(encoded[(2 * x) + 1]  & 0xFF) & 15]);
						s = ((s * 5) + ((((encoded[2 * x] & 0xFF)) * 7) + (((encoded[(2 * x) + 1] & 0xFF))))) % 36;
					}
				} else {
					b.append(VOWELS[s % 6]);
					b.append(CONSONANTS[16]);
					b.append(VOWELS[s / 6]);
				}
			}
			b.append('x');
			return b.toString();
		} catch (SshException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	public static void main(String[] args) {

		System.out.println(getBubbleBabble("".getBytes())); // xexax
		System.out.println(getBubbleBabble("1234567890".getBytes())); // xesef-disof-gytuf-katof-movif-baxux
		System.out.println(getBubbleBabble("Pineapple".getBytes())); // xigak-nyryk-humil-bosek-sonax

	}
}
