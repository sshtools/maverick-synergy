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
