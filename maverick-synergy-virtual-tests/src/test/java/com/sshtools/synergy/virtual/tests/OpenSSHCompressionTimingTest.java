package com.sshtools.synergy.virtual.tests;

/*-
 * #%L
 * Virtual Connection Tests
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
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sshtools.client.PasswordAuthenticator;
import com.sshtools.client.SshClientContext;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.compression.SshCompressionFactory;
import com.sshtools.common.zlib.OpenSSHZLibCompression;
import com.sshtools.server.ServerConnectionStateListener;
import com.sshtools.server.SshServerContext;
import com.sshtools.synergy.nio.ConnectRequestFuture;
import com.sshtools.synergy.ssh.SshContext;

/**
 * Regression tests for the {@code zlib@openssh.com} delayed-start fix.
 *
 * <p>RFC / OpenSSH extension requires that {@code zlib@openssh.com} compression
 * is activated <em>only after</em> {@code SSH_MSG_USERAUTH_SUCCESS} is exchanged,
 * not immediately after {@code SSH_MSG_NEWKEYS}.  Before the fix the server
 * activated compression at key-exchange time, which breaks interoperability with
 * standards-compliant clients.</p>
 *
 * <p>Two virtual (in-memory) tests are provided:</p>
 * <ol>
 *   <li>{@link #testZlibOpenSSHConnectionSucceeds()} — end-to-end connectivity
 *       check: both sides negotiate {@code zlib@openssh.com}, the connection
 *       completes authentication, and the client can exchange data.</li>
 *   <li>{@link #testServerCompressionNotActiveBeforeAuthSuccess()} — timing
 *       check: uses a {@link TrackingCompression} wrapper to assert that the
 *       server's outgoing compressor is not exercised until after
 *       {@code SSH_MSG_USERAUTH_SUCCESS} has been sent.</li>
 * </ol>
 */
public class OpenSSHCompressionTimingTest extends AbstractVirtualConnectionTests {

    // -------------------------------------------------------------------------
    // Tracking state shared between the state listener and the compressor
    // -------------------------------------------------------------------------

    /**
     * Set to {@code true} by the per-connection {@link ServerConnectionStateListener}
     * when {@code authenticationComplete()} is called — i.e. immediately before
     * the server calls {@code enablePostAuthCompression()} in its
     * {@code messageSent()} callback for {@code SSH_MSG_USERAUTH_SUCCESS}.
     */
    private final AtomicBoolean authCompleteNotified = new AtomicBoolean(false);

    /**
     * Set to {@code true} by {@link TrackingCompression#compress} if the
     * compressor is called before {@link #authCompleteNotified} becomes {@code true}.
     */
    private final AtomicBoolean compressionUsedBeforeAuth = new AtomicBoolean(false);

