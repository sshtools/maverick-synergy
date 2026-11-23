package com.sshtools.common.ssh.x509.tests;

/*-
 * #%L
 * X509 Certificate Support Tests
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.SshX509PublicKey;
import com.sshtools.common.ssh.x509.SshX509DsaPublicKeyRfc6187;
import com.sshtools.common.ssh.x509.SshX509EcdsaSha2Nist256Rfc6187;
import com.sshtools.common.ssh.x509.SshX509EcdsaSha2Nist384Rfc6187;
import com.sshtools.common.ssh.x509.SshX509EcdsaSha2Nist521Rfc6187;
import com.sshtools.common.ssh.x509.SshX509Rsa2048Sha256Rfc6187;
import com.sshtools.common.ssh.x509.SshX509RsaPublicKeyRfc6187;
import com.sshtools.common.util.ByteArrayWriter;

class Rfc6187InputBoundsTest {

    @Test
    void rejectsCertificateCountsBeforeAllocation() throws Exception {
        for (KeyDecoder decoder : decoders()) {
            assertInvalidCount(decoder, SshX509PublicKey.MAX_CERTIFICATE_CHAIN_LENGTH + 1);
            assertInvalidCount(decoder, Integer.MAX_VALUE);
            assertInvalidCount(decoder, 0xffffffffL);
        }
    }

    @Test
    void rejectsCountsThatCannotFitInRemainingBlob() throws Exception {
        for (KeyDecoder decoder : decoders()) {
            assertInvalidCount(decoder, 1);
        }
    }

    private static void assertInvalidCount(KeyDecoder decoder, long count) throws Exception {
        byte[] blob = blob(decoder.algorithm(), count);
        SshException error = assertThrows(SshException.class,
                () -> decoder.factory().get().init(blob, 0, blob.length));
        assertTrue(error.getMessage().contains("certificate")
                || error.getMessage().contains("Certificate"));
    }

    private static byte[] blob(String algorithm, long count) throws Exception {
        try (ByteArrayWriter writer = new ByteArrayWriter()) {
            writer.writeString(algorithm);
            writer.writeInt(count);
            return writer.toByteArray();
        }
    }

    private static List<KeyDecoder> decoders() {
        return List.of(
                new KeyDecoder(SshX509RsaPublicKeyRfc6187.X509V3_SSH_RSA,
                        SshX509RsaPublicKeyRfc6187::new),
                new KeyDecoder(SshX509Rsa2048Sha256Rfc6187.X509V3_SSH_RSA,
                        SshX509Rsa2048Sha256Rfc6187::new),
                new KeyDecoder(SshX509DsaPublicKeyRfc6187.X509V3_SSH_DSS,
                        SshX509DsaPublicKeyRfc6187::new),
                new KeyDecoder("x509v3-ecdsa-sha2-nistp256",
                        SshX509EcdsaSha2Nist256Rfc6187::new),
                new KeyDecoder("x509v3-ecdsa-sha2-nistp384",
                        SshX509EcdsaSha2Nist384Rfc6187::new),
                new KeyDecoder("x509v3-ecdsa-sha2-nistp521",
                        SshX509EcdsaSha2Nist521Rfc6187::new));
    }

    private record KeyDecoder(String algorithm, Supplier<SshPublicKey> factory) {
    }
}
