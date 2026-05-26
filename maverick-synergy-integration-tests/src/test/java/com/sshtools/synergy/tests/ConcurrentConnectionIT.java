package com.sshtools.synergy.tests;

/*-
 * #%L
 * Integration Tests
 * %%
 * Copyright (C) 2002 - 2026 JADAPTIVE Limited
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sshtools.client.SshClient;
import com.sshtools.client.sftp.SftpClient;
import com.sshtools.client.sftp.SftpClient.SftpClientBuilder;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshException;

/**
 * Integration tests that verify the server handles multiple simultaneous
 * SSH connections correctly: all connections authenticate, and concurrent
 * SFTP operations complete without errors or data corruption.
 */
@DisplayName("Concurrent connections")
class ConcurrentConnectionIT extends AbstractSshIntegrationTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private SftpClient openSftp(SshClient ssh)
            throws SshException, PermissionDeniedException, IOException {
        return SftpClientBuilder.create().withClient(ssh).build();
    }

    /**
     * Opens {@code n} SSH connections concurrently using a thread pool and
     * returns a list of the authenticated clients.  All clients must be closed
     * by the caller.
     */
    private List<SshClient> openConcurrentConnections(int n) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch startLatch = new CountDownLatch(1); // release all threads simultaneously
        List<Future<SshClient>> futures = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            futures.add(pool.submit(() -> {
                startLatch.await();   // wait until all tasks are queued
                return connectWithPassword();
            }));
        }

        startLatch.countDown();   // release all threads at once

        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS),
                "thread pool did not terminate within 30 seconds");

        List<SshClient> clients = new ArrayList<>(n);
        for (Future<SshClient> f : futures) {
            clients.add(f.get());
        }
        return clients;
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("10 simultaneous password-auth connections all authenticate successfully")
    void tenConcurrentConnections_allAuthenticate() throws Exception {
        final int N = 10;
        List<SshClient> clients = openConcurrentConnections(N);
        try {
            long authenticated = clients.stream().filter(SshClient::isAuthenticated).count();
            assertEquals(N, authenticated,
                    "all " + N + " concurrent connections must be authenticated; got " + authenticated);
        } finally {
            clients.forEach(c -> { try { c.close(); } catch (Exception ignore) { } });
        }
    }

    @Test
    @DisplayName("10 simultaneous connections are all connected")
    void tenConcurrentConnections_allConnected() throws Exception {
        final int N = 10;
        List<SshClient> clients = openConcurrentConnections(N);
        try {
            long connected = clients.stream().filter(SshClient::isConnected).count();
            assertEquals(N, connected,
                    "all " + N + " concurrent connections must report isConnected(); got " + connected);
        } finally {
            clients.forEach(c -> { try { c.close(); } catch (Exception ignore) { } });
        }
    }

    @Test
    @DisplayName("20 concurrent SFTP uploads to distinct filenames all succeed")
    void twentyConcurrentSftpUploads_allSucceed(@TempDir Path localDir) throws Exception {
        final int N = 20;
        byte[] content = "concurrent-sftp-data".getBytes(StandardCharsets.UTF_8);

        // Prepare local source files
        List<Path> sources = new ArrayList<>(N);
        for (int i = 0; i < N; i++) {
            Path p = localDir.resolve("file-" + i + ".txt");
            Files.write(p, content);
            sources.add(p);
        }

        ExecutorService pool = Executors.newFixedThreadPool(N);
        CountDownLatch ready = new CountDownLatch(N);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>(N);
        for (int i = 0; i < N; i++) {
            final int idx = i;
            futures.add(pool.submit((Callable<Void>) () -> {
                ready.countDown();
                start.await();   // wait for all threads to be ready
                try (SshClient ssh = connectWithPassword();
                     SftpClient sftp = openSftp(ssh)) {
                    sftp.put(sources.get(idx).toString(), "concurrent-" + idx + ".txt");
                    succeeded.incrementAndGet();
                }
                return null;
            }));
        }

        // Wait for all threads to be ready, then release them
        assertTrue(ready.await(15, TimeUnit.SECONDS), "threads did not become ready in time");
        start.countDown();

        pool.shutdown();
        assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS),
                "upload pool did not terminate within 60 seconds");

        // Propagate any exceptions from individual tasks
        for (Future<?> f : futures) {
            f.get();
        }

        assertEquals(N, succeeded.get(),
                "all " + N + " concurrent SFTP uploads must succeed");
    }

    @Test
    @DisplayName("Connections established sequentially by multiple threads are stable")
    void sequentialConnectionsAcrossThreads_allStable() throws Exception {
        final int N = 5;
        AtomicInteger successCount = new AtomicInteger(0);
        List<Thread> threads = new ArrayList<>(N);

        for (int i = 0; i < N; i++) {
            Thread t = new Thread(() -> {
                try (SshClient ssh = connectWithPassword()) {
                    if (ssh.isAuthenticated()) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // counted as failure
                }
            });
            threads.add(t);
        }

        threads.forEach(Thread::start);
        for (Thread t : threads) {
            t.join(10_000);
        }

        assertEquals(N, successCount.get(),
                "all " + N + " threaded connections must succeed");
    }
}
