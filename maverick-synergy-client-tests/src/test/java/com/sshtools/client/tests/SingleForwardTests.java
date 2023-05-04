package com.sshtools.client.tests;

import java.io.IOException;

import com.sshtools.common.tests.ForwardingConfiguration;

public class SingleForwardTests extends AbstractNGForwardingTests {

	@Override
	protected ForwardingConfiguration createForwardingConfiguration() throws IOException {
		return new ForwardingConfiguration() {
			protected String getFilename() {
				return "single-forwarding.properties";
			}
		};
	}

}
