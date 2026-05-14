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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import com.sshtools.client.PasswordAuthenticator;
import com.sshtools.client.SshClient;
import com.sshtools.client.SshClient.SshClientBuilder;
import com.sshtools.client.SshClientContext;
import com.sshtools.client.sftp.SftpClient;
import com.sshtools.client.sftp.SftpClient.SftpClientBuilder;
import com.sshtools.client.sftp.SftpFile;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.ssh.SshException;
import com.sshtools.synergy.nio.ConnectRequestFuture;

/**
 * Concrete tests for the virtual (in-memory) SSH transport.
 *
 * Exercises shell commands provided by {@code ShellCommandFactory} and all SFTP
 * operations supported by {@code InMemoryFileFactory}.
 */
public class VirtualConnectionTests extends AbstractVirtualConnectionTests {

    @Override
    protected void configureClientContext(SshClientContext ctx) throws IOException, SshException {
        ctx.setUsername("admin");
        ctx.addAuthenticator(PasswordAuthenticator.forPassword("admin"));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private SshClient connect() throws Exception {
        ConnectRequestFuture future = connectVirtual();
        assertTrue("Virtual connection did not succeed", future.isSuccess());
        com.sshtools.common.ssh.SshConnection con = future.getConnection();
        con.getAuthenticatedFuture().waitForever();
        assertTrue("Authentication did not succeed", con.getAuthenticatedFuture().isSuccess());
        return SshClientBuilder.create(con).build();
    }

    // -------------------------------------------------------------------------
    // Shell / exec tests
    // -------------------------------------------------------------------------

    public void testEchoCommand() throws Exception {
        try (SshClient client = connect()) {
            String output = client.executeCommand("echo hello");
            assertTrue("Expected 'hello' in echo output but got: " + output,
                    output.trim().equals("hello"));
        }
    }

    public void testEchoMultipleWords() throws Exception {
        try (SshClient client = connect()) {
            String output = client.executeCommand("echo foo bar baz");
            assertTrue("Expected 'foo bar baz' in output but got: " + output,
                    output.trim().equals("foo bar baz"));
        }
    }

    public void testDateCommand() throws Exception {
        try (SshClient client = connect()) {
            String output = client.executeCommand("date");
            assertNotNull("date command produced no output", output);
            assertFalse("date command output was empty", output.trim().isEmpty());
        }
    }

    public void testEnvCommand() throws Exception {
        try (SshClient client = connect()) {
            String output = client.executeCommand("env");
            assertNotNull("env command produced no output", output);
        }
    }

    public void testSetCommand() throws Exception {
        try (SshClient client = connect()) {
            String output = client.executeCommand("set");
            assertNotNull("set command produced no output", output);
        }
    }

    public void testHelpCommand() throws Exception {
        try (SshClient client = connect()) {
            String output = client.executeCommand("help");
            assertNotNull("help command produced no output", output);
            assertFalse("help command output was empty", output.trim().isEmpty());
        }
    }

    public void testClearCommand() throws Exception {
        try (SshClient client = connect()) {
            // clear just sends terminal reset sequences; it should not throw
            client.executeCommand("clear");
        }
    }

    public void testSleepCommand() throws Exception {
        try (SshClient client = connect()) {
            long start = System.currentTimeMillis();
            client.executeCommand("sleep 0");
            long elapsed = System.currentTimeMillis() - start;
            assertTrue("sleep 0 took unreasonably long: " + elapsed + " ms", elapsed < 5000);
        }
    }

    public void testAliasAndUnalias() throws Exception {
        try (SshClient client = connect()) {
            // 'alias' with no args lists aliases – should not throw
            client.executeCommand("alias");
        }
    }

    // -------------------------------------------------------------------------
    // SFTP tests
    // -------------------------------------------------------------------------

    public void testSftpPwd() throws Exception {
        try (SshClient client = connect();
             SftpClient sftp = SftpClientBuilder.create().withClient(client).build()) {
            String pwd = sftp.pwd();
            assertNotNull("pwd() returned null", pwd);
            assertFalse("pwd() returned empty string", pwd.isEmpty());
        }
    }

    public void testSftpLsRoot() throws Exception {
        try (SshClient client = connect();
             SftpClient sftp = SftpClientBuilder.create().withClient(client).build()) {
            SftpFile[] files = sftp.ls("/");
            assertNotNull("ls('/') returned null", files);
        }
    }

    public void testSftpMkdirAndLs() throws Exception {
        try (SshClient client = connect();
             SftpClient sftp = SftpClientBuilder.create().withClient(client).build()) {

            sftp.mkdir("/testdir");

            SftpFile[] files = sftp.ls("/");
            List<String> names = Arrays.stream(files)
                    .map(SftpFile::getFilename)
                    .collect(java.util.stream.Collectors.toList());
            assertTrue("Created directory 'testdir' not found in ls('/'): " + names,
                    names.contains("testdir"));

            // cleanup
            sftp.rmdir("/testdir");
        }
    }

    public void testSftpMkdirs() throws Exception {
        try (SshClient client = connect();
             SftpClient sftp = SftpClientBuilder.create().withClient(client).build()) {

            sftp.mkdirs("/nested/deep/path");

            SftpFileAttributes attrs = sftp.stat("/nested/deep/path");
            assertTrue("mkdirs target is not a directory", attrs.isDirectory());

            // cleanup
            sftp.rm("/nested/deep/path", true, true);
        }
    }

    public void testSftpPutAndGet() throws Exception {
        byte[] content = "hello sftp world\n".getBytes(StandardCharsets.UTF_8);

        try (SshClient client = connect();
             SftpClient sftp = SftpClientBuilder.create().withClient(client).build()) {

            sftp.put(new ByteArrayInputStream(content), "/upload.txt");

            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            sftp.get("/upload.txt", buf);

            assertArrayEquals("Downloaded content does not match uploaded content",
                    content, buf.toByteArray());

            // cleanup
            sftp.rm("/upload.txt");
        }
    }

    public void testSftpStat() throws Exception {
        byte[] content = "stat test".getBytes(StandardCharsets.UTF_8);

        try (SshClient client = connect();
             SftpClient sftp = SftpClientBuilder.create().withClient(client).build()) {

            sftp.put(new ByteArrayInputStream(content), "/stattest.txt");

            SftpFileAttributes attrs = sftp.stat("/stattest.txt");
            assertNotNull("stat returned null", attrs);
            assertFalse("stat says /stattest.txt is a directory", attrs.isDirectory());
            assertEquals("stat size mismatch", (long) content.length, attrs.size().longValue());

            // cleanup
            sftp.rm("/stattest.txt");
        }
    }

    public void testSftpRename() throws Exception {
        byte[] content = "rename me".getBytes(StandardCharsets.UTF_8);

        try (SshClient client = connect();
             SftpClient sftp = SftpClientBuilder.create().withClient(client).build()) {

            sftp.put(new ByteArrayInputStream(content), "/original.txt");
            sftp.rename("/original.txt", "/renamed.txt");

            SftpFileAttributes attrs = sftp.stat("/renamed.txt");
            assertNotNull("renamed file not found", attrs);

            // cleanup
            sftp.rm("/renamed.txt");
        }
    }

    public void testSftpRm() throws Exception {
        byte[] content = "delete me".getBytes(StandardCharsets.UTF_8);

        try (SshClient client = connect();
             SftpClient sftp = SftpClientBuilder.create().withClient(client).build()) {

            sftp.put(new ByteArrayInputStream(content), "/todelete.txt");
            sftp.rm("/todelete.txt");

            SftpFile[] files = sftp.ls("/");
            List<String> names = Arrays.stream(files)
                    .map(SftpFile::getFilename)
                    .collect(java.util.stream.Collectors.toList());
            assertFalse("Deleted file still appears in listing: " + names,
                    names.contains("todelete.txt"));
        }
    }

    public void testSftpCdAndPwd() throws Exception {
        try (SshClient client = connect();
             SftpClient sftp = SftpClientBuilder.create().withClient(client).build()) {

            sftp.mkdir("/cdtest");
            sftp.cd("/cdtest");
            String pwd = sftp.pwd();
            assertTrue("pwd after cd should end with 'cdtest' but was: " + pwd,
                    pwd.endsWith("cdtest") || pwd.endsWith("cdtest/"));

            sftp.cd("/");
            // cleanup
            sftp.rmdir("/cdtest");
        }
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    private static void assertArrayEquals(String message, byte[] expected, byte[] actual) {
        assertTrue(message + " (expected length " + expected.length + ", actual " + actual.length + ")",
                Arrays.equals(expected, actual));
    }
}
