package com.sshtools.common.knownhosts;

/*-
 * #%L
 * Base API
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sshtools.common.publickey.OpenSshCertificate;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.SshHmac;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.util.Base64;
import com.sshtools.common.util.Utils;

/**
 * <p>
 * An abstract
 * <a href="../../maverick/ssh/HostKeyVerification.html">HostKeyVerification</a>
 * class implementation providing validation against the known_hosts format.
 * </p>
 * 
 * @author Lee David Painter
 */
public class KnownHostsKeyVerification implements HostKeyVerification, HostKeyUpdater {

	LinkedList<HostFileEntry> entries = new LinkedList<>();
	Set<KeyEntry> keyEntries = new LinkedHashSet<>();
	Set<KeyEntry> revokedEntries = new LinkedHashSet<>();
	Map<SshPublicKey, List<KeyEntry>> entriesByPublicKey = new HashMap<>();
	List<CertAuthorityEntry> certificateAuthorities = new ArrayList<>();

// Hashed support
	private boolean hashHosts = false;
	private boolean useCanonicalHostname = System.getProperty("maverick.knownHosts.enableReverseDNS", "true")
			.equalsIgnoreCase("true");
	private boolean useReverseDNS = System.getProperty("maverick.knownHosts.enableReverseDNS", "true")
			.equalsIgnoreCase("true");
	private static final String HASH_MAGIC = "|1|";
	private static final String HASH_DELIM = "|";

	Pattern nonStandard = Pattern.compile("\\[([^\\]]+)\\]:([\\d]{1,5})");

	public KnownHostsKeyVerification(InputStream in) throws SshException, IOException {
		load(in);
	}

	public KnownHostsKeyVerification(String knownhosts) throws SshException, IOException {
		load(new ByteArrayInputStream(Utils.getUTF8Bytes(knownhosts)));
	}

	public KnownHostsKeyVerification() {
	}

	public synchronized void clear() {
		entries.clear();
		keyEntries.clear();
		revokedEntries.clear();
		entriesByPublicKey.clear();
		certificateAuthorities.clear();
	}

