/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.common.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.components.ComponentFactory;
import com.sshtools.common.util.IOUtils;
import com.sshtools.common.util.Utils;

public class AdaptiveConfiguration {

	public static final String KEY_EXCHANGE = "kex";
	public static final String PUBLIC_KEYS = "publickeys";
	public static final String CIPHERS = "ciphers";
	public static final String MACS = "macs";
	public static final String COMPRESSION = "compressions";
	
	public static final String SCP_BUFFER_SIZE = "scp.bufferSize";
	
	public static final String SFTP_MAX_WINDOW_SPACE = "sftp.maxWindowSpace";
	public static final String SFTP_MIN_WINDOW_SPACE = "sftp.minWindowSpace";
	
	public static final String SESSION_MAX_WINDOW_SPACE = "ssh.maxWindowSpace";
	
	public static final String SOCKET_OPTION_KEEP_ALIVE = "tcp.keepAlive";
	public static final String SOCKET_OPTION_REUSE_ADDR = "tcp.reuseAddr";
	public static final String SOCKET_OPTION_SEND_BUFFER = "tcp.sendBuffer";
	public static final String SOCKET_OPTION_NO_DELAY = "tcp.noDelay";
	public static final String SOCKET_OPTION_RECV_BUFFER = "tcp.recvBuffer";
	public static final String SOCKET_OPTION_SO_LINGER = "tcp.soLinger";
	public static final String SOCKET_OPTION_LINGER_TIMEOUT = "tcp.lingerTimeout";

	

	//static Logger log = LoggerFactory.getLogger(AdaptiveConfiguration.class);
	
	private static Map<String,String> globalConfig = new HashMap<>();
	private static Map<String,Map<String,String>> patternConfigs = new HashMap<>();
	

	public static String createAlgorithmList(String supportedList, String key,
			 String ident, String hostname, String...ignores) {
		
		List<String> supported = Arrays.asList(supportedList.split("."));
		
		String locallist = getPatternConfig(key, ident);
		if(Utils.isBlank(locallist)) {
			locallist = getPatternConfig(key, hostname);
		}
		
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
	
	public static String createAlgorithmList(ComponentFactory<?> factory, String key, String contextPreference, String remoteIdentification, String hostname, String...ignores) {
		
		String ident = getIdent(remoteIdentification);
		
		String locallist = factory.filter(getPatternConfig(key, ident));
		if(Utils.isBlank(locallist)) {
			locallist = factory.filter(getPatternConfig(key, hostname));
		}
		
		if(Utils.isBlank(locallist)) {
			locallist = factory.filter(getGlobalConfig(key));
		}
		
		if(Utils.isBlank(locallist)) {
			locallist = factory.list(contextPreference, ignores);
		}
		
		return locallist;
	}
	
	public static String getPatternConfig(String key, String... values) {
		for(String value : values) {
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
		
		return getGlobalConfig(key);
	}
	
	private static String formatKey(String key1, String key2) {
		StringBuilder str = new StringBuilder();
		str.append(key1);
		str.append(".");
		str.append(key2);
		return str.toString();
	}
	
	private static String getSystemProperty(String key) {
		String result = System.getProperty(key);
		if(result!=null) {
			if(Log.isDebugEnabled()) {
				Log.debug("Matched {} from system property with value {}", key, result);
			}
		}
		return result;
	}
	
	public static void setPatternConfig(String pattern, String key, String val) {
		if(!patternConfigs.containsKey(pattern)) {
			patternConfigs.put(pattern, new HashMap<String,String>());
		}
		patternConfigs.get(pattern).put(key,  val);
	}
	
	public static String getGlobalConfig(String key) {
		String result = globalConfig.get(key);
		if(result!=null) {
			if(Log.isDebugEnabled()) {
				Log.debug("Matched {} from global configuration with value {}", key, result);
			}
			return result;
		}
		return getSystemProperty(formatKey("maverick", key));
	}
	
	public static void setGlobalConfig(String key, String val) {
		globalConfig.put(key,  val);
	}
	
	public static String getIdent(String remoteIdentification) {
		String ident = remoteIdentification.substring(8);
		int idx = ident.indexOf(' ');
		if(idx > -1) {
			ident = ident.substring(0, idx);
		}
		return ident;
	}

	public static boolean getBoolean(String key, String... match) {
		
		String result = getPatternConfig(key, match);
		if(result==null) {
			return getBoolean(key);
		}
		return Boolean.parseBoolean(result);
	}
	
	public static boolean getBoolean(String key) {
		return Boolean.parseBoolean(getGlobalConfig(key));
	}

	public static void setBoolean(String key, String pattern) {
		setPatternConfig(pattern, key, Boolean.TRUE.toString());
	}
	
	public static void setBoolean(String key, String pattern, Boolean val) {
		setPatternConfig(pattern, key, val.toString());
	}
	
	public static void setBoolean(String key, Boolean value) {
		setGlobalConfig(key, value.toString());
	}

	public static long getLong(String key) {
		return Long.parseLong(getGlobalConfig(key));
	}

	public static boolean getBooleanOrDefault(String key, boolean defaultValue) {
		String result = getGlobalConfig(key);
		if(result!=null) {
			return Boolean.parseBoolean(result);
		}
		return defaultValue;
	}

	public static long getByteSizeOrDefault(String key, String defaultValue) {
		
		String result = getGlobalConfig(key);
		if(result!=null) {
			return IOUtils.fromByteSize(result);
		}
		return  IOUtils.fromByteSize(defaultValue);
	}

	public static int getIntOrDefault(String key, int defaultValue) {
		String result = getGlobalConfig(key);
		if(result!=null) {
			return Integer.parseInt(result);
		}
		return defaultValue;
	}
}