    // -------------------------------------------------------------------------
    // JUnit lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void setUp() throws Exception {
        authCompleteNotified.set(false);
        compressionUsedBeforeAuth.set(false);
        super.setUp();
    }

    // -------------------------------------------------------------------------
    // Configuration hooks
    // -------------------------------------------------------------------------

    @Override
    protected void configureClientContext(SshClientContext ctx)
            throws IOException, SshException {
        ctx.setUsername("admin");
        ctx.addAuthenticator(PasswordAuthenticator.forPassword("admin"));
        ctx.setPreferredCompressionCS(SshContext.COMPRESSION_ZLIB_OPENSSH);
        ctx.setPreferredCompressionSC(SshContext.COMPRESSION_ZLIB_OPENSSH);
    }

    /**
     * Configures the server context to negotiate {@code zlib@openssh.com},
     * installs a {@link TrackingCompressionFactory} for the SC (server-to-client)
     * direction, and registers a {@link ServerConnectionStateListener} that sets
     * {@link #authCompleteNotified} in the same call stack as
     * {@code enablePostAuthCompression()} — strictly before the first compressed
     * message leaves the server.
     */
    @Override
    protected void configureServerContext(SshServerContext ctx)
            throws IOException, SshException {
        ctx.setPreferredCompressionCS(SshContext.COMPRESSION_ZLIB_OPENSSH);
        ctx.setPreferredCompressionSC(SshContext.COMPRESSION_ZLIB_OPENSSH);

        // Replace the default zlib@openssh.com SC factory with our tracking one.
        // (CS direction stays standard — we only care about server outgoing.)
        ctx.supportedCompressionsSC().remove(SshContext.COMPRESSION_ZLIB_OPENSSH);
        ctx.supportedCompressionsSC().add(new TrackingCompressionFactory());

        // Register a state listener: authenticationComplete() fires from the
        // messageSent() callback for SSH_MSG_USERAUTH_SUCCESS, *before*
        // enablePostAuthCompression() is called.  Setting authCompleteNotified
        // here guarantees that any subsequent compress() call sees it as true.
        ctx.addStateListener(new ServerConnectionStateListener() {
            @Override
            public void authenticationComplete(SshConnection con) {
                authCompleteNotified.set(true);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    /**
     * Verifies that a virtual connection with {@code zlib@openssh.com} on both
     * sides completes authentication successfully.
     *
     * <p>Before the fix, the server would activate compression immediately after
     * key exchange, causing a decompression failure on the client during the auth
     * exchange (when talking to a correctly-implemented client), and the
     * connection would fail.  After the fix, both sides defer activation until
     * after {@code SSH_MSG_USERAUTH_SUCCESS}, and the connection succeeds.</p>
     */
    public void testZlibOpenSSHConnectionSucceeds() throws Exception {
        ConnectRequestFuture future = connectVirtual();
        assertTrue("zlib@openssh.com virtual connection should succeed",
                future.isSuccess());
    }

    /**
     * Verifies that the server-side outgoing compressor is not called before
     * {@code SSH_MSG_USERAUTH_SUCCESS} has been sent.
     *
     * <p>A {@link TrackingCompression} wrapper is installed as the server's
     * SC (Server→Client) compression.  The {@link ServerConnectionStateListener}
     * sets {@link #authCompleteNotified} in the same {@code messageSent()} call
     * stack, strictly before {@code enablePostAuthCompression()} activates the
     * compressor.  If {@code compress()} is invoked before
     * {@link #authCompleteNotified} is {@code true}, {@link #compressionUsedBeforeAuth}
     * is set to {@code true} and the assertion fails.</p>
     *
     * <p>Without the fix, the server activates outgoing compression right after
     * {@code SSH_MSG_NEWKEYS} and then compresses the auth-service accept and
     * userauth-related messages — all of which arrive before the auth-complete
     * notification, causing the assertion to fail.</p>
     */
    public void testServerCompressionNotActiveBeforeAuthSuccess() throws Exception {
        ConnectRequestFuture future = connectVirtual();
        assertTrue("zlib@openssh.com virtual connection should succeed",
                future.isSuccess());
        assertFalse(
                "Server must not use the zlib@openssh.com compressor before " +
                "SSH_MSG_USERAUTH_SUCCESS is sent",
                compressionUsedBeforeAuth.get());
    }

    // -------------------------------------------------------------------------
    // Inner helpers
    // -------------------------------------------------------------------------

    /**
     * A {@link SshCompression} wrapper around {@link OpenSSHZLibCompression}
     * that records the first call to {@link #compress} relative to the
     * authentication-complete notification.
     */
    private class TrackingCompression extends OpenSSHZLibCompression {

        @Override
        public byte[] compress(byte[] buf, int start, int len) throws IOException {
            if (!authCompleteNotified.get()) {
                compressionUsedBeforeAuth.set(true);
            }
            return super.compress(buf, start, len);
        }
    }

    /**
     * Factory that creates {@link TrackingCompression} instances and registers
     * them under the {@code zlib@openssh.com} algorithm name.
     */
    private class TrackingCompressionFactory
            implements SshCompressionFactory<TrackingCompression> {

        @Override
        public TrackingCompression create()
                throws NoSuchAlgorithmException, IOException {
            return new TrackingCompression();
        }

        @Override
        public String[] getKeys() {
            return new String[] { SshContext.COMPRESSION_ZLIB_OPENSSH };
        }
    }
}


/**
 * Regression tests for the {@code zlib@openssh.com} delayed-start fix.
 *
 * <p>RFC / OpenSSH extension requires that {@code zlib@openssh.com} compression
 * is activated <em>only after</em> {@code SSH_MSG_USERAUTH_SUCCESS} is exchanged,
 * not immediately after {@code SSH_MSG_NEWKEYS}.  Before the fix the server
 * activated compression at key-exchange time, which breaks interoperability with
 * standards-compliant clients.</p>
 *
 * <p>Two virtual (in-memory) tests are provided:</p>
 * <ol>
 *   <li>{@link #testZlibOpenSSHConnectionSucceeds()} — end-to-end connectivity
 *       check: both sides negotiate {@code zlib@openssh.com}, the connection
 *       completes authentication, and the client can exchange data.</li>
 *   <li>{@link #testServerCompressionNotActiveBeforeAuthSuccess()} — timing
 *       check: uses a {@link TrackingCompression} wrapper to assert that the
 *       server's outgoing compressor is not exercised until after
 *       {@code SSH_MSG_USERAUTH_SUCCESS} has been sent.</li>
 * </ol>
 */
public class OpenSSHCompressionTimingTest extends AbstractVirtualConnectionTests {

    // -------------------------------------------------------------------------
    // Tracking state shared between the event listener and the compressor
    // -------------------------------------------------------------------------

    /**
     * Set to {@code true} by the global event listener when
     * {@code EVENT_USERAUTH_SUCCESS} fires (i.e. the server has just sent
     * {@code SSH_MSG_USERAUTH_SUCCESS}).
     */
    private final AtomicBoolean authSuccessEventFired = new AtomicBoolean(false);

    /**
     * Set to {@code true} by {@link TrackingCompression#compress} if the
     * compressor is called before {@link #authSuccessEventFired} becomes {@code true}.
     */
    private final AtomicBoolean compressionUsedBeforeAuth = new AtomicBoolean(false);

    /** The event listener registered for this test; removed in tearDown. */
    private EventListener authEventListener;

    // -------------------------------------------------------------------------
    // JUnit lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void setUp() throws Exception {
        authSuccessEventFired.set(false);
        compressionUsedBeforeAuth.set(false);

        authEventListener = new EventListener() {
            @Override
            public void processEvent(Event evt) {
                if (evt.getId() == EventCodes.EVENT_USERAUTH_SUCCESS) {
                    authSuccessEventFired.set(true);
                }
            }
        };
        EventServiceImplementation.getInstance().addListener(authEventListener);

        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            super.tearDown();
        } finally {
            if (authEventListener != null) {
                EventServiceImplementation.getInstance().removeListener(authEventListener);
                authEventListener = null;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Configuration hooks
    // -------------------------------------------------------------------------

    @Override
    protected void configureClientContext(SshClientContext ctx)
            throws IOException, SshException {
        ctx.setUsername("admin");
        ctx.addAuthenticator(PasswordAuthenticator.forPassword("admin"));
        ctx.setPreferredCompressionCS(SshContext.COMPRESSION_ZLIB_OPENSSH);
        ctx.setPreferredCompressionSC(SshContext.COMPRESSION_ZLIB_OPENSSH);
    }

    /**
     * Configures the server context to negotiate {@code zlib@openssh.com} and
     * installs a {@link TrackingCompressionFactory} that records whether the
     * outgoing compressor is used before authentication completes.
     */
    @Override
    protected void configureServerContext(SshServerContext ctx)
            throws IOException, SshException {
        ctx.setPreferredCompressionCS(SshContext.COMPRESSION_ZLIB_OPENSSH);
        ctx.setPreferredCompressionSC(SshContext.COMPRESSION_ZLIB_OPENSSH);

        // Replace the default zlib@openssh.com factory with our tracking one
        // so we can observe when the server first calls compress().
        ctx.supportedCompressionsSC().remove(SshContext.COMPRESSION_ZLIB_OPENSSH);
        ctx.supportedCompressionsSC().add(new TrackingCompressionFactory());

        ctx.supportedCompressionsCS().remove(SshContext.COMPRESSION_ZLIB_OPENSSH);
        ctx.supportedCompressionsCS().add(new TrackingCompressionFactory());
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    /**
     * Verifies that a virtual connection with {@code zlib@openssh.com} on both
     * sides completes authentication successfully.
     *
     * <p>Before the fix, the server would activate compression immediately after
     * key exchange, causing a decompression failure on the client during the auth
     * exchange (when talking to a correctly-implemented client), and the
     * connection would fail.  After the fix, both sides defer activation until
     * after {@code SSH_MSG_USERAUTH_SUCCESS}, and the connection succeeds.</p>
     */
    public void testZlibOpenSSHConnectionSucceeds() throws Exception {
        ConnectRequestFuture future = connectVirtual();
        assertTrue("zlib@openssh.com virtual connection should succeed",
                future.isSuccess());
    }

    /**
     * Verifies that the server-side outgoing compressor is not called before
     * {@code SSH_MSG_USERAUTH_SUCCESS} has been sent.
     *
     * <p>A {@link TrackingCompression} wrapper is installed as the server's
     * SC (Server→Client) compression.  If {@code compress()} is invoked before
     * the {@code EVENT_USERAUTH_SUCCESS} event fires, {@link #compressionUsedBeforeAuth}
     * is set to {@code true} and the assertion fails.</p>
     *
     * <p>Without the fix, the server activates outgoing compression right after
     * {@code SSH_MSG_NEWKEYS} and then compresses the auth-service accept and
     * userauth-related messages — all of which arrive before the success event,
     * causing the assertion to fail.</p>
     */
    public void testServerCompressionNotActiveBeforeAuthSuccess() throws Exception {
        ConnectRequestFuture future = connectVirtual();
        assertTrue("zlib@openssh.com virtual connection should succeed",
                future.isSuccess());
        assertFalse(
                "Server must not use the zlib@openssh.com compressor before " +
                "SSH_MSG_USERAUTH_SUCCESS is sent",
                compressionUsedBeforeAuth.get());
    }

    // -------------------------------------------------------------------------
    // Inner helpers
    // -------------------------------------------------------------------------

    /**
     * A {@link SshCompression} wrapper around {@link OpenSSHZLibCompression}
     * that records the first call to {@link #compress} relative to the
     * authentication-success event.
     */
    private class TrackingCompression extends OpenSSHZLibCompression {

        @Override
        public byte[] compress(byte[] buf, int start, int len) throws IOException {
            if (!authSuccessEventFired.get()) {
                compressionUsedBeforeAuth.set(true);
            }
            return super.compress(buf, start, len);
        }
    }

    /**
     * Factory that creates {@link TrackingCompression} instances and registers
     * them under the {@code zlib@openssh.com} algorithm name.
     */
    private class TrackingCompressionFactory
            implements SshCompressionFactory<TrackingCompression> {

        @Override
        public TrackingCompression create()
                throws NoSuchAlgorithmException, IOException {
            return new TrackingCompression();
        }

        @Override
        public String[] getKeys() {
            return new String[] { SshContext.COMPRESSION_ZLIB_OPENSSH };
        }
    }
}
