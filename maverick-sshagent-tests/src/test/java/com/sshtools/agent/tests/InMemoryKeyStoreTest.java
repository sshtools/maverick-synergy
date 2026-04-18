package com.sshtools.agent.tests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sshtools.agent.InMemoryKeyStore;
import com.sshtools.agent.KeyConstraints;
import com.sshtools.agent.KeyStore;
import com.sshtools.agent.KeyStoreListener;
import com.sshtools.agent.exceptions.KeyTimeoutException;
import com.sshtools.common.publickey.SshKeyPairGenerator;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;

/**
 * Unit tests for {@link InMemoryKeyStore}.
 */
public class InMemoryKeyStoreTest {

    private static SshKeyPair ed25519Pair;
    private static SshKeyPair rsaPair;

    private InMemoryKeyStore store;

    @BeforeAll
    static void generateKeys() throws IOException, SshException {
        JCEComponentManager.getDefaultInstance();
        ed25519Pair = SshKeyPairGenerator.generateKeyPair(SshKeyPairGenerator.ED25519);
        rsaPair     = SshKeyPairGenerator.generateKeyPair(SshKeyPairGenerator.SSH2_RSA, 2048);
    }

    @BeforeEach
    void setUp() {
        store = new InMemoryKeyStore();
    }

    // ---------------------------------------------------------------
    // addKey / size / getPublicKeys
    // ---------------------------------------------------------------

    @Test
    void addKey_incrementsSize() throws IOException {
        assertEquals(0, store.size());
        store.addKey(ed25519Pair, "ed25519-key", new KeyConstraints());
        assertEquals(1, store.size());
    }

    @Test
    void addKey_sameKeyTwice_returnsFalseSecondTime() throws IOException {
        assertTrue(store.addKey(ed25519Pair, "key-1", new KeyConstraints()));
        assertFalse(store.addKey(ed25519Pair, "key-1-dup", new KeyConstraints()),
                "Adding the same key a second time should return false");
        assertEquals(1, store.size(), "Duplicate add must not increase the store size");
    }

    @Test
    void addKey_twoDistinctKeys_sizeIsTwo() throws IOException {
        store.addKey(ed25519Pair, "ed25519", new KeyConstraints());
        store.addKey(rsaPair,     "rsa",     new KeyConstraints());
        assertEquals(2, store.size());
    }

    @Test
    void getPublicKeys_returnsAddedPublicKey() throws IOException {
        store.addKey(ed25519Pair, "ed25519", new KeyConstraints());
        Map<SshPublicKey, String> keys = store.getPublicKeys();
        assertEquals(1, keys.size());
        assertTrue(keys.containsKey(ed25519Pair.getPublicKey()),
                "getPublicKeys() should contain the public key that was added");
    }

    @Test
    void getPublicKeys_returnsCorrectDescription() throws IOException {
        store.addKey(ed25519Pair, "my-test-key", new KeyConstraints());
        Map<SshPublicKey, String> keys = store.getPublicKeys();
        assertEquals("my-test-key", keys.get(ed25519Pair.getPublicKey()));
    }

    @Test
    void getPublicKeys_returnsUnmodifiableMap() throws IOException {
        store.addKey(ed25519Pair, "ed25519", new KeyConstraints());
        Map<SshPublicKey, String> keys = store.getPublicKeys();
        assertThrows(UnsupportedOperationException.class,
                () -> keys.put(rsaPair.getPublicKey(), "illegal"));
    }

    // ---------------------------------------------------------------
    // deleteKey
    // ---------------------------------------------------------------

    @Test
    void deleteKey_removesKeyFromStore() throws IOException {
        store.addKey(ed25519Pair, "ed25519", new KeyConstraints());
        assertTrue(store.deleteKey(ed25519Pair.getPublicKey()));
        assertEquals(0, store.size());
    }

    @Test
    void deleteKey_keyNotPresent_returnsFalse() throws IOException {
        assertFalse(store.deleteKey(ed25519Pair.getPublicKey()));
    }

    @Test
    void deleteKey_removesOnlyTargetKey() throws IOException {
        store.addKey(ed25519Pair, "ed25519", new KeyConstraints());
        store.addKey(rsaPair,     "rsa",     new KeyConstraints());
        store.deleteKey(ed25519Pair.getPublicKey());
        assertEquals(1, store.size());
        assertTrue(store.getPublicKeys().containsKey(rsaPair.getPublicKey()),
                "RSA key should still be in the store after removing Ed25519 key");
    }

