package com.sshtools.client.tests;

/*-
 * #%L
 * Client API Tests
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

import java.lang.reflect.Field;

import com.sshtools.client.sftp.SftpClient.SftpClientBuilder;

import junit.framework.TestCase;

public class SftpClientBuilderTests extends TestCase {

    public void testBuilderOptions() throws Exception {
        SftpClientBuilder b = SftpClientBuilder.create()
                .withAdaptiveBlockSize(true)
                .withResilientTransfers(true)
                .withMaxReconnectAttempts(5);
        Field f = b.getClass().getDeclaredField("adaptiveBlockSize");
        f.setAccessible(true);
        assertTrue((Boolean) f.get(b));
        f = b.getClass().getDeclaredField("resilientTransfers");
        f.setAccessible(true);
        assertTrue((Boolean) f.get(b));
        f = b.getClass().getDeclaredField("maxReconnectAttempts");
        f.setAccessible(true);
        assertEquals(5, f.getInt(b));
    }

    public void testNewPutGetMethodsExist() throws Exception {
        assertNotNull(SftpClientBuilder.class.getEnclosingClass()
                .getMethod("put", String.class, String.class, boolean.class, boolean.class));
        assertNotNull(SftpClientBuilder.class.getEnclosingClass()
                .getMethod("get", String.class, String.class, boolean.class, boolean.class));
    }
}

