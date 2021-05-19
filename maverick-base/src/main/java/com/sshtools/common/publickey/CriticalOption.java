/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.common.publickey;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.common.util.Utils;

public class CriticalOption extends EncodedExtension {

	public static final String FORCE_COMMAND = "force-command";
	public static final String SOURCE_ADDRESS = "source-address";

	public CriticalOption(String name, byte[] value, boolean known) {
		setName(name);
		setKnown(known);
		setStoredValue(value);
	}

	public CriticalOption(String name, String value, boolean known) {
		setName(name);
		setKnown(known);
		try(ByteArrayWriter writer = new ByteArrayWriter()) {
			writer.writeString(value);
			setStoredValue(writer.toByteArray());
		} catch(IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		
	}
	
	public String getStringValue() {
		try {
			try(ByteArrayReader reader = new ByteArrayReader(getStoredValue())) {
				return reader.readString();
			}
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	public static class Builder {
	
		List<CriticalOption> tmp = new ArrayList<>();
		
		public Builder createCustomOption(String name, String value) {
			tmp.add(new CriticalOption(name, value, false));
			return this;
		}
		
		public Builder createCustomOption(String name, byte[] value) {
			tmp.add(new CriticalOption(name, value, false));
			return this;
		}
		
		public Builder forceCommand(String cmd) {
			tmp.add(new CriticalOption(FORCE_COMMAND, cmd, true));
			return this;
		}
		
		public Builder sourceAddress(String... addresses) {
			tmp.add(new CriticalOption(SOURCE_ADDRESS, Utils.csv(addresses), true));
			return this;
		}

		public List<CriticalOption> build() {
			return tmp;
		}
	}

	public static CriticalOption createKnownOption(String name, byte[] value) {
		switch(name) {
		case FORCE_COMMAND:
		case SOURCE_ADDRESS:
			return new CriticalOption(name, value, true);
		default:
			return new CriticalOption(name, value, false);
		}
		
	}
}
