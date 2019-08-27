/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
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
		return Long.parseLong(properties.getProperty("forwardingTestTimeout", "300000"));
	}

	public int getForwardingClientInterval() {
		return Integer.parseInt(properties.getProperty("forwardingClientInterval", "5000"));
	}

	public int getForwardingDataBlock() {
		return Integer.parseInt(properties.getProperty("forwardingDataBlock", "65536"));
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
}
