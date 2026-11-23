package com.sshtools.synergy.ssh;

/*-
 * #%L
 * Common API
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.IOException;

import org.junit.Test;

public class NameListParsingTest {

    @Test
    public void parsesNamesInOrder() throws Exception {
        assertEquals(3, TransportProtocol.parseNameList("curve25519-sha256, ssh-ed25519,none").size());
    }

    @Test
    public void rejectsEmptyEntries() {
        assertThrows(IOException.class, () -> TransportProtocol.parseNameList("unknown,,,,"));
        assertThrows(IOException.class, () -> TransportProtocol.parseNameList(",unknown"));
        assertThrows(IOException.class, () -> TransportProtocol.parseNameList("unknown,"));
    }

    @Test
    public void boundsEntryCount() throws Exception {
        StringBuilder list = new StringBuilder();
        for (int i = 0; i < 256; i++) {
            if (i > 0) {
                list.append(',');
            }
            list.append('a');
        }
        assertEquals(256, TransportProtocol.parseNameList(list.toString()).size());

        list.append(",a");
        assertThrows(IOException.class, () -> TransportProtocol.parseNameList(list.toString()));
    }
}
