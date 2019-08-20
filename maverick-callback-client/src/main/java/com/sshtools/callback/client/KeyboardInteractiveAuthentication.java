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
