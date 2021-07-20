
package com.sshtools.client.tests;

import java.io.IOException;

import com.sshtools.common.tests.ForwardingConfiguration;

public class MultipleForwardTests extends AbstractNGForwardingTests {

	@Override
	protected ForwardingConfiguration createForwardingConfiguration() throws IOException {
		return new ForwardingConfiguration() {
			protected String getFilename() {
				return "multiple-forwarding.properties";
			}
		};
	}

}