    // ---------------------------------------------------------------
    // deleteAllKeys
    // ---------------------------------------------------------------

    @Test
    void deleteAllKeys_emptiesStore() throws IOException {
        store.addKey(ed25519Pair, "ed25519", new KeyConstraints());
        store.addKey(rsaPair,     "rsa",     new KeyConstraints());
        assertTrue(store.deleteAllKeys());
        assertEquals(0, store.size());
        assertTrue(store.getPublicKeys().isEmpty());
    }

    // ---------------------------------------------------------------
    // performHashAndSign
    // ---------------------------------------------------------------

    @Test
    void performHashAndSign_returnsNonNullSignature()
            throws IOException, SshException, KeyTimeoutException {
        store.addKey(ed25519Pair, "ed25519", new KeyConstraints());
        byte[] data = "sign-me".getBytes("UTF-8");
        byte[] sig = store.performHashAndSign(
                ed25519Pair.getPublicKey(), Collections.emptyList(), data, 0);
        assertNotNull(sig);
        assertTrue(sig.length > 0);
    }

    @Test
    void performHashAndSign_signatureIsVerifiable()
            throws IOException, SshException, KeyTimeoutException {
        store.addKey(ed25519Pair, "ed25519", new KeyConstraints());
        byte[] data = "hello-agent".getBytes("UTF-8");
        byte[] rawSig = store.performHashAndSign(
                ed25519Pair.getPublicKey(), Collections.emptyList(), data, 0);

        // Wrap into SSH signature blob: string(algorithm) + binary(sig)
        byte[] sigBlob = wrapSignature("ssh-ed25519", rawSig);
        assertTrue(ed25519Pair.getPublicKey().verifySignature(sigBlob, data),
                "Signature produced by the agent must be verifiable with the public key");
    }

    @Test
    void performHashAndSign_unknownPublicKey_throwsSshException() {
        byte[] data = "data".getBytes();
        assertThrows(SshException.class, () ->
            store.performHashAndSign(
                    ed25519Pair.getPublicKey(), Collections.emptyList(), data, 0));
    }

    // ---------------------------------------------------------------
    // KeyStoreListener
    // ---------------------------------------------------------------

    @Test
    void addKeyStoreListener_onAddKey_isCalled() throws IOException {
        AtomicInteger callCount = new AtomicInteger(0);
        store.addKeyStoreListener(new KeyStoreListener() {
            @Override public void onAddKey(KeyStore keyStore) { callCount.incrementAndGet(); }
            @Override public void onDeleteKey(KeyStore keyStore) {}
            @Override public void onDeleteAllKeys(KeyStore keyStore) {}
            @Override public void onKeyOperation(KeyStore keyStore, String operation) {}
            @Override public void onLock(KeyStore keyStore) {}
            @Override public void onUnlock(KeyStore keyStore) {}
        });

        store.addKey(ed25519Pair, "ed25519", new KeyConstraints());
        assertEquals(1, callCount.get(), "Listener.onAddKey should have been called once");
    }

    @Test
    void addKeyStoreListener_onDeleteAllKeys_isCalled() throws IOException {
        AtomicInteger callCount = new AtomicInteger(0);
        store.addKeyStoreListener(new KeyStoreListener() {
            @Override public void onAddKey(KeyStore keyStore) {}
            @Override public void onDeleteKey(KeyStore keyStore) {}
            @Override public void onDeleteAllKeys(KeyStore keyStore) { callCount.incrementAndGet(); }
            @Override public void onKeyOperation(KeyStore keyStore, String operation) {}
            @Override public void onLock(KeyStore keyStore) {}
            @Override public void onUnlock(KeyStore keyStore) {}
        });

        store.addKey(ed25519Pair, "ed25519", new KeyConstraints());
        store.deleteAllKeys();
        assertEquals(1, callCount.get(), "Listener.onDeleteAllKeys should have been called once");
    }

    // ---------------------------------------------------------------
    // helper
    // ---------------------------------------------------------------

    private static byte[] wrapSignature(String algorithm, byte[] sig) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream out = new java.io.DataOutputStream(baos);
        byte[] algBytes = algorithm.getBytes("UTF-8");
        out.writeInt(algBytes.length);
        out.write(algBytes);
        out.writeInt(sig.length);
        out.write(sig);
        return baos.toByteArray();
    }
}
