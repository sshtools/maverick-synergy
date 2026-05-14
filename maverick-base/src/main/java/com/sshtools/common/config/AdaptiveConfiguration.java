/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */
package com.sshtools.common.config;

/*-
 * #%L
 * Base API
 * %%
 * Copyright (C) 2002 - 2026 JADAPTIVE Limited
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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SecureComponent;
import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.ComponentFactory;
import com.sshtools.common.util.IOUtils;
import com.sshtools.common.util.Utils;

public class AdaptiveConfiguration {

	public static final String KEY_EXCHANGE = "Kex";
	public static final String PUBLIC_KEYS = "Publickeys";
	public static final String CIPHERS = "Ciphers";
	public static final String MACS = "Macs";
	public static final String COMPRESSION = "Compressions";
	public static final String SECURITY_LEVEL = "SecurityLevel";
	public static final String MANAGED_SECURITY = "ManagedSecurity";
	public static final String DROP_SECURITY_AS_LAST_RESORT = "DropSecurityAsLastResort";
	public static final String GATEWAY_FORWARDING = "GatewayPorts";
	public static final String ALLOW_FORWARDING = "AllowTcpForwarding";
	public static final String PERMITTED_FORWARDING = "PermitOpen";
	public static final String HOST_KEY = "HostKey";
	public static final String PORT = "Port";
	public static final String LISTEN_ADDRESS = "ListenAddress";
	public static final String BANNER = "Banner";
	public static final String USER = "User";
	public static final String PASSWORD_AUTHENTICATION = "PasswordAuthentication";
	public static final String PUBKEY_AUTHENTICATION = "PubkeyAuthentication";
	public static final String ENABLE_SCP = "EnableScp";
	public static final String REQUIRED_AUTHENTICATION = "AuthenticationMethods";

	private static final Map<String, AdaptiveConfiguration> instances = new ConcurrentHashMap<>();
	private static final Set<String> multipleConfigKeys = new HashSet<>(Arrays.asList(HOST_KEY, PORT, LISTEN_ADDRESS));

	private Map<String, String> globalConfig = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	private Map<String, String[]> multipleConfig = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	private Map<String, Map<String, String>> patternConfigs = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	private File configFile;
	private Map<String, String> cachedValues = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

	private AdaptiveConfiguration(String filename) throws IOException, SshException {
		this.configFile = new File(filename);
		resetConfiguration();
	}
	
	private AdaptiveConfiguration(Path path) throws IOException, SshException {
		this.configFile = path.toFile();
		resetConfiguration();
	}

	public static AdaptiveConfiguration getConfiguration(String filename) throws IOException, SshException {
		return instances.computeIfAbsent(filename, f -> {
			try {
				return new AdaptiveConfiguration(f);
			} catch (IOException | SshException e) {
				Log.error("Failed to initialize AdaptiveConfiguration for " + f, e);
				throw new RuntimeException(e);
			}
		});
	}
	
	public static AdaptiveConfiguration getConfiguration(Path path) throws IOException, SshException {
		return instances.computeIfAbsent(path.toAbsolutePath().toString(), f -> {
			try {
				return new AdaptiveConfiguration(f);
			} catch (IOException | SshException e) {
				Log.error("Failed to initialize AdaptiveConfiguration for " + f, e);
				throw new RuntimeException(e);
			}
		});
	}

	public void resetConfiguration() throws IOException, SshException {
		globalConfig = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		patternConfigs = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		cachedValues = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

		if (configFile.exists()) {
			try (InputStream in = new FileInputStream(configFile)) {
				loadConfiguration(in);
			}
		}
	}

	private String generateCacheKey(String key, String... matches) {
		if (matches.length > 0) {
			StringBuilder buf = new StringBuilder();
			buf.append(key);
			buf.append("|");
			buf.append(Utils.csv(matches));
			return buf.toString();
		}
		return key;
	}

	private String cacheValue(String key, String value, String... matches) {
		cachedValues.put(generateCacheKey(key, matches), value);
		return value;
	}

	public void saveMatchingConfiguration(String match, String keyexchange, String publickey, String cipher, String mac, String compression) throws IOException {
		if (getBoolean("LastKnownGoodConfiguration", false, match)) {
			setPatternConfig(match, KEY_EXCHANGE, keyexchange);
			setPatternConfig(match, PUBLIC_KEYS, publickey);
			setPatternConfig(match, CIPHERS, cipher);
			setPatternConfig(match, MACS, mac);
			setPatternConfig(match, COMPRESSION, compression);
			saveConfig();
		}
	}

	public void saveConfig() throws IOException {
		StringWriter writer = new StringWriter();
		for (Map.Entry<String, String> entry : globalConfig.entrySet()) {
			writer.write(entry.getKey() + " " + entry.getValue() + System.lineSeparator());
		}
		writer.write(System.lineSeparator());
		for (Map.Entry<String, Map<String, String>> entry : patternConfigs.entrySet()) {
			writer.write("Match " + entry.getKey() + System.lineSeparator());
			for (Map.Entry<String, String> patternEntry : entry.getValue().entrySet()) {
				writer.write(" " + patternEntry.getKey() + " " + patternEntry.getValue() + System.lineSeparator());
			}
			writer.write(System.lineSeparator());
		}
		IOUtils.writeStringToFile(configFile, writer.toString(), "UTF-8");
	}

	public void loadConfiguration(String config) throws IOException {
		loadConfiguration(IOUtils.toInputStream(config, "UTF-8"));
	}

	public void loadConfiguration(String config, String charset) throws IOException {
		loadConfiguration(IOUtils.toInputStream(config, charset));
	}

	public void loadConfiguration(InputStream in) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.length() > 0) {
					if (line.toLowerCase().startsWith("match ")) {
						break;
					}
					String key = before(line);
					if (!key.startsWith("#") && Utils.isNotBlank(key)) {
						String value = after(line);
						setGlobalConfig(key, value);
					}
				}
			}
			while (line != null && line.toLowerCase().startsWith("match ")) {
				String matchValue = after(line);
				while ((line = reader.readLine()) != null) {
					if (line.toLowerCase().startsWith("match ")) {
						break;
					}
					line = line.trim();
					String key = before(line);
					if (!key.startsWith("#") && Utils.isNotBlank(key)) {
						String value = after(line);
						setPatternConfig(matchValue, key, value);
					}
				}
			}
		}
	}

	private String before(String str) {
		String[] vals = str.trim().split("\\s+", 2);
		if (vals.length > 0) {
			return vals[0];
		}
		throw new IllegalArgumentException(str + " does not contain elements separated by whitespace");
	}

	private String after(String str) {
		String[] vals = str.trim().split("\\s+", 2);
		if (vals.length > 1) {
			return vals[1];
		}
		throw new IllegalArgumentException(str + " does not contain elements separated by whitespace");
	}

	public String createAlgorithmList(String supportedList, String key, String ident, String hostname, String... ignores) {
		List<String> supported = Arrays.asList(supportedList.split(","));
		String locallist = getPatternConfig(key, hostname, hostname);
		if (Utils.isBlank(locallist)) {
			locallist = getGlobalConfig(key);
		}
		if (Utils.isBlank(locallist)) {
			locallist = supportedList;
		}
		List<String> ignoreAlgs = Arrays.asList(ignores);
		List<String> results = new ArrayList<>();
		for (String algorithm : locallist.split(",")) {
			if (supported.contains(algorithm) && !ignoreAlgs.contains(algorithm)) {
				results.add(algorithm);
			}
		}
		return Utils.csv(results);
	}

	public String createAlgorithmList(ComponentFactory<?> factory, String key, String contextPreference, String ident, String hostname, String... ignores) {
		String locallist = factory.filter(getPatternConfig(key, hostname, ident));
		if (Utils.isBlank(locallist)) {
			locallist = factory.filter(getGlobalConfig(key));
		}
		if (Utils.isBlank(locallist)) {
			locallist = factory.list(contextPreference);
		}
		List<String> ignoreAlgs = Arrays.asList(ignores);
		List<String> results = new ArrayList<>();
		for (String algorithm : locallist.split(",")) {
			if (!ignoreAlgs.contains(algorithm)) {
				results.add(algorithm);
			}
		}
		return Utils.csv(results);
	}

	public String getPatternConfig(String key, String... values) {
		String cachedValue = cachedValues.get(generateCacheKey(key, values));
		if (cachedValue != null) {
			return cachedValue;
		}
		if (values.length > 0) {
			for (String value : values) {
				for (String pattern : patternConfigs.keySet()) {
					if (value.matches(pattern)) {
						String result = patternConfigs.get(pattern).get(key);
						if (result != null) {
							if (Log.isDebugEnabled()) {
								Log.debug(String.format("Matched %s from pattern configuration %s [%s] with value %s", key, value, pattern, result));
							}
							return cacheValue(key, result, values);
						}
					}
				}
				String result = getSystemProperty(formatKey(value, key));
				if (result != null) {
					return result;
				}
			}
		}
		return getGlobalConfig(key);
	}

	private String formatKey(String key1, String key2) {
		return key1 + "." + key2;
	}

	private String getSystemProperty(String key) {
		String result = System.getProperty(key);
		if (result != null && Log.isDebugEnabled()) {
			Log.debug(String.format("Matched %s from system property with value %s", key, result));
		}
		return result;
	}

	public void clearPatternConfig(String pattern) {
		patternConfigs.remove(pattern);
	}

	public void parsePatternConfig(String pattern, String config) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(IOUtils.toInputStream(config, "UTF-8")))) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.length() > 0) {
					if (line.toLowerCase().startsWith("match ")) {
						throw new IllegalStateException("Match directive not allowed in parsePatternConfig!");
					}
					String key = before(line);
					if (!key.startsWith("#") && Utils.isNotBlank(key)) {
						String value = after(line);
						setPatternConfig(pattern, key, value);
					}
				}
			}
		}
	}

	public void setPatternConfig(String pattern, String key, String val) {
		patternConfigs.computeIfAbsent(pattern, k -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER)).put(key, val);
	}

	public void setPatternConfig(String pattern, String key, boolean val) {
		setPatternConfig(pattern, key, String.valueOf(val));
	}

	public void setPatternConfig(String pattern, String key, int val) {
		setPatternConfig(pattern, key, String.valueOf(val));
	}

	public void setPatternConfig(String pattern, String key, long val) {
		setPatternConfig(pattern, key, String.valueOf(val));
	}

	public String getGlobalConfig(String key) {
		String result = globalConfig.get(key);
		if (result != null) {
			if (Log.isDebugEnabled()) {
				Log.debug(String.format("Matched %s from global configuration with value %s", key, result));
			}
			return result;
		}
		return getSystemProperty(formatKey("maverick", key));
	}

	public String[] getMultipleConfig(String key) {
		String[] result = multipleConfig.get(key);
		if (result != null) {
			if (Log.isDebugEnabled()) {
				Log.debug(String.format("Matched %s from global configuration with values %s", key, Utils.csv(result)));
			}
			return result;
		}
		String tmp = getSystemProperty(formatKey("maverick", key));
		return (tmp != null) ? tmp.split(",") : new String[0];
	}

	public int[] getMultipleIntConfig(String key) {
		String[] result = getMultipleConfig(key);
		return convertToIntArray(result);
	}

	private int[] convertToIntArray(String[] result) {
		int[] tmp = new int[result.length];
		for (int i = 0; i < result.length; i++) {
			tmp[i] = Integer.parseInt(result[i]);
		}
		return tmp;
	}

	public void setGlobalConfig(String key, String val) {
		if (multipleConfigKeys.contains(key)) {
			addMultipleConfig(key, val);
		} else {
			globalConfig.put(key, val);
		}
	}

	private void addMultipleConfig(String key, String val) {
		List<String> v = new ArrayList<>(Arrays.asList(multipleConfig.getOrDefault(key, new String[0])));
		v.add(val);
		multipleConfig.put(key, v.toArray(new String[0]));
	}

	public void setGlobalConfig(String key, int val) {
		setGlobalConfig(key, String.valueOf(val));
	}

	public void setGlobalConfig(String key, long val) {
		setGlobalConfig(key, String.valueOf(val));
	}

	public void setGlobalConfig(String key, boolean value) {
		setGlobalConfig(key, String.valueOf(value));
	}

	public static String getIdent(String remoteIdentification) {
		if (remoteIdentification.startsWith("SSH")) {
			String[] elements = remoteIdentification.split("-");
			if (elements.length >= 3) {
				String ident = elements[2];
				int idx = ident.indexOf(' ');
				return (idx > -1) ? ident.substring(0, idx) : ident;
			}
		}
		Log.error("Remote identification cannot be parsed to capture the remote nodes identity [{}]", remoteIdentification);
		return "<unknown>";
	}

	public boolean getBoolean(String key, boolean defaultValue, String... match) {
		String result = getPatternConfig(key, match);
		return (result == null) ? getBooleanOrDefault(key, defaultValue) : parseBoolean(result);
	}

	private boolean parseBoolean(String val) {
		String upperVal = val.toUpperCase();
		return "YES".equals(upperVal) || "Y".equals(upperVal) || "TRUE".equals(upperVal) || "ON".equals(upperVal);
	}

	public void setBoolean(String key, String pattern) {
		setPatternConfig(pattern, key, Boolean.TRUE.toString());
	}

	public void setBoolean(String key, String pattern, Boolean val) {
		setPatternConfig(pattern, key, val.toString());
	}

	public boolean getBooleanOrDefault(String key, boolean defaultValue) {
		String result = getGlobalConfig(key);
		return (result != null) ? parseBoolean(result) : defaultValue;
	}

	public long getLong(String key, Long defaultValue, String... match) {
		String result = getPatternConfig(key, match);
		return (result == null) ? getLongOrDefault(key, defaultValue) : Long.parseLong(result);
	}

	private long getLongOrDefault(String key, long defaultValue) {
		String result = getGlobalConfig(key);
		return (result != null) ? Long.parseLong(result) : defaultValue;
	}

	public int getInt(String key, int defaultValue, String... match) {
		String result = getPatternConfig(key, match);
		return (result == null) ? getIntOrDefault(key, defaultValue) : Integer.parseInt(result);
	}

	private int getIntOrDefault(String key, int defaultValue) {
		String result = getGlobalConfig(key);
		return (result != null) ? Integer.parseInt(result) : defaultValue;
	}

	public long getByteSize(String key, String defaultValue, String... match) {
		String result = getPatternConfig(key, match);
		return (result != null) ? IOUtils.fromByteSize(result) : getByteSizeOrDefault(key, defaultValue);
	}

	private long getByteSizeOrDefault(String key, String defaultValue) {
		String result = getGlobalConfig(key);
		return (result != null) ? IOUtils.fromByteSize(result) : IOUtils.fromByteSize(defaultValue);
	}

	public String getProperty(String key, String defaultValue, String... match) {
		String result = getPatternConfig(key, match);
		if (result != null) {
			return result;
		}
		result = getGlobalConfig(key);
		return (result == null) ? defaultValue : result;
	}

	public String getSecurityLevel(SecurityLevel securityLevel, String configurationDirective, ComponentFactory<?> factory) throws SshException {
		if (securityLevel == SecurityLevel.NONE) {
			throw new IllegalArgumentException("SecurityLevel.NONE cannot be used as a preferred security level!");
		}
		StringBuilder buf = new StringBuilder();
		for (int i = SecurityLevel.PARANOID.ordinal(); i >= securityLevel.ordinal(); i--) {
			if (buf.length() > 0) {
				buf.append(",");
			}
			SecurityLevel tmp = SecurityLevel.values()[i];
			String value = getPatternConfig(configurationDirective, tmp.name());
			buf.append(Utils.isNotBlank(value) ? value : csv(factory, tmp));
		}
		return buf.toString();
	}

	public String csv(ComponentFactory<?> algs, SecurityLevel level) throws SshException {
		StringBuilder buf = new StringBuilder();
		for (String alg : algs.names()) {
			try {
				SecureComponent c = (SecureComponent) algs.getInstance(alg);
				if (c.getSecurityLevel() == level) {
					if (buf.length() > 0) {
						buf.append(",");
					}
					buf.append(alg);
				}
			} catch (Throwable t) {
				// Intentionally ignored
			}
		}
		return buf.toString();
	}

	public SecurityLevel getSecurityLevel() {
		return SecurityLevel.valueOf(getProperty(SECURITY_LEVEL, SecurityLevel.STRONG.name()));
	}
}
