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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.components.ComponentFactory;
import com.sshtools.common.util.IOUtils;
import com.sshtools.common.util.Utils;

public class AdaptiveConfiguration {
	
	public final static AdaptiveConfiguration DEFAULT;

	public static final String KEY_EXCHANGE = "Kex";
	public static final String PUBLIC_KEYS = "Publickeys";
	public static final String CIPHERS = "Ciphers";
	public static final String MACS = "Macs";
	public static final String COMPRESSION = "Compressions";

	private Map<String,String> globalConfig = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	private Map<String,Map<String,String>> patternConfigs = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	private final File configFile;
	private final File configDir;
	
	static {
		DEFAULT = new AdaptiveConfiguration(new File(System.getProperty("maverick.configFile", "maverick.cfg")),
				new File(System.getProperty("maverick.configDir", "conf.d")), false);
		try {
			DEFAULT.resetConfiguration();
		} catch (IOException e) {
			Log.error("Failed to initialize AdaptiveConfiguration", e);
		}
	}

	public AdaptiveConfiguration(File configFile, File configDir) {
		this(configFile, configDir, true);
	}
	
	private AdaptiveConfiguration(File configFile, File configDir, boolean load) {
		this.configFile = configFile;
		this.configDir = configDir;
		if(load) {
			try {
				resetConfiguration();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}
	
	
	public void resetConfiguration() throws IOException {
		globalConfig = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		patternConfigs = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

		if(configFile.exists()) {
			loadConfigurationFile(configFile);
		}
		
		if(configDir.exists()) {
			for(File file : configDir.listFiles(new FileFilter() {

				@Override
				public boolean accept(File path) {
					return path.getName().endsWith(".cfg");
				}
				
			})) {
				loadConfigurationFile(file);
			}
		}
	}
	
	private void loadConfigurationFile(File file) throws IOException {
		try(InputStream in = new FileInputStream(file)) {
			loadConfiguration(in);
		} 
	}
	
	public void saveMatchingConfiguration(String match, String keyexchange, String publickey, String cipher, String mac, String compression) throws IOException {
		
		if(getBoolean("LastKnownGoodConfiguration", false, match)) {
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
		
		for(String key : globalConfig.keySet()) {
			writer.write(key);
			writer.write(" ");
			writer.write(globalConfig.get(key));
			writer.write(System.lineSeparator());
		}
		
		writer.write(System.lineSeparator());
		
		for(String key : patternConfigs.keySet()) {
			writer.write("Match ");
			writer.write(key);
			writer.write(System.lineSeparator());
			
			Map<String,String> pattern = patternConfigs.get(key);
			for(String k : pattern.keySet()) {
				writer.write(" ");
				writer.write(k);
				writer.write(" ");
				writer.write(pattern.get(k));
				writer.write(System.lineSeparator());
			}
			
			writer.write(System.lineSeparator());
		}
		
		IOUtils.writeStringToFile(configFile, writer.toString(), "UTF-8");
	}
	
	public void loadConfiguration(InputStream in) throws IOException {
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		try {
			String line;
			while((line = reader.readLine()) != null) {
				line = line.trim();
				if(line.length() > 0) {
					if(line.toLowerCase().startsWith("match ")) {
						break;
					}
					String key = before(line);
					if(key.startsWith("#")) { 
						continue;
					} else if(Utils.isNotBlank(key)) {
						String value = after(line);
						setGlobalConfig(key, value);
					}
				}
			}
			
			while(line!=null && line.toLowerCase().startsWith("match ")) {
				String matchValue = after(line);
				while((line = reader.readLine())!=null) {
					if(line.toLowerCase().startsWith("match ")) {
						break;
					}
					line = line.trim();
					String key = before(line);
					
					if(!key.startsWith("#") && Utils.isNotBlank(key)) {
						String value = after(line);
						setPatternConfig(matchValue, key, value);
					}
				}
			}
		} finally {
			reader.close();
		}
	}
	
	private String before(String str) {
		
		String[] vals = str.trim().split("\\s+");
		if(vals.length > 0) {
			return vals[0];
		}
		throw new IllegalArgumentException(str + " does not contain elements separated by whitespace");
	}
	
	private String after(String str) {
		String key = before(str);
		int idx = str.indexOf(key);
		
		if(idx >  -1) {
			return str.substring(idx+key.length()).trim();
		}
		throw new IllegalArgumentException(str + " does not contain elements separated by whitespace");
	}
	
	public String createAlgorithmList(String supportedList, String key,
			 String ident, String hostname, String...ignores) {
		
		List<String> supported = Arrays.asList(supportedList.split("."));
		
		String locallist = getPatternConfig(key, hostname, hostname);
		
		if(Utils.isBlank(locallist)) {
			locallist = getGlobalConfig(key);
		}
		
		if(Utils.isBlank(locallist)) {
			return supportedList;
		}
		
		List<String> results = new ArrayList<>();
		for(String algorithm : locallist.split(",")) {
			if(supported.contains(algorithm)) {
				results.add(algorithm);
			}
		}
		
		return Utils.csv(results);
	}
	
	public String createAlgorithmList(ComponentFactory<?> factory, String key, String contextPreference, String ident, String hostname, String...ignores) {
		
		String locallist = factory.filter(getPatternConfig(key, hostname, ident));
		
		if(Utils.isBlank(locallist)) {
			locallist = factory.filter(getGlobalConfig(key));
		}
		
		if(Utils.isBlank(locallist)) {
			locallist = factory.list(contextPreference, ignores);
		}
		
		return locallist;
	}
	
	public String getPatternConfig(String key, String... values) {
		for(String value : values) {
			if(Utils.isNotBlank(value)) {
				for(String pattern : patternConfigs.keySet()) {
					if(value.matches(pattern)) {
						String result = patternConfigs.get(pattern).get(key);
						if(result!=null) {
							if(Log.isDebugEnabled()) {
								Log.debug("Matched {} from pattern configuration {} [{}] with value {}", key, value, pattern, result);
							}
							return result;
						}
					}
				}
				String result = getSystemProperty(formatKey(value, key));
				if(result!=null) {
					return result;
				}
			}
		}
		
		return getGlobalConfig(key);
	}
	
	private String formatKey(String key1, String key2) {
		StringBuilder str = new StringBuilder();
		str.append(key1);
		str.append(".");
		str.append(key2);
		return str.toString();
	}
	
	private String getSystemProperty(String key) {
		String result = System.getProperty(key);
		if(result!=null) {
			if(Log.isDebugEnabled()) {
				Log.debug("Matched {} from system property with value {}", key, result);
			}
		}
		return result;
	}
	
	public void setPatternConfig(String pattern, String key, String val) {
		if(!patternConfigs.containsKey(pattern)) {
			patternConfigs.put(pattern, new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER));
		}
		patternConfigs.get(pattern).put(key,  val);
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
		if(result!=null) {
			if(Log.isDebugEnabled()) {
				Log.debug("Matched {} from global configuration with value {}", key, result);
			}
			return result;
		}
		return getSystemProperty(formatKey("maverick", key));
	}
	
	public void setGlobalConfig(String key, String val) {
		globalConfig.put(key,  val);
	}
	
	public void setGlobalConfig(String key, int val) {
		globalConfig.put(key,  String.valueOf(val));
	}
	
	public void setGlobalConfig(String key, long val) {
		globalConfig.put(key,  String.valueOf(val));
	}
	
	public void setGlobalConfig(String key, boolean value) {
		setGlobalConfig(key, String.valueOf(value));
	}
	
	public String getIdent(String remoteIdentification) {
		if(remoteIdentification.startsWith("SSH")) {
			String[] elements = remoteIdentification.split("-");
			if(elements.length == 3) {
				return elements[2].trim();
			} else if(elements.length > 3) {
				String ident = elements[2];
				int idx = ident.indexOf(' ');
				if(idx > -1) {
					ident = ident.substring(0, idx);
				}
				return ident;
			}
		}
		
		Log.error("Remote identification cannot be parsed to capture the remote nodes identity [{}]", remoteIdentification);
		return "<unknown>";
		

	}

	public boolean getBoolean(String key, boolean defaultValue, String... match) {
		
		String result = getPatternConfig(key, match);
		if(result==null) {
			return getBooleanOrDefault(key, defaultValue);
		}
		return parseBoolean(result);
	}

	private boolean parseBoolean(String val) {
		switch(val.toUpperCase()) {
		case "YES":
		case "Y":
		case "TRUE":
			return true;
		default:
			return false;			
		}
	}

	public void setBoolean(String key, String pattern) {
		setPatternConfig(pattern, key, Boolean.TRUE.toString());
	}
	
	public void setBoolean(String key, String pattern, Boolean val) {
		setPatternConfig(pattern, key, val.toString());
	}

	public boolean getBooleanOrDefault(String key, boolean defaultValue) {
		String result = getGlobalConfig(key);
		if(result!=null) {
			return parseBoolean(result);
		}
		return defaultValue;
	}
	
	public long getLong(String key, Long defaultValue, String... match) {
		String result = getPatternConfig(key, match);
		if(result==null) {
			return getLongOrDefault(key, defaultValue);
		}
		return Long.parseLong(result);
	}

	private long getLongOrDefault(String key, long defaultValue) {
		String result = getGlobalConfig(key);
		if(result!=null) {
			return Long.parseLong(result);
		}
		return defaultValue;
	}
	
	public int getInt(String key, int defaultValue, String... match) {
		String result = getPatternConfig(key, match);
		if(result==null) {
			return getIntOrDefault(key, defaultValue);
		}
		return Integer.parseInt(result);
	}
	
	private int getIntOrDefault(String key, int defaultValue) {
		String result = getGlobalConfig(key);
		if(result!=null) {
			return Integer.parseInt(result);
		}
		return defaultValue;
	}
	
	public long getByteSize(String key, String defaultValue, String... match) {
		
		String result = getPatternConfig(key, match);
		if(result!=null) {
			return IOUtils.fromByteSize(result);
		}
		return getByteSizeOrDefault(key, defaultValue);
	}
	
	private long getByteSizeOrDefault(String key, String defaultValue) {
		String result = getGlobalConfig(key);
		if(result!=null) {
			return IOUtils.fromByteSize(result);
		}
		return IOUtils.fromByteSize(defaultValue);
	}

	public String getProperty(String key, String defaultValue, String... match) {
		String result = getPatternConfig(key, match);
		if(result!=null) {
			return result;
		}
		result = getGlobalConfig(key);
		if(result == null) {
			return defaultValue;
		}
		return result;
	}
}
