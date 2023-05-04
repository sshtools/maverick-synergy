/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.common.knownhosts;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshKeyFingerprint;
import com.sshtools.common.ssh.components.SshPublicKey;

public class ConsoleKnownHostsFile extends KnownHostsFile {

	public ConsoleKnownHostsFile() throws SshException, IOException {
		super();
	}

	public ConsoleKnownHostsFile(File file) throws SshException, IOException {
		super(file);
	}
	
	@Override
	protected void onRevokedKey(String host, SshPublicKey key) {

		
		super.onRevokedKey(host, key);
	}

	@Override
	protected void onHostKeyMismatch(String host, List<SshPublicKey> allowedHostKey, SshPublicKey actual)
			throws SshException {

		try {
			System.out.println("The host key supplied by " + host + "(" + actual.getAlgorithm() + ")" + " is: "
					+ actual.getFingerprint());
			System.out.println("The current allowed keys for " + host + "(" + actual.getAlgorithm() + ")" + " are:");

			for (SshPublicKey key : allowedHostKey) {
				System.out.println(SshKeyUtils.getFormattedKey(key, ""));
			}

			getResponse(host, actual);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * <p>
	 * Prompts the user through the console to verify the host key.
	 * </p>
	 *
	 * @param host
	 *            the name of the host
	 * @param pk
	 *            the public key supplied by the host
	 */
	public void onUnknownHost(String host, SshPublicKey pk) {
		try {
			System.out.println("The host " + host + " is currently unknown to the system");
			System.out.println("The host key " + "(" + pk.getAlgorithm() + ") fingerprint is: "
					+ SshKeyFingerprint.getFingerprint(pk.getEncoded(), SshKeyFingerprint.SHA256_FINGERPRINT));

			getResponse(host, pk);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void onInvalidHostEntry(String entry) throws SshException {
		System.out.println("Invalid host entry in " + getKnownHostsFile().getAbsolutePath());
		System.out.println(entry);
	}

	private void getResponse(String host, SshPublicKey pk) throws SshException, IOException {
		String response = "";
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

		while (!(response.equalsIgnoreCase("YES") || response.equalsIgnoreCase("NO")
				|| (response.equalsIgnoreCase("ALWAYS") && isHostFileWriteable()))) {
			String options = (isHostFileWriteable() ? "Yes|No|Always" : "Yes|No");

			if (!isHostFileWriteable()) {
				System.out.println("Always option disabled, host file is not writeable");
			}

			System.out.print("Do you want to allow this host key? [" + options + "]: ");

			try {
				response = reader.readLine();
			} catch (IOException ex) {
				throw new SshException("Failed to read response", SshException.INTERNAL_ERROR);
			}
		}

		if (response.equalsIgnoreCase("YES")) {
			allowHost(host, pk, false);
		}

		if (response.equalsIgnoreCase("ALWAYS") && isHostFileWriteable()) {
			allowHost(host, pk, true);
		}

		// Do nothing on NO
	}
}
