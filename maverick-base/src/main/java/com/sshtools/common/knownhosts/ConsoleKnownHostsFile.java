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

	private void allowHost(String host, SshPublicKey key, boolean save) throws IOException, SshException {

		addEntry(key, "", resolveNames(host).toArray(new String[0]));
		if (save) {
			store();
		}
	}

}
