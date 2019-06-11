package com.sshtools.callback.client;

import java.io.IOException;
import java.util.Collection;

import com.sshtools.common.auth.KeyboardInteractiveProvider;
import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh2.KBIPrompt;

public class KeyboardInteractiveAuthentication implements KeyboardInteractiveProvider {

	
	
	SshConnection con;
	CallbackClient<?> client;
	
	KeyboardInteractiveAuthentication(CallbackClient<?> client) {
		this.client = client;
	}
	
	@Override
	public KBIPrompt[] init(SshConnection con) {
		this.con = con;
		return new KBIPrompt[] {new KBIPrompt("UUID", true)};
	}

	@Override
	public boolean setResponse(String[] answers, Collection<KBIPrompt> additionalPrompts) throws IOException {
		if(answers.length > 0) {
			if(Log.isDebugEnabled()) {
				Log.debug(String.format("Remote UUID is %s",answers[0]));
			}
			con.setProperty(CallbackClient.REMOTE_UUID, answers[0]);
		}
		return true;
	}

	@Override
	public String getName() {
		return client.getName();
	}

	@Override
	public String getInstruction() {
		return client.getUUID();
	}

	@Override
	public boolean hasAuthenticated() {
		try {
			return con.containsProperty(CallbackClient.REMOTE_UUID)
					&& client.authenticateUUID((String)con.getProperty(CallbackClient.REMOTE_UUID));
		} catch (IOException e) {
			Log.error("Failed to authenticate remote UUID", e);
			return false;
		}
	}

}
