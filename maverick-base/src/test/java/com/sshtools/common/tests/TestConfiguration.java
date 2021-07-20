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

package com.sshtools.common.tests;

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
