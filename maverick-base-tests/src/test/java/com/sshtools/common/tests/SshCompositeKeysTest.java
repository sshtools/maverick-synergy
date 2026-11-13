package com.sshtools.common.tests;

/*-
 * #%L
 * Base API Tests
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

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sshtools.common.publickey.SshKeyPairGenerator;
import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.jce.CompositeSignatureUtils;
import com.sshtools.common.ssh.components.jce.SshMldsa44Ed25519PrivateKey;
import com.sshtools.common.ssh.components.jce.SshMldsa44Ed25519PublicKey;
import com.sshtools.common.ssh.components.jce.SshMldsa87P384PrivateKey;
import com.sshtools.common.ssh.components.jce.SshMldsa87P384PublicKey;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SshCompositeKeysTest {

	@BeforeClass
	public static void setup() {
		if (Security.getProvider("BC") == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	// =========================================================================
	// ssh-mldsa44-ed25519 tests
	// =========================================================================

	@Test
	public void testMldsa44Ed25519KeyPairGeneration() throws Exception {
		SshKeyPair pair = SshKeyPairGenerator.generateKeyPair(SshKeyPairGenerator.SSH_MLDSA44_ED25519);
		assertNotNull(pair);
		assertTrue(pair.getPrivateKey() instanceof SshMldsa44Ed25519PrivateKey);
		assertTrue(pair.getPublicKey() instanceof SshMldsa44Ed25519PublicKey);

		SshMldsa44Ed25519PrivateKey prv = (SshMldsa44Ed25519PrivateKey) pair.getPrivateKey();
		SshMldsa44Ed25519PublicKey pub = (SshMldsa44Ed25519PublicKey) pair.getPublicKey();

		assertEquals("ssh-mldsa44-ed25519", prv.getAlgorithm());
		assertEquals("ssh-mldsa44-ed25519", pub.getAlgorithm());
		assertEquals("ssh-mldsa44-ed25519", pub.getSigningAlgorithm());
		assertEquals("ssh-mldsa44-ed25519", pub.getEncodingAlgorithm());
		assertEquals(SecurityLevel.PARANOID, pub.getSecurityLevel());

		assertNotNull(prv.getMldsaSeed());
		assertEquals(SshMldsa44Ed25519PrivateKey.MLDSA44_SEED_SIZE, prv.getMldsaSeed().length);
		assertNotNull(prv.getEd25519Seed());
		assertEquals(SshMldsa44Ed25519PrivateKey.ED25519_SEED_SIZE, prv.getEd25519Seed().length);
		assertEquals(SshMldsa44Ed25519PrivateKey.COMPOSITE_PRIVATE_KEY_SIZE, prv.getCompositePrivateKey().length);

		assertNotNull(pub.getMldsaPublicKey());
		assertEquals(SshMldsa44Ed25519PublicKey.MLDSA44_PUBLIC_KEY_SIZE, pub.getMldsaPublicKey().length);
		assertNotNull(pub.getEd25519PublicKey());
		assertEquals(SshMldsa44Ed25519PublicKey.ED25519_PUBLIC_KEY_SIZE, pub.getEd25519PublicKey().length);
		assertEquals(SshMldsa44Ed25519PublicKey.COMPOSITE_PUBLIC_KEY_SIZE, pub.getCompositePublicKey().length);

		// Component ordering: composite is mldsa_pk || ed25519_pk
		byte[] compPk = pub.getCompositePublicKey();
		byte[] mldsaSlice = Arrays.copyOfRange(compPk, 0, 1312);
		byte[] edSlice = Arrays.copyOfRange(compPk, 1312, 1344);
		assertArrayEquals(pub.getMldsaPublicKey(), mldsaSlice);
		assertArrayEquals(pub.getEd25519PublicKey(), edSlice);

		// Check freshness: two generated key pairs must have different key material
		SshKeyPair pair2 = SshMldsa44Ed25519PrivateKey.generateKeyPair();
		assertFalse(prv.equals(pair2.getPrivateKey()));
		assertFalse(pub.equals(pair2.getPublicKey()));
	}

	@Test
	public void testMldsa44Ed25519PublicKeyEncodingAndDecoding() throws Exception {
		SshKeyPair pair = SshMldsa44Ed25519PrivateKey.generateKeyPair();
		SshMldsa44Ed25519PublicKey pub = (SshMldsa44Ed25519PublicKey) pair.getPublicKey();

		byte[] encoded = pub.getEncoded();
		assertNotNull(encoded);

		// Wire format validation: string "ssh-mldsa44-ed25519", string composite_public_key
		ByteArrayReader bar = new ByteArrayReader(encoded);
		String alg = bar.readString();
		assertEquals("ssh-mldsa44-ed25519", alg);
		byte[] rawComp = bar.readBinaryString();
		assertEquals(1344, rawComp.length);
		assertEquals(0, bar.available());
		bar.close();

		// Round-trip decoding via init
		SshMldsa44Ed25519PublicKey decoded = new SshMldsa44Ed25519PublicKey();
		decoded.init(encoded, 0, encoded.length);

		assertEquals(pub, decoded);
		assertEquals(pub.getFingerprint(), decoded.getFingerprint());
		assertArrayEquals(pub.getMldsaPublicKey(), decoded.getMldsaPublicKey());
		assertArrayEquals(pub.getEd25519PublicKey(), decoded.getEd25519PublicKey());
		assertArrayEquals(pub.getEncoded(), decoded.getEncoded());
	}

	@Test
	public void testMldsa44Ed25519SigningAndVerification() throws Exception {
		SshKeyPair pair = SshMldsa44Ed25519PrivateKey.generateKeyPair();
		SshMldsa44Ed25519PrivateKey prv = (SshMldsa44Ed25519PrivateKey) pair.getPrivateKey();
		SshMldsa44Ed25519PublicKey pub = (SshMldsa44Ed25519PublicKey) pair.getPublicKey();

		byte[][] testMessages = new byte[][] {
			new byte[0],
			"Test message for ML-DSA-44 and Ed25519 composite SSH signature".getBytes(StandardCharsets.UTF_8),
			new byte[16384]
		};
		new Random(42).nextBytes(testMessages[2]);

		for (byte[] msg : testMessages) {
			byte[] rawSig = prv.sign(msg);
			assertEquals(SshMldsa44Ed25519PublicKey.COMPOSITE_SIGNATURE_SIZE, rawSig.length);

			// Component ordering: byte[2420] mldsa_sig || byte[64] ed25519_sig
			// Verify raw signature directly
			assertTrue("Raw composite signature should verify", pub.verifySignature(rawSig, msg));

			// Verify wrapped SSH signature: string "ssh-mldsa44-ed25519", string composite_sig
			byte[] wrappedSig = CompositeSignatureUtils.wrapSignature(pub.getSigningAlgorithm(), rawSig);
			assertTrue("Wrapped SSH signature should verify", pub.verifySignature(wrappedSig, msg));

			// Verify via SshKeyPair.sign wrapper
			byte[] keyPairSig = pair.sign(pub, pub.getSigningAlgorithm(), msg);
			assertTrue("KeyPair signature should verify", pub.verifySignature(keyPairSig, msg));
		}
	}

	@Test
	public void testMldsa44Ed25519FailureOnMessageTampering() throws Exception {
		SshKeyPair pair = SshMldsa44Ed25519PrivateKey.generateKeyPair();
		byte[] msg = "Authentic original message".getBytes(StandardCharsets.UTF_8);
		byte[] sig = pair.getPrivateKey().sign(msg);

		byte[] tamperedMsg = "Tampered modified message".getBytes(StandardCharsets.UTF_8);
		assertFalse("Signature must fail to verify against modified message", pair.getPublicKey().verifySignature(sig, tamperedMsg));
	}

	@Test
	public void testMldsa44Ed25519FailureOnComponentSignatureModification() throws Exception {
		SshKeyPair pair = SshMldsa44Ed25519PrivateKey.generateKeyPair();
		byte[] msg = "Message to be signed".getBytes(StandardCharsets.UTF_8);
		byte[] sig = pair.getPrivateKey().sign(msg);

		// Tamper with ML-DSA-44 component (within first 2420 bytes)
		byte[] tamperedMldsa = Arrays.copyOf(sig, sig.length);
		tamperedMldsa[100] ^= 0x55;
		assertFalse("Modified ML-DSA component must fail verification", pair.getPublicKey().verifySignature(tamperedMldsa, msg));

		// Tamper with Ed25519 component (within last 64 bytes)
		byte[] tamperedEd = Arrays.copyOf(sig, sig.length);
		tamperedEd[2450] ^= 0x55;
		assertFalse("Modified Ed25519 component must fail verification", pair.getPublicKey().verifySignature(tamperedEd, msg));
	}

	@Test
	public void testMldsa44Ed25519FailureWithIncorrectPublicKey() throws Exception {
		SshKeyPair pair1 = SshMldsa44Ed25519PrivateKey.generateKeyPair();
		SshKeyPair pair2 = SshMldsa44Ed25519PrivateKey.generateKeyPair();

		byte[] msg = "Test message".getBytes(StandardCharsets.UTF_8);
		byte[] sig1 = pair1.getPrivateKey().sign(msg);

		assertFalse("Signature from key1 must not verify with key2", pair2.getPublicKey().verifySignature(sig1, msg));
	}

	@Test
	public void testMldsa44Ed25519RejectionOfMalformedKeyEncodings() throws Exception {
		SshKeyPair pair = SshMldsa44Ed25519PrivateKey.generateKeyPair();
		byte[] validEncoded = pair.getPublicKey().getEncoded();

		// 1. Truncated blob
		try {
			new SshMldsa44Ed25519PublicKey().init(Arrays.copyOf(validEncoded, 50));
			fail("Expected SshException on truncated blob");
		} catch (SshException expected) {
		}

		// 2. Trailing data in blob
		byte[] trailingBlob = new byte[validEncoded.length + 10];
		System.arraycopy(validEncoded, 0, trailingBlob, 0, validEncoded.length);
		try {
			new SshMldsa44Ed25519PublicKey().init(trailingBlob);
			fail("Expected SshException on blob with trailing garbage");
		} catch (SshException expected) {
		}

		// 3. Wrong algorithm name
		ByteArrayWriter baw = new ByteArrayWriter();
		baw.writeString("ssh-rsa");
		baw.writeBinaryString(((SshMldsa44Ed25519PublicKey) pair.getPublicKey()).getCompositePublicKey());
		byte[] wrongAlgBlob = baw.toByteArray();
		baw.close();
		try {
			new SshMldsa44Ed25519PublicKey().init(wrongAlgBlob);
			fail("Expected SshException on mismatched algorithm name");
		} catch (SshException expected) {
		}

		// 4. Truncated composite public key (1343 bytes instead of 1344)
		baw = new ByteArrayWriter();
		baw.writeString("ssh-mldsa44-ed25519");
		baw.writeBinaryString(new byte[1343]);
		byte[] truncatedCompKey = baw.toByteArray();
		baw.close();
		try {
			new SshMldsa44Ed25519PublicKey().init(truncatedCompKey);
			fail("Expected SshException on truncated composite key");
		} catch (SshException expected) {
		}

		// 5. Oversized composite public key (1345 bytes instead of 1344)
		baw = new ByteArrayWriter();
		baw.writeString("ssh-mldsa44-ed25519");
		baw.writeBinaryString(new byte[1345]);
		byte[] oversizedCompKey = baw.toByteArray();
		baw.close();
		try {
			new SshMldsa44Ed25519PublicKey().init(oversizedCompKey);
			fail("Expected SshException on oversized composite key");
		} catch (SshException expected) {
		}
	}

	@Test
	public void testMldsa44Ed25519RejectionOfMalformedSignatures() throws Exception {
		SshKeyPair pair = SshMldsa44Ed25519PrivateKey.generateKeyPair();
		SshMldsa44Ed25519PublicKey pub = (SshMldsa44Ed25519PublicKey) pair.getPublicKey();
		byte[] msg = "Some message".getBytes(StandardCharsets.UTF_8);

		// Null or empty
		assertFalse(pub.verifySignature(null, msg));
		assertFalse(pub.verifySignature(new byte[0], msg));

		// Truncated signature
		assertFalse(pub.verifySignature(new byte[2483], msg));

		// Oversized raw signature
		assertFalse(pub.verifySignature(new byte[2485], msg));

		// Wrapped signature with mismatched algorithm
		byte[] rawSig = pair.getPrivateKey().sign(msg);
		byte[] wrongWrapped = CompositeSignatureUtils.wrapSignature("ssh-ed25519", rawSig);
		assertFalse(pub.verifySignature(wrongWrapped, msg));

		// Wrapped signature with trailing data
		byte[] validWrapped = CompositeSignatureUtils.wrapSignature(pub.getSigningAlgorithm(), rawSig);
		byte[] trailingWrapped = Arrays.copyOf(validWrapped, validWrapped.length + 5);
		assertFalse(pub.verifySignature(trailingWrapped, msg));
	}

	private String loadTestVectorField(String resourcePath, String fieldName) throws Exception {
		try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
			if (in == null) {
				throw new IllegalStateException("Test vector resource not found: " + resourcePath);
			}
			String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			String marker = "\"" + fieldName + "\": \"";
			int start = content.indexOf(marker);
			if (start == -1) {
				throw new IllegalArgumentException("Field not found: " + fieldName);
			}
			start += marker.length();
			int end = content.indexOf("\"", start);
			return content.substring(start, end);
		}
	}

	@Test
	public void testMldsa44Ed25519IndependentTestVectorFromDraft() throws Exception {
		// Test vector from draft-ietf-lamps-pq-composite-sigs-19 Appendix E for id-MLDSA44-Ed25519-SHA512
		String mStr = "The quick brown fox jumps over the lazy dog.";
		byte[] M = mStr.getBytes(StandardCharsets.US_ASCII);

		String vectorPath = "/composite-test-vectors/id-MLDSA44-Ed25519-SHA512.json";
		String pkB64 = loadTestVectorField(vectorPath, "pk");
		byte[] pkBytes = Base64.getDecoder().decode(pkB64);
		assertEquals(1344, pkBytes.length);

		String skB64 = loadTestVectorField(vectorPath, "sk");
		byte[] skBytes = Base64.getDecoder().decode(skB64);
		assertEquals(64, skBytes.length);

		String sB64 = loadTestVectorField(vectorPath, "s");
		byte[] sBytes = Base64.getDecoder().decode(sB64);
		assertEquals(2484, sBytes.length);

		// 1. Verify that known signature verifies with known public key
		SshMldsa44Ed25519PublicKey pub = new SshMldsa44Ed25519PublicKey(pkBytes);
		assertTrue("LAMPS draft test vector signature must verify", pub.verifySignature(sBytes, M));

		// 2. Reconstitute private key from known seeds and verify public key matches
		SshMldsa44Ed25519PrivateKey prv = new SshMldsa44Ed25519PrivateKey(skBytes);
		SshMldsa44Ed25519PublicKey derivedPub = prv.getPublicKey();
		assertEquals("Derived public key must equal test vector public key", pub, derivedPub);
		assertArrayEquals("Derived composite pk bytes must match", pkBytes, derivedPub.getCompositePublicKey());

		// 3. Generate a new signature with the reconstituted private key and verify with the public key
		byte[] freshSig = prv.sign(M);
		assertTrue("Fresh signature generated from test vector private key must verify", pub.verifySignature(freshSig, M));
	}

	@Test
	public void testOpenSshMldsa44Ed25519PublicKeyDecoding() throws Exception {
		// Public key from openssh-portable testdata: mldsa44_ed25519_1.pub
		String openSshPubBlobB64 = 
			"AAAAE3NzaC1tbGRzYTQ0LWVkMjU1MTkAAAVA5vnf402KSS7+u01Wy0CDdPz5VPz6f5Zi" +
			"aMjw//F+fA/4KTsZuNZHhYpCEsN4LHoiOfqMKVurC9OwvGSttaW8rTYQXXeibA2/r+b4" +
			"aI61FRpX8fBq69l/nAfG6AHYpgTEczrEuOsfvTqqxvOCmTjcLQlUzGAmmEAKxC5hkoqT" +
			"hlYpPDy1mBpWKZaQhU0faGfRiuHnWDiCDnFTmRFyRmj+z60pAY6/8ruggW294qFdNrpM" +
			"tYYsiWXl6mObMdvqci0USq9d6JEoI7UxGZt+a5neh0v8KPVdzd3c8rMp7ZB5MorS7QQi" +
			"OS7qykYl/mFSkPmvLJBTvg7JywrdQls0EErRX1o9Unm3ocIOJ111qOaWTO3WE8S0otCp" +
			"L+WVVP5m/fNzFffGlD1cRdOKMQqu69pqLcSfp3u1p//yEt7bmtu6FEpohwCHXal16uCG" +
			"a6l+lLuhpgrPREGVnsuR6/btT4+3AFKP4JlxEXBofkmxIcG/3oQGyI2yY6QJTkTFCbfm" +
			"aFNH7/Z8qqrmz3npdEWwSCTK8ZiZKosqGt5DmHLg73ntSS8gmoqbSCcTajwdTATn08xe" +
			"dyKS5yQEU2t8is4PM7xXMZeXWhmzBHouPsXY5Pe4VG+x9X9ScpczdTEZOHDBxiJsUk+M" +
			"N/8hCg/VHUF7v1Z3BmTpvBJQl4Hl7rk/6N3AtYL9MQQ7q0tjXabpIYhQpe2USKZcqTkE" +
			"bvSgegQmJSBXZqdHJCdJehRp6ZEGPIO0uDJJef1cUbNrB6oVcIz7Ev0VmGIUjZslXpCJ" +
			"ylZ3QQNIZltAmprqlGJAogsVWErSCVaFKVAjWSZM3BtbNcwx/RdVOhsNr1Hx6niLQ8ZL" +
			"k5dVqF8ZNeRCCh5xEkcoWGCQgD00BpJo9eP2GRTWS1ur7gcc5wmBlf2CATc+Tqms8+Ck" +
			"SNqQMqZ4rFYbFlK5C9cbTEJ/l+GfcaXFQ6u8KFO0pJW/XC7U4eRVJxb7EA823Bnk8Smv" +
			"cWQakfguY5rZL2o9LbfCKV6tNFLMsSwWKtlg64k+zeVsbnaapR7jmP/mGYJxI0qXlhW1" +
			"C0XLZwhvM3766bkjfdEqLdyu27IeyMNgXaf/0xp9UZXQwGg9Xz+c8a+EDKt5HpL8IvRl" +
			"EPFkw1zipicglT3mcsmoXWLa8UZh7DQbgqCtlsrDwz9XLkqhlwsj6pF00vS8kTDqtdcd" +
			"9g/3kt8d9chdGpWiA/mTx+gYS9LFG5wKW7PVYZyj1L0HN2X+Rce7O+FrJshMs4GGmezR" +
			"b6G9ZY9nusdH1GMn/9S7a33U1LiBp4XMM/vhj1aYWor3BBCstaJnED6A5aSDBg5nixTe" +
			"AhvyKkYlgQgRMhvwthssmjSGt1lnOszuQjeBLSKdlcXPmr0apHGtSCYai8PL7zdEnk4D" +
			"K6Ul6jL+hAWs5F0J2R32j1OCCUlbS3xziOpWKGxXPrEJ0+g0aF15cKfvPzJ8zchPPC5Q" +
			"cjFmSgXfh/3xNzN0ktGAfoU1ko5asfAJwqd8dN6KWoLh68VDnztQg2nGQtXlrRZK/emN" +
			"aeIRJbF+6pVjzCwFnFJbgXPqG1Yr0KwqOP0/guYO11QTszlNJi8WidtPa3NhB6JcXvpZ" +
			"UpWBYk1khEnX3KRGW5GVflOgWq4PYHG7GFUp26iaKuoUg17qx7NMSJzq/UoLOYIu3oKM" +
			"zuLZqc2f8PEy7ZXaeRrg07R1EfSL4dfpZ2eKy4ldxHPfqhYKExEC8UfMLgR1YBzYpyB5" +
			"OyRDMoRgxOu322bmeX6gYN3u5IrC1DIIuFSbih8v0+4KSYIfJ+TRjI18cCBC";
		byte[] blob = Base64.getDecoder().decode(openSshPubBlobB64);

		SshMldsa44Ed25519PublicKey pub = new SshMldsa44Ed25519PublicKey();
		pub.init(blob, 0, blob.length);

		assertEquals("ssh-mldsa44-ed25519", pub.getAlgorithm());
		assertEquals(1312, pub.getMldsaPublicKey().length);
		assertEquals(32, pub.getEd25519PublicKey().length);
		assertArrayEquals(blob, pub.getEncoded());
	}

	// =========================================================================
	// ssh-mldsa87-p384 tests
	// =========================================================================

	@Test
	public void testMldsa87P384KeyPairGeneration() throws Exception {
		SshKeyPair pair = SshKeyPairGenerator.generateKeyPair(SshKeyPairGenerator.SSH_MLDSA87_P384);
		assertNotNull(pair);
		assertTrue(pair.getPrivateKey() instanceof SshMldsa87P384PrivateKey);
		assertTrue(pair.getPublicKey() instanceof SshMldsa87P384PublicKey);

		SshMldsa87P384PrivateKey prv = (SshMldsa87P384PrivateKey) pair.getPrivateKey();
		SshMldsa87P384PublicKey pub = (SshMldsa87P384PublicKey) pair.getPublicKey();

		assertEquals("ssh-mldsa87-p384", prv.getAlgorithm());
		assertEquals("ssh-mldsa87-p384", pub.getAlgorithm());
		assertEquals("ssh-mldsa87-p384", pub.getSigningAlgorithm());
		assertEquals("ssh-mldsa87-p384", pub.getEncodingAlgorithm());
		assertEquals(SecurityLevel.PARANOID, pub.getSecurityLevel());

		assertNotNull(prv.getMldsaSeed());
		assertEquals(SshMldsa87P384PrivateKey.MLDSA87_SEED_SIZE, prv.getMldsaSeed().length);
		assertNotNull(prv.getD());
		assertTrue(prv.getD().bitLength() <= 384);
		assertNotNull(prv.getCompositePrivateKey());

		assertNotNull(pub.getMldsaPublicKey());
		assertEquals(SshMldsa87P384PublicKey.MLDSA87_PUBLIC_KEY_SIZE, pub.getMldsaPublicKey().length);
		assertNotNull(pub.getQ());
		assertEquals(SshMldsa87P384PublicKey.ECDSA_P384_POINT_SIZE, pub.getQ().length);
		assertEquals(0x04, pub.getQ()[0]); // uncompressed point format

		// Total composite public key: 2592 bytes mldsa_pk || string Q (4 + 97 = 101 bytes) = 2693 bytes
		assertEquals(SshMldsa87P384PublicKey.COMPOSITE_PUBLIC_KEY_SIZE, pub.getCompositePublicKey().length);

		// Check freshness
		SshKeyPair pair2 = SshMldsa87P384PrivateKey.generateKeyPair();
		assertFalse(prv.equals(pair2.getPrivateKey()));
		assertFalse(pub.equals(pair2.getPublicKey()));
	}

	@Test
	public void testMldsa87P384PublicKeyEncodingAndDecoding() throws Exception {
		SshKeyPair pair = SshMldsa87P384PrivateKey.generateKeyPair();
		SshMldsa87P384PublicKey pub = (SshMldsa87P384PublicKey) pair.getPublicKey();

		byte[] encoded = pub.getEncoded();
		assertNotNull(encoded);

		// Wire format validation: string "ssh-mldsa87-p384", string composite_public_key
		ByteArrayReader bar = new ByteArrayReader(encoded);
		String alg = bar.readString();
		assertEquals("ssh-mldsa87-p384", alg);
		byte[] rawComp = bar.readBinaryString();
		assertEquals(2693, rawComp.length);
		assertEquals(0, bar.available());
		bar.close();

		// Round-trip decoding via init
		SshMldsa87P384PublicKey decoded = new SshMldsa87P384PublicKey();
		decoded.init(encoded, 0, encoded.length);

		assertEquals(pub, decoded);
		assertEquals(pub.getFingerprint(), decoded.getFingerprint());
		assertArrayEquals(pub.getMldsaPublicKey(), decoded.getMldsaPublicKey());
		assertArrayEquals(pub.getQ(), decoded.getQ());
		assertArrayEquals(pub.getEncoded(), decoded.getEncoded());
	}

	@Test
	public void testMldsa87P384SigningAndVerification() throws Exception {
		SshKeyPair pair = SshMldsa87P384PrivateKey.generateKeyPair();
		SshMldsa87P384PrivateKey prv = (SshMldsa87P384PrivateKey) pair.getPrivateKey();
		SshMldsa87P384PublicKey pub = (SshMldsa87P384PublicKey) pair.getPublicKey();

		byte[][] testMessages = new byte[][] {
			new byte[0],
			"Test message for ML-DSA-87 and ECDSA P-384 composite SSH signature".getBytes(StandardCharsets.UTF_8),
			new byte[8192]
		};
		new Random(99).nextBytes(testMessages[2]);

		for (byte[] msg : testMessages) {
			byte[] rawSig = prv.sign(msg);
			assertNotNull(rawSig);

			// Structure: byte[4627] mldsa_sig || mpint r || mpint s
			assertTrue("Signature length must be at least 4627 + 8", rawSig.length >= 4627 + 8);
			ByteArrayReader rReader = new ByteArrayReader(rawSig, 4627, rawSig.length - 4627);
			BigInteger r = rReader.readBigInteger();
			BigInteger s = rReader.readBigInteger();
			assertEquals(0, rReader.available());
			rReader.close();

			assertTrue(r.signum() > 0);
			assertTrue(s.signum() > 0);

			// Verify raw signature
			assertTrue("Raw composite signature should verify", pub.verifySignature(rawSig, msg));

			// Verify wrapped SSH signature: string "ssh-mldsa87-p384", string composite_sig
			byte[] wrappedSig = CompositeSignatureUtils.wrapSignature(pub.getSigningAlgorithm(), rawSig);
			assertTrue("Wrapped SSH signature should verify", pub.verifySignature(wrappedSig, msg));

			// Verify via SshKeyPair.sign
			byte[] keyPairSig = pair.sign(pub, pub.getSigningAlgorithm(), msg);
			assertTrue("KeyPair signature should verify", pub.verifySignature(keyPairSig, msg));
		}
	}

	@Test
	public void testMldsa87P384FailureOnMessageTampering() throws Exception {
		SshKeyPair pair = SshMldsa87P384PrivateKey.generateKeyPair();
		byte[] msg = "Authentic original message for P-384".getBytes(StandardCharsets.UTF_8);
		byte[] sig = pair.getPrivateKey().sign(msg);

		byte[] tamperedMsg = "Modified message for P-384".getBytes(StandardCharsets.UTF_8);
		assertFalse("Signature must fail to verify against modified message", pair.getPublicKey().verifySignature(sig, tamperedMsg));
	}

	@Test
	public void testMldsa87P384FailureOnComponentSignatureModification() throws Exception {
		SshKeyPair pair = SshMldsa87P384PrivateKey.generateKeyPair();
		byte[] msg = "Message to be signed with P-384".getBytes(StandardCharsets.UTF_8);
		byte[] sig = pair.getPrivateKey().sign(msg);

		// Tamper with ML-DSA-87 component (first 4627 bytes)
		byte[] tamperedMldsa = Arrays.copyOf(sig, sig.length);
		tamperedMldsa[500] ^= 0x55;
		assertFalse("Modified ML-DSA-87 component must fail verification", pair.getPublicKey().verifySignature(tamperedMldsa, msg));

		// Tamper with ECDSA component (last byte)
		byte[] tamperedEcdsa = Arrays.copyOf(sig, sig.length);
		tamperedEcdsa[tamperedEcdsa.length - 1] ^= 0x55;
		assertFalse("Modified ECDSA component must fail verification", pair.getPublicKey().verifySignature(tamperedEcdsa, msg));
	}

	@Test
	public void testMldsa87P384FailureWithIncorrectPublicKey() throws Exception {
		SshKeyPair pair1 = SshMldsa87P384PrivateKey.generateKeyPair();
		SshKeyPair pair2 = SshMldsa87P384PrivateKey.generateKeyPair();

		byte[] msg = "Message".getBytes(StandardCharsets.UTF_8);
		byte[] sig1 = pair1.getPrivateKey().sign(msg);

		assertFalse("Signature from key1 must not verify with key2", pair2.getPublicKey().verifySignature(sig1, msg));
	}

	@Test
	public void testMldsa87P384RejectionOfMalformedKeyEncodings() throws Exception {
		SshKeyPair pair = SshMldsa87P384PrivateKey.generateKeyPair();
		byte[] validEncoded = pair.getPublicKey().getEncoded();

		// Truncated blob
		try {
			new SshMldsa87P384PublicKey().init(Arrays.copyOf(validEncoded, 100));
			fail("Expected SshException on truncated blob");
		} catch (SshException expected) {
		}

		// Trailing data in blob
		byte[] trailingBlob = new byte[validEncoded.length + 8];
		System.arraycopy(validEncoded, 0, trailingBlob, 0, validEncoded.length);
		try {
			new SshMldsa87P384PublicKey().init(trailingBlob);
			fail("Expected SshException on trailing data in key blob");
		} catch (SshException expected) {
		}

		// Wrong algorithm name
		ByteArrayWriter baw = new ByteArrayWriter();
		baw.writeString("ecdsa-sha2-nistp384");
		baw.writeBinaryString(((SshMldsa87P384PublicKey) pair.getPublicKey()).getCompositePublicKey());
		byte[] wrongAlgBlob = baw.toByteArray();
		baw.close();
		try {
			new SshMldsa87P384PublicKey().init(wrongAlgBlob);
			fail("Expected SshException on mismatched algorithm name");
		} catch (SshException expected) {
		}

		// Invalid point format in Q (not starting with 0x04)
		byte[] compPk = ((SshMldsa87P384PublicKey) pair.getPublicKey()).getCompositePublicKey();
		byte[] badPointCompPk = Arrays.copyOf(compPk, compPk.length);
		badPointCompPk[2592 + 4] = 0x02; // compressed format not allowed by spec Section 4.1.2.1
		baw = new ByteArrayWriter();
		baw.writeString("ssh-mldsa87-p384");
		baw.writeBinaryString(badPointCompPk);
		byte[] badPointBlob = baw.toByteArray();
		baw.close();
		try {
			new SshMldsa87P384PublicKey().init(badPointBlob);
			fail("Expected SshException on invalid uncompressed point format");
		} catch (SshException expected) {
		}
	}

	@Test
	public void testMldsa87P384RejectionOfMalformedSignatures() throws Exception {
		SshKeyPair pair = SshMldsa87P384PrivateKey.generateKeyPair();
		SshMldsa87P384PublicKey pub = (SshMldsa87P384PublicKey) pair.getPublicKey();
		byte[] msg = "Some message".getBytes(StandardCharsets.UTF_8);

		assertFalse(pub.verifySignature(null, msg));
		assertFalse(pub.verifySignature(new byte[0], msg));
		assertFalse(pub.verifySignature(new byte[100], msg));

		// Wrapped signature with wrong algorithm
		byte[] rawSig = pair.getPrivateKey().sign(msg);
		byte[] wrongWrapped = CompositeSignatureUtils.wrapSignature("ssh-mldsa44-ed25519", rawSig);
		assertFalse(pub.verifySignature(wrongWrapped, msg));

		// Wrapped signature with trailing garbage
		byte[] validWrapped = CompositeSignatureUtils.wrapSignature(pub.getSigningAlgorithm(), rawSig);
		byte[] trailingWrapped = Arrays.copyOf(validWrapped, validWrapped.length + 5);
		assertFalse(pub.verifySignature(trailingWrapped, msg));
	}

	@Test
	public void testMldsa87P384AlternatePaddedSignatureLengthVerification() throws Exception {
		SshKeyPair pair = SshMldsa87P384PrivateKey.generateKeyPair();
		SshMldsa87P384PublicKey pub = (SshMldsa87P384PublicKey) pair.getPublicKey();
		byte[] msg = "Testing draft text alternate 4896 signature size".getBytes(StandardCharsets.UTF_8);

		byte[] rawSig = pair.getPrivateKey().sign(msg);
		// rawSig is: byte[4627] mldsaSig || mpint r || mpint s
		byte[] mldsaSig = Arrays.copyOfRange(rawSig, 0, 4627);
		byte[] mpints = Arrays.copyOfRange(rawSig, 4627, rawSig.length);

		// Construct padded signature with 4896 bytes of ML-DSA (padded with zeros) + mpint r + mpint s
		byte[] paddedMldsa = new byte[4896];
		System.arraycopy(mldsaSig, 0, paddedMldsa, 0, 4627);

		byte[] altSig = new byte[4896 + mpints.length];
		System.arraycopy(paddedMldsa, 0, altSig, 0, 4896);
		System.arraycopy(mpints, 0, altSig, 4896, mpints.length);

		// Our verifier supports both 4627-byte and 4896-byte ML-DSA component representations
		assertTrue("Verifier should accept alternate 4896-byte ML-DSA signature framing", pub.verifySignature(altSig, msg));
	}

	@Test
	public void testMldsa87P384IndependentTestVectorFromDraft() throws Exception {
		// Test vector from draft-ietf-lamps-pq-composite-sigs-19 Appendix E for id-MLDSA87-ECDSA-P384-SHA512
		String mStr = "The quick brown fox jumps over the lazy dog.";
		byte[] M = mStr.getBytes(StandardCharsets.US_ASCII);

		String vectorPath = "/composite-test-vectors/id-MLDSA87-ECDSA-P384-SHA512.json";
		String pkB64 = loadTestVectorField(vectorPath, "pk");
		byte[] pkBytes = Base64.getDecoder().decode(pkB64);
		assertEquals(2689, pkBytes.length);

		byte[] mldsaPk = Arrays.copyOfRange(pkBytes, 0, 2592);
		byte[] ecQ = Arrays.copyOfRange(pkBytes, 2592, 2689);
		assertEquals(97, ecQ.length);

		String skB64 = loadTestVectorField(vectorPath, "sk");
		byte[] skBytes = Base64.getDecoder().decode(skB64);
		assertEquals(96, skBytes.length);
		byte[] mldsaSeed = Arrays.copyOfRange(skBytes, 0, 32);

		// Extract private key d from SEC1 ECPrivateKey DER in skBytes
		byte[] ecSec1 = Arrays.copyOfRange(skBytes, 32, skBytes.length);
		ASN1InputStream ecAsn = new ASN1InputStream(ecSec1);
		ASN1Sequence ecSeq = (ASN1Sequence) ecAsn.readObject();
		ecAsn.close();
		byte[] dBytes = ASN1OctetString.getInstance(ecSeq.getObjectAt(1)).getOctets();
		BigInteger d = new BigInteger(1, dBytes);

		String sB64 = loadTestVectorField(vectorPath, "s");
		byte[] sBytes = Base64.getDecoder().decode(sB64);
		assertEquals(4730, sBytes.length);

		byte[] mldsaSig = Arrays.copyOfRange(sBytes, 0, 4627);
		byte[] ecDer = Arrays.copyOfRange(sBytes, 4627, sBytes.length);

		// Extract r and s from DER
		ASN1InputStream asnIn = new ASN1InputStream(ecDer);
		ASN1Sequence seq = (ASN1Sequence) asnIn.readObject();
		asnIn.close();
		BigInteger r = ((ASN1Integer) seq.getObjectAt(0)).getValue();
		BigInteger s = ((ASN1Integer) seq.getObjectAt(1)).getValue();

		// Convert to SSH composite format: mldsa_sig || mpint r || mpint s
		ByteArrayWriter sigBaw = new ByteArrayWriter();
		sigBaw.write(mldsaSig);
		sigBaw.writeBigInteger(r);
		sigBaw.writeBigInteger(s);
		byte[] sshCompositeSig = sigBaw.toByteArray();
		sigBaw.close();

		// 1. Verify with SshMldsa87P384PublicKey
		SshMldsa87P384PublicKey pub = new SshMldsa87P384PublicKey(mldsaPk, ecQ);
		assertTrue("LAMPS draft test vector signature must verify in SSH composite format", pub.verifySignature(sshCompositeSig, M));

		// 2. Reconstitute private key from seeds and test public key derivation
		SshMldsa87P384PrivateKey prv = new SshMldsa87P384PrivateKey(mldsaSeed, d);
		SshMldsa87P384PublicKey derivedPub = prv.getPublicKey();
		assertEquals("Derived public key must equal test vector public key", pub, derivedPub);
		assertArrayEquals("Derived ML-DSA-87 pk must match", mldsaPk, derivedPub.getMldsaPublicKey());
		assertArrayEquals("Derived Q must match", ecQ, derivedPub.getQ());

		// 3. Generate a fresh signature with reconstituted private key and verify
		byte[] freshSig = prv.sign(M);
		assertTrue("Fresh signature from reconstituted private key must verify", pub.verifySignature(freshSig, M));
	}
}
