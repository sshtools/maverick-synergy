
package com.sshtools.client;

import java.io.IOException;

public class NoneAuthenticator extends SimpleClientAuthenticator {

	@Override
	public String getName() {
		return "none";
	}

	@Override
	public void authenticate(TransportProtocolClient transport, String username) throws IOException {
		transport.postMessage(new AuthenticationMessage(username,
				"ssh-connection", "none"));
	}

}
