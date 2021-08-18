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
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
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
