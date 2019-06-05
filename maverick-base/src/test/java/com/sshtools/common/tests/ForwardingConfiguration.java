package com.sshtools.common.tests;

import java.io.IOException;

import com.sshtools.common.util.IOUtil;

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
		return IOUtil.fromByteSize(properties.getProperty("forwardingDataAmount", "100mb"));
	}

	public int getForwardingChannelsPerClientCount() {
		return Integer.parseInt(properties.getProperty("forwardingChannelsPerClientCount", "1"));
	}

	public int getForwardingChannelInterval() {
		return Integer.parseInt(properties.getProperty("forwardingChannelInterval", "1000"));
	}
}