	public synchronized void load(InputStream in) throws SshException, IOException {

		clear();
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String line;

		try {
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.equals("")) {
					entries.add(new BlankEntry());
					continue;
				}

				if (line.startsWith("#")) {
					entries.add(new CommentEntry(line.substring(1)));
					continue;
				}

				StringTokenizer tokens = new StringTokenizer(line, " ");

				if (!tokens.hasMoreTokens()) {
					entries.add(new InvalidEntry(line));
					try {
						onInvalidHostEntry(line);
					} catch (SshException e) {
					}
					continue;
				}

				String host = (String) tokens.nextElement();
				String marker = "";
				if (host.startsWith("@")) {
					marker = host;
					host = (String) tokens.nextElement();
				}

				String algorithm = null;

				try {
					if (!tokens.hasMoreTokens()) {
						entries.add(new InvalidEntry(line));
						try {
							onInvalidHostEntry(line);
						} catch (SshException e) {
						}
						continue;
					}

					algorithm = tokens.nextToken();

					if (!loadSsh1PublicKey(host, algorithm, tokens, line)) {

						if (!tokens.hasMoreTokens()) {
							entries.add(new InvalidEntry(line));
							try {
								onInvalidHostEntry(line);
							} catch (SshException e) {
							}
							continue;
						}

						SshPublicKey key = SshKeyUtils.getPublicKey(algorithm + " " + tokens.nextToken());
						StringBuffer comment = new StringBuffer();
						while (tokens.hasMoreTokens()) {
							if (comment.length() > 0) {
								comment.append(" ");
							}
							comment.append(tokens.nextToken());
						}
						loadSsh2PublicKey(host, marker, algorithm, key, comment.toString());
					}

				} catch (IOException e) {
					entries.add(new InvalidEntry(line));
					try {
						onInvalidHostEntry(line);
					} catch (SshException e2) {
					}
				} catch (SshException e) {
					entries.add(new InvalidEntry(line));
					try {
						onInvalidHostEntry(line);
					} catch (SshException e2) {
					}
				} catch (OutOfMemoryError ox) {
					reader.close();
					throw new SshException("Error parsing known_hosts file, is your file corrupt?",
							SshException.POSSIBLE_CORRUPT_FILE);
				}

			}
		} finally {
			reader.close();
			in.close();
		}
	}

	private Set<String> getNames(String host) {
		return new LinkedHashSet<String>(Arrays.asList(host.split(",")));
	}

	private void loadSsh2PublicKey(String host, String marker, String algorithm, SshPublicKey key, String comment)
			throws SshException {

		KeyEntry entry;

		if (marker.equalsIgnoreCase("@cert-authority")) {
			CertAuthorityEntry e = new CertAuthorityEntry(getNames(host), key, comment);
			certificateAuthorities.add(e);
			entry = e;
		} else if (marker.equalsIgnoreCase("@revoked")) {
			entry = new RevokedEntry(getNames(host), new Ssh2KeyEntry(getNames(host), key, comment, false));
		} else {
			entry = new Ssh2KeyEntry(getNames(host), key, comment, false);
		}

		addEntry(entry);
	}

	private void addEntry(KeyEntry entry) {

		if (!entriesByPublicKey.containsKey(entry.getKey())) {
			entriesByPublicKey.put(entry.getKey(), new ArrayList<KeyEntry>());
		}

		entries.add(entry);
		entriesByPublicKey.get(entry.getKey()).add(entry);
		if (entry instanceof KeyEntry) {
			keyEntries.add(entry);
		}
		if (entry instanceof RevokedEntry) {
			revokedEntries.add(entry);
		}
		onHostKeyAdded(getNames(entry.getNames()), entry.getKey());
	}

	protected void onHostKeyAdded(Set<String> names, SshPublicKey key) {

	}

	public synchronized void setComment(KeyEntry entry, String comment) {

		if (!keyEntries.contains(entry)) {
			throw new IllegalArgumentException("KeyEntry provided is no longer in this known_hosts file.");
		}
		entry.comment = comment;
	}

	private boolean loadSsh1PublicKey(String host, String algorithm, StringTokenizer tokens, String line)
			throws SshException {

		if (!algorithm.matches("[0-9]+")) {
			return false;
		}

		if (!tokens.hasMoreTokens()) {
			// Do not fail just tell the implementation to
			// allow it to decide what to do.
			entries.add(new InvalidEntry(line));
			try {
				onInvalidHostEntry(line);
			} catch (SshException e) {
			}
			return true;
		}

		@SuppressWarnings("unused")
		String e = (String) tokens.nextElement();

		if (!tokens.hasMoreTokens()) {
			entries.add(new InvalidEntry(line));
			try {
				onInvalidHostEntry(line);
			} catch (SshException e2) {
			}
			return true;
		}

		entries.add(new Ssh1KeyEntry(line));

		return true;
	}

	public synchronized void setHashHosts(boolean hashHosts) {
		this.hashHosts = hashHosts;
	}

	protected void onInvalidHostEntry(String entry) throws SshException {
// Do nothing
	}

	/**
	 * <p>
	 * Called by the <code>verifyHost</code> method when the host key supplied by
	 * the host does not match the current key recording in the known hosts file.
	 * </p>
	 * 
	 * @param host           the name of the host
	 * @param allowedHostKey the current key recorded in the known_hosts file.
	 * @param actualHostKey  the actual key supplied by the user
	 * 
	 * @throws SshException if an error occurs
	 * 
	 * @since 0.2.0
	 */
	protected void onHostKeyMismatch(String host, List<SshPublicKey> allowedHostKey, SshPublicKey actualHostKey)
			throws SshException {

	}

	/**
	 * <p>
	 * Called by the <code>verifyHost</code> method when the host key supplied is
	 * not recorded in the known_hosts file.
	 * </p>
	 * 
	 * <p>
	 * </p>
	 * 
	 * @param host the name of the host
	 * @param key  the public key supplied by the host
	 * 
	 * @throws SshException if an error occurs
	 * 
	 * @since 0.2.0
	 */
	protected void onUnknownHost(String host, SshPublicKey key) throws SshException {

	}

	/**
	 * Called by the <code>verifyHost</code> method when the host key supplied is
	 * listed as a revoked key. This is informational, any changes made to the
	 * current entries will still result in a failed host verification.
	 * 
	 * @param host
	 * @param key
	 * @throws SshException
	 */
	protected void onRevokedKey(String host, SshPublicKey key) {

	}

	/**
	 * <p>
	 * Removes an allowed host.
	 * </p>
	 * 
	 * @param host the host to remove
	 * @throws SshException
	 * 
	 * @since 0.2.0
	 */
	public synchronized void removeEntries(String host) throws SshException {

		List<KeyEntry> toRemove = new ArrayList<>();

		for (KeyEntry entry : getKeyEntries()) {
			if (entry.matchesHost(host)) {
				toRemove.add(entry);
			}
		}

		removeEntry(toRemove.toArray(new KeyEntry[0]));
	}

	public synchronized void removeEntries(String... hosts) throws SshException {
		for (String host : hosts) {
			removeEntries(host);
		}
	}

	public synchronized void removeEntries(SshPublicKey key) {

		List<KeyEntry> toRemove = entriesByPublicKey.get(key);
		removeEntry(toRemove.toArray(new KeyEntry[0]));
	}

	public synchronized void removeEntry(KeyEntry... keys) {

		List<KeyEntry> toRemove = Arrays.asList(keys);

		keyEntries.removeAll(toRemove);
		revokedEntries.removeAll(toRemove);
		entries.removeAll(toRemove);

		for (Map.Entry<SshPublicKey, List<KeyEntry>> entry : entriesByPublicKey.entrySet()) {
			entry.getValue().removeAll(toRemove);
		}

		certificateAuthorities.removeAll(toRemove);

		for (KeyEntry entry : keys) {
			onHostKeyRemoved(getNames(entry.getNames()), entry.getKey());
		}
	}

	protected void onHostKeyRemoved(Set<String> names, SshPublicKey key) {

	}

	public boolean isHostFileWriteable() {
		return true;
	}

	public void allowHost(String host, SshPublicKey key, boolean always) throws SshException {
		addEntry(key, "", always, resolveNames(host).toArray(new String[0]));
	}

	public synchronized void addEntry(SshPublicKey key, String comment, String... names) throws SshException {
		addEntry(key, comment, true, names);
	}

	public synchronized void addEntry(SshPublicKey key, String comment, boolean always, String... names) throws SshException {

		if (useHashHosts()) {
			for (String name : names) {
				addEntry(new Ssh2KeyEntry(new HashSet<String>(Arrays.asList(generateHash(name))), key, comment, !always));
			}
		} else {
			addEntry(new Ssh2KeyEntry(new HashSet<String>(Arrays.asList(names)), key, comment, !always));
		}
	}

	/**
	 * <p>
	 * Verifies a host key against the list of known_hosts.
	 * </p>
	 * 
	 * <p>
	 * If the host unknown or the key does not match the currently allowed host key
	 * the abstract <code>onUnknownHost</code> or <code>onHostKeyMismatch</code>
	 * methods are called so that the caller may identify and allow the host.
	 * </p>
	 * 
	 * @param host the name of the host
	 * @param pk   the host key supplied
	 * 
	 * @return true if the host is accepted, otherwise false
	 * 
	 * @throws SshException if an error occurs
	 * 
	 * @since 0.2.0
	 */
	public synchronized boolean verifyHost(String host, SshPublicKey pk) throws SshException {
		return verifyHost(host, pk, true);
	}

	protected synchronized boolean verifyHost(String host, SshPublicKey pk, boolean validateUnknown) throws SshException {

		Set<String> resolvedNames = resolveNames(host);

		for (KeyEntry entry : revokedEntries) {
			if (entry.validate(pk, resolvedNames.toArray(new String[0]))) {
				onRevokedKey(host, pk);
				return false;
			}
		}

		if (entriesByPublicKey.containsKey(pk)) {
			List<KeyEntry> keys = entriesByPublicKey.get(pk);
			for (KeyEntry entry : keys) {
				if (entry.validate(pk, resolvedNames.toArray(new String[0]))) {
					return true;
				}
			}
		}
		else {
			var allowed = new ArrayList<SshPublicKey>();
			for (Map.Entry<SshPublicKey, List<KeyEntry>> entry : entriesByPublicKey.entrySet()) {
				for(var k : entry.getValue()) {
					if(k.matchesHost(host)) {
						if (k.validate(entry.getKey(), resolvedNames.toArray(new String[0]))) {
							allowed.add(entry.getKey());
							break;
						}
					}
				}
			}
			if(!allowed.isEmpty()) {
				onHostKeyMismatch(host, allowed, pk);
				// Recheck ans return the result
				return verifyHost(host, pk, false);
			}
		}

		
		if (pk instanceof OpenSshCertificate) {
			for (CertAuthorityEntry ca : certificateAuthorities) {
				if (ca.validate(pk, resolvedNames.toArray(new String[0]))) {
					return true;
				}
			}
		}

		var existingKeys = new ArrayList<SshPublicKey>();
		for(KeyEntry k : keyEntries) {
			if(k.matchesHost(resolvedNames.toArray(new String[0]))) {
				if(k.getKey().equals(pk)) {
					return true;
				} else {
					existingKeys.add(k.getKey());
				}
			}
		}
		
		if(existingKeys.size() > 0) {
			onHostKeyMismatch(host, null, pk);
		} else {

			// The host is unknown os ask the user
			if (!validateUnknown)
				return false;
	
			onUnknownHost(host, pk);
		}

// Recheck ans return the result
		return verifyHost(host, pk, false);

	}

	protected Set<String> resolveNames(String host) {

		String fqn = null;
		String ip = null;

		String resolveHost = host;

		Set<String> resolvedNames = new LinkedHashSet<String>();
		resolvedNames.add(host);

		Matcher m = nonStandard.matcher(host);
		boolean nonStandardPorts = m.matches();
		if (nonStandardPorts) {
			resolveHost = m.group(1);
		}

		if (useCanonicalHostname() || useReverseDNS()) {
			try {
				InetAddress addr = InetAddress.getByName(resolveHost);

				if (useCanonicalHostname()) {
					if (nonStandardPorts) {
						fqn = String.format("[%s]:%s", addr.getHostName(), m.group(2));
					} else {
						fqn = addr.getHostName();
					}
					resolvedNames.add(fqn);
				}
				if (useReverseDNS()) {
					if (nonStandardPorts) {
						ip = String.format("[%s]:%s", addr.getHostAddress(), m.group(2));
					} else {
						ip = addr.getHostAddress();
					}
					resolvedNames.add(ip);
				}

			} catch (UnknownHostException ex) {
				// Just record the host as the user typed it
			}
		}

		return resolvedNames;
	}

	public boolean useCanonicalHostname() {
		return useCanonicalHostname;
	}

	public boolean useReverseDNS() {
		return useReverseDNS;
	}

	public boolean useHashHosts() {
		return hashHosts;
	}

	private boolean checkHash(String name, String resolvedName) throws SshException {
		SshHmac sha1 = (SshHmac) ComponentManager.getInstance().supportedHMacsCS().getInstance("hmac-sha1");
		String hashData = name.substring(HASH_MAGIC.length());
		String hashSalt = hashData.substring(0, hashData.indexOf(HASH_DELIM));
		String hashStr = hashData.substring(hashData.indexOf(HASH_DELIM) + 1);

		byte[] theHash = Base64.decode(hashStr);

		sha1.init(Base64.decode(hashSalt));
		sha1.update(resolvedName.getBytes());

		byte[] ourHash = sha1.doFinal();

		return Arrays.equals(theHash, ourHash);
	}

	private String generateHash(String host) throws SshException {
		SshHmac sha1 = (SshHmac) ComponentManager.getInstance().supportedHMacsCS().getInstance("hmac-sha1");
		byte[] hashSalt = new byte[sha1.getMacLength()];
		ComponentManager.getInstance().getRND().nextBytes(hashSalt);

		sha1.init(hashSalt);
		sha1.update(host.getBytes());

		byte[] theHash = sha1.doFinal();

		return HASH_MAGIC + Base64.encodeBytes(hashSalt, false) + HASH_DELIM + Base64.encodeBytes(theHash, false);
	}

	/**
	 * <p>
	 * Outputs the allowed hosts in the known_hosts file format.
	 * </p>
	 * 
	 * <p>
	 * The format consists of any number of lines each representing one key for a
	 * single host.
	 * </p>
	 * <code> titan,192.168.1.12 ssh-dss AAAAB3NzaC1kc3MAAACBAP1/U4Ed.....
	* titan,192.168.1.12 ssh-rsa AAAAB3NzaC1kc3MAAACBAP1/U4Ed.....
	* einstein,192.168.1.40 ssh-dss AAAAB3NzaC1kc3MAAACBAP1/U4Ed..... </code>
	 * 
	 * @return String
	 * 
	 * @since 0.2.0
	 */

	public synchronized String toString() {

		StringBuffer buf = new StringBuffer("");
		for (HostFileEntry entry : entries) {
			if(!entry.temporary) {
				buf.append(entry.getFormattedLine());
				buf.append(System.getProperty("line.separator"));
			}
		}
		return buf.toString();
	}

	public abstract class HostFileEntry {
		
		boolean temporary;

		abstract String getFormattedLine();

		abstract boolean canValidate();

		abstract boolean validate(SshPublicKey key, String... resolvedNames) throws SshException;

	}

	public abstract class KeyEntry extends HostFileEntry {

		String comment;
		Set<String> names;
		SshPublicKey key;
		boolean hashedEntry = false;

		KeyEntry(Set<String> names, SshPublicKey key, String comment, boolean temporary) {
			this.names = names;
			this.temporary = temporary;
			this.key = key;
			this.comment = comment;
			if (names.size() == 1) {
				if (names.iterator().next().startsWith(HASH_DELIM)) {
					hashedEntry = true;
				}
			}
		}

		public boolean isHashedEntry() {
			return hashedEntry;
		}

		public SshPublicKey getKey() {
			return key;
		}

		public String getNames() {
			StringBuffer buf = new StringBuffer();
			for (String name : names) {
				if (buf.length() > 0) {
					buf.append(",");
				}
				buf.append(name);
			}
			return buf.toString();
		}

		boolean matchesHash(String name, String... resolvedNames) throws SshException {
			for (String resolvedName : resolvedNames) {
				if (checkHash(name, resolvedName)) {
					return true;
				}
			}
			return false;
		}

		boolean matchesHost(String... resolvedNames) throws SshException {

			boolean success = true;
			boolean matched = false;

			for (String name : names) {

				if (name.startsWith(HASH_MAGIC)) {
					return matchesHash(name, resolvedNames);
				} else {
					if (name.startsWith("!")) {
						if (matches(name.substring(1), resolvedNames)) {
							success = false;
							matched = true;
						}
					} else {
						if (matches(name, resolvedNames)) {
							matched = true;
						}
					}
				}
			}

			if (matched) {
				return success;
			}
			return false;
		}

		@Override
		boolean canValidate() {
			return true;
		}

		@Override
		boolean validate(SshPublicKey key, String... resolvedNames) throws SshException {
			if (matchesHost(resolvedNames)) {
				return key.equals(this.key);
			}
			return false;
		}

		boolean matches(String name, String... resolvedNames) {

			// First escape any dots
			name = name.replace(".", "\\.");
			name = name.replace("[", "\\[");
			name = name.replace("]", "\\]");

			if (name.contains("*")) {
				name = name.replace("*", ".*");
			}

			if (name.contains("?")) {
				name = name.replace("?", ".");
			}

			for (String resolvedName : resolvedNames) {
				if (resolvedName.matches(name)) {
					return true;
				}
			}

			return false;
		}

		public String getComment() {
			return comment;
		}

		public boolean isRevoked() {
			return false;
		}

		public boolean isCertAuthority() {
			return false;
		}
	}

	class Ssh1KeyEntry extends HostFileEntry {
		String line;
		Ssh1KeyEntry(String line) {
			this.line = line;
		}

		@Override
		String getFormattedLine() {
			return line;
		}

		@Override
		boolean canValidate() {
			return false;
		}

		@Override
		boolean validate(SshPublicKey key, String... resolvedNames) throws SshException {
			return false;
		}
	}

	public class Ssh2KeyEntry extends KeyEntry {

		boolean hashedEntry = false;

		Ssh2KeyEntry(Set<String> names, SshPublicKey key, String comment, boolean temporary) {
			super(names, key, comment, temporary);
			if (names.size() == 1) {
				if (names.iterator().next().startsWith(HASH_DELIM)) {
					hashedEntry = true;
				}
			}
		}

		public boolean isHashedEntry() {
			return hashedEntry;
		}

		@Override
		String getFormattedLine() {
			try {
				return String.format("%s %s", getNames(), SshKeyUtils.getFormattedKey(key, comment)).trim();
			} catch (IOException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}

	}

	public class CertAuthorityEntry extends KeyEntry {

		CertAuthorityEntry(Set<String> names, SshPublicKey key, String comment) {
			super(names, key, comment, false);
		}

		@Override
		String getFormattedLine() {
			try {
				return String.format("@cert-authority %s %s", getNames(), SshKeyUtils.getFormattedKey(key, comment));
			} catch (IOException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}

		@Override
		boolean canValidate() {
			return true;
		}

		@Override
		boolean validate(SshPublicKey key, String... resolvedNames) throws SshException {
			if (matchesHost(resolvedNames)) {
				if (key instanceof OpenSshCertificate) {
					return ((OpenSshCertificate) key).getSignedBy().equals(this.key);
				}
			}
			return false;
		}

		@Override
		public final boolean isCertAuthority() {
			return true;
		}
	}

	public class RevokedEntry extends KeyEntry {

		KeyEntry revokedEntry;

		RevokedEntry(Set<String> names, KeyEntry revokedEntry) {
			super(names, revokedEntry.getKey(), revokedEntry.getComment(), false);
			this.revokedEntry = revokedEntry;
		}

		@Override
		String getFormattedLine() {
			return String.format("@revoked %s", revokedEntry.getFormattedLine());
		}

		@Override
		boolean canValidate() {
			return true;
		}

		@Override
		public final boolean isRevoked() {
			return true;
		}

	}

	public class CommentEntry extends NonValidatingFileEntry {

		String comment;

		CommentEntry(String comment) {
			this.comment = comment;
		}

		@Override
		String getFormattedLine() {
			return String.format("#%s", comment);
		}
	}

	public class InvalidEntry extends NonValidatingFileEntry {

		String line;

		InvalidEntry(String line) {
			this.line = line;
		}

		@Override
		String getFormattedLine() {
			return line;
		}
	}

	public class BlankEntry extends NonValidatingFileEntry {

		@Override
		String getFormattedLine() {
			return "";
		}
	}

	abstract class NonValidatingFileEntry extends HostFileEntry {

		@Override
		boolean canValidate() {
			return false;
		}

		@Override
		boolean validate(SshPublicKey key, String... resolvedNames) throws SshException {
			throw new UnsupportedOperationException();
		}

	}

	public void setUseCanonicalHostnames(boolean value) {
		this.useCanonicalHostname = value;
	}

	public void setUseReverseDNS(boolean value) {
		this.useReverseDNS = value;
	}

	public Set<KeyEntry> getKeyEntries() {
		return keyEntries;
	}

	@Override
	public boolean isKnownHost(String host, SshPublicKey key) throws SshException {
		return verifyHost(host, key, false);
	}

	@Override
	public void updateHostKey(String host, SshPublicKey key) throws SshException {

		KeyEntry existingEntry = null;
		Set<String> names = resolveNames(host);

		for (KeyEntry e : getKeyEntries()) {
			if (e.isHashedEntry()) {
				if (e.matchesHash(e.getNames(), names.toArray(new String[0]))) {
					existingEntry = e;
				}
			} else if (e.matchesHost(names.toArray(new String[0]))) {
				existingEntry = e;
			}
		}

		if (existingEntry != null) {
			removeEntries(host);
		}

		addEntry(key, "", names.toArray(new String[0]));

		if (existingEntry != null) {
			onHostKeyUpdated(names, key);
		}
	}

	protected void onHostKeyUpdated(Set<String> names, SshPublicKey key) {

	}

}
