/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2025 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */
package com.sshtools.common.sshd.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Test;

/**
 * Tests for {@link SshdConfigFile}, {@link SshdConfigFileReader}, and
 * {@link SshdConfigFileWriter}.
 */
public class SshdConfigFileTest {

    // -----------------------------------------------------------------------
    // Basic parsing
    // -----------------------------------------------------------------------

    @Test
    public void testReadSimplePort() throws IOException {
        String config = "Port 2222\n";
        SshdConfigFile file = new SshdConfigFileReader(config).read();
        assertEquals("2222", file.getGlobalConfiguration().getValue(SshdConfigFile.Port));
    }

    @Test
    public void testReadPasswordAuthentication() throws IOException {
        String config = "PasswordAuthentication no\n";
        SshdConfigFile file = new SshdConfigFileReader(config).read();
        assertEquals("no", file.getGlobalConfiguration().getValue(SshdConfigFile.PasswordAuthentication));
    }

    @Test
    public void testReadPubkeyAuthentication() throws IOException {
        String config = "PubkeyAuthentication yes\n";
        SshdConfigFile file = new SshdConfigFileReader(config).read();
        assertEquals("yes", file.getGlobalConfiguration().getValue(SshdConfigFile.PubkeyAuthentication));
    }

    @Test
    public void testReadMultipleDirectives() throws IOException {
        String config =
                "Port 22\n" +
                "PasswordAuthentication no\n" +
                "PubkeyAuthentication yes\n" +
                "PermitRootLogin no\n";
        SshdConfigFile file = new SshdConfigFileReader(config).read();
        GlobalConfiguration global = file.getGlobalConfiguration();
        assertEquals("22", global.getValue(SshdConfigFile.Port));
        assertEquals("no", global.getValue(SshdConfigFile.PasswordAuthentication));
        assertEquals("yes", global.getValue(SshdConfigFile.PubkeyAuthentication));
        assertEquals("no", global.getValue(SshdConfigFile.PermitRootLogin));
    }

    @Test
    public void testReadCommentIsIgnoredForDirective() throws IOException {
        String config = "# This is a comment\nPort 22\n";
        SshdConfigFile file = new SshdConfigFileReader(config).read();
        assertEquals("22", file.getGlobalConfiguration().getValue(SshdConfigFile.Port));
    }

    @Test
    public void testReadEmptyConfig() throws IOException {
        String config = "";
        SshdConfigFile file = new SshdConfigFileReader(config).read();
        assertNotNull(file);
        assertNotNull(file.getGlobalConfiguration());
    }

    @Test
    public void testReadBlankLinesHandled() throws IOException {
        String config = "\n\nPort 22\n\n";
        SshdConfigFile file = new SshdConfigFileReader(config).read();
        assertEquals("22", file.getGlobalConfiguration().getValue(SshdConfigFile.Port));
    }

    @Test
    public void testUnknownDirectiveGivesNullValue() throws IOException {
        String config = "Port 22\n";
        SshdConfigFile file = new SshdConfigFileReader(config).read();
        assertNull(file.getGlobalConfiguration().getEntry(SshdConfigFile.PasswordAuthentication));
    }

    // -----------------------------------------------------------------------
    // Various directives
    // -----------------------------------------------------------------------

    @Test
    public void testReadListenAddress() throws IOException {
        String config = "ListenAddress 0.0.0.0\n";
        SshdConfigFile file = new SshdConfigFileReader(config).read();
        assertEquals("0.0.0.0", file.getGlobalConfiguration().getValue(SshdConfigFile.ListenAddress));
    }

    @Test
    public void testReadPermitEmptyPasswords() throws IOException {
        String config = "PermitEmptyPasswords no\n";
        SshdConfigFile file = new SshdConfigFileReader(config).read();
        assertEquals("no", file.getGlobalConfiguration().getValue(SshdConfigFile.PermitEmptyPasswords));
    }

    @Test
    public void testReadUsePAM() throws IOException {
        String config = "UsePAM yes\n";
        SshdConfigFile file = new SshdConfigFileReader(config).read();
        assertEquals("yes", file.getGlobalConfiguration().getValue(SshdConfigFile.UsePAM));
    }

    @Test
    public void testReadX11Forwarding() throws IOException {
        String config = "X11Forwarding no\n";
        SshdConfigFile file = new SshdConfigFileReader(config).read();
        assertEquals("no", file.getGlobalConfiguration().getValue(SshdConfigFile.X11Forwarding));
    }

    @Test
    public void testReadSubsystem() throws IOException {
        String config = "Subsystem sftp /usr/lib/openssh/sftp-server\n";
        SshdConfigFile file = new SshdConfigFileReader(config).read();
        assertEquals("sftp /usr/lib/openssh/sftp-server",
                file.getGlobalConfiguration().getValue(SshdConfigFile.Subsystem));
    }

