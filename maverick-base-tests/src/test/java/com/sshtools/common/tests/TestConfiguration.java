package com.sshtools.common.tests;

/*-
 * #%L
 * Base API Tests
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.util.Arrays;
import com.sshtools.common.util.IOUtils;

public class TestConfiguration {

	protected Properties properties;
	
	protected void load() throws IOException {
		properties = new Properties();
		try(InputStream in = new FileInputStream(new File(getFilename()))) {
			properties.load(in);
		}
	}

	protected String getFilename() {
		return "test.properties";
	}

	public String getUsername() {
		return properties.getProperty("username", "root");
	}

	public String getHostname() {
		return properties.getProperty("hostname", "localhost");
	}

	public int getPort() {
		return Integer.parseInt(properties.getProperty("port", "22"));
	}

	public char[] getPassword() {
		String res = properties.getProperty("password");
		if(res==null) {
			return null;
		}
		return res.toCharArray();
	}

	public File getPrivateKey() {
		return new File(properties.getProperty("privateKey"), "testkey");
	}

	public String getPassphrase() {
		return properties.getProperty("passphrase");
	}

	public long getKeyExchangeLimit() {
		return IOUtils.fromByteSize(properties.getProperty("keyExchangeLimit", "10mb"));
	}

	public boolean enableLogging() {
		return Boolean.parseBoolean(properties.getProperty("enableLogging", "true"));
	}
	
	public String getLoggingLevel() {
		return properties.getProperty("loggingLevel", "INFO");
	}

	public String getLoggingConfig() {
		return properties.getProperty("loggingConfig", "log4j.properties");
	}

	public SshKeyPair[] getIdentities() throws IOException, InvalidPassphraseException {
		File privateKey = getPrivateKey();
		if(privateKey.exists()) {
			return Arrays.add(SshKeyUtils.getPrivateKey(getPrivateKey(), getPassphrase()), new SshKeyPair[0]);
		} else {
			return new SshKeyPair[0];
		}
		
	}
}
