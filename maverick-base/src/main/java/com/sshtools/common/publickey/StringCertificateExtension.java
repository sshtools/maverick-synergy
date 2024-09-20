package com.sshtools.common.publickey;

/*-
 * #%L
 * Base API
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

import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;

public class StringCertificateExtension extends CertificateExtension {

	public StringCertificateExtension(String name, String value, boolean known) {
		setName(name);
		setKnown(known);
		try(ByteArrayWriter writer = new ByteArrayWriter()) {
			writer.writeString(value);
			setStoredValue(writer.toByteArray());
		} catch(IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
	
	StringCertificateExtension(String name, byte[] value, boolean known) {
		setName(name);
		setStoredValue(value);
	}

	public String getValue() {
		try {
			try(ByteArrayReader reader = new ByteArrayReader(getStoredValue())) {
				return reader.readString();
			}
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
}
