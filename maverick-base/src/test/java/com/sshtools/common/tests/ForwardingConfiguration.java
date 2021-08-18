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

import java.io.IOException;

import com.sshtools.common.util.IOUtils;

public abstract class ForwardingConfiguration extends TestConfiguration {

	public ForwardingConfiguration() throws IOException {
		load();
	}
	
	protected String getFilename() {
		return "forwarding.properties";
	}
	
	public int getForwardingClientCount() {
		return Integer.parseInt(properties.getProperty("forwardingClientCount", "1"));
	}
	
	public long getForwardingTimeout() {
		return Long.parseLong(properties.getProperty("forwardingTestTimeout", "5")) * 60000 * 1000;
	}

	public int getForwardingClientInterval() {
		return Integer.parseInt(properties.getProperty("forwardingClientInterval", "5000"));
	}

	public int getForwardingDataBlock() {
		return Integer.parseInt(properties.getProperty("forwardingDataBlock", "33768"));
	}
	
	public long getForwardingDataAmount() {
		return IOUtils.fromByteSize(properties.getProperty("forwardingDataAmount", "100mb"));
	}

	public int getForwardingChannelsPerClientCount() {
		return Integer.parseInt(properties.getProperty("forwardingChannelsPerClientCount", "1"));
	}

	public int getForwardingChannelInterval() {
		return Integer.parseInt(properties.getProperty("forwardingChannelInterval", "1000"));
	}

	public boolean getRandomBlockSize() {
		return Boolean.parseBoolean(properties.getProperty("forwardingRandomBlock", "true"));
	}
}