    @Test
    public void testReadBanner() throws IOException {
        String config = "Banner /etc/issue.net\n";
        SshdConfigFile file = new SshdConfigFileReader(config).read();
        assertEquals("/etc/issue.net", file.getGlobalConfiguration().getValue(SshdConfigFile.Banner));
    }

    @Test
    public void testReadLogLevel() throws IOException {
        String config = "LogLevel INFO\n";
        SshdConfigFile file = new SshdConfigFileReader(config).read();
        assertEquals("INFO", file.getGlobalConfiguration().getValue(SshdConfigFile.LogLevel));
    }

    @Test
    public void testReadMaxAuthTries() throws IOException {
        String config = "MaxAuthTries 6\n";
        SshdConfigFile file = new SshdConfigFileReader(config).read();
        assertEquals("6", file.getGlobalConfiguration().getValue(SshdConfigFile.MaxAuthTries));
    }

    @Test
    public void testReadMaxSessions() throws IOException {
        String config = "MaxSessions 10\n";
        SshdConfigFile file = new SshdConfigFileReader(config).read();
        assertEquals("10", file.getGlobalConfiguration().getValue(SshdConfigFile.MaxSessions));
    }

    @Test
    public void testReadClientAliveInterval() throws IOException {
        String config = "ClientAliveInterval 120\n";
        SshdConfigFile file = new SshdConfigFileReader(config).read();
        assertEquals("120", file.getGlobalConfiguration().getValue(SshdConfigFile.ClientAliveInterval));
    }

    @Test
    public void testReadAuthorizedKeysFile() throws IOException {
        String config = "AuthorizedKeysFile .ssh/authorized_keys\n";
        SshdConfigFile file = new SshdConfigFileReader(config).read();
        assertEquals(".ssh/authorized_keys",
                file.getGlobalConfiguration().getValue(SshdConfigFile.AuthorizedKeysFile));
    }

    // -----------------------------------------------------------------------
    // Match entries
    // -----------------------------------------------------------------------

    @Test
    public void testReadMatchEntry() throws IOException {
        String config =
                "Port 22\n" +
                "Match User admin\n" +
                "  PasswordAuthentication yes\n";
        SshdConfigFile file = new SshdConfigFileReader(config).read();
        assertNotNull(file.getGlobalConfiguration());
        assertEquals("22", file.getGlobalConfiguration().getValue(SshdConfigFile.Port));
        // Match entry should exist
        assertNotNull(file.getMatchEntriesIterator());
    }

    // -----------------------------------------------------------------------
    // Write round-trip
    // -----------------------------------------------------------------------

    @Test
    public void testWriteRoundTrip() throws IOException {
        String config =
                "Port 2222\n" +
                "PasswordAuthentication no\n";
        SshdConfigFile file = new SshdConfigFileReader(config).read();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new SshdConfigFileWriter(baos).write(file, false);
        String written = baos.toString("UTF-8");

        // The written output should contain the key directives
        assertNotNull(written);
        // Re-parse and verify values are still correct
        SshdConfigFile reparsed = new SshdConfigFileReader(written).read();
        assertEquals("2222", reparsed.getGlobalConfiguration().getValue(SshdConfigFile.Port));
        assertEquals("no", reparsed.getGlobalConfiguration().getValue(SshdConfigFile.PasswordAuthentication));
    }

    // -----------------------------------------------------------------------
    // Update entry
    // -----------------------------------------------------------------------

    @Test
    public void testUpdateEntry() throws IOException {
        String config = "Port 22\n";
        SshdConfigFile file = new SshdConfigFileReader(config).read();
        assertEquals("22", file.getGlobalConfiguration().getValue(SshdConfigFile.Port));

        file.getGlobalConfiguration().updateEntry(SshdConfigFile.Port, "2222");
        assertEquals("2222", file.getGlobalConfiguration().getValue(SshdConfigFile.Port));
    }

    @Test
    public void testEnableEntry() throws IOException {
        String config = "#PasswordAuthentication yes\n";
        SshdConfigFile file = new SshdConfigFileReader(config).read();
        // commented-out directive gets enabled
        file.getGlobalConfiguration().enable(SshdConfigFile.PasswordAuthentication, "no");
        assertEquals("no", file.getGlobalConfiguration().getValue(SshdConfigFile.PasswordAuthentication));
    }

    // -----------------------------------------------------------------------
    // Builder API
    // -----------------------------------------------------------------------

    @Test
    public void testBuilderCreateAndBuild() {
        SshdConfigFile file = SshdConfigFile.builder().build();
        assertNotNull(file);
        assertNotNull(file.getGlobalConfiguration());
    }
}
