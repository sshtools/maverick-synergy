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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.common.publickey.authorized;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import com.sshtools.common.logger.Log;
import com.sshtools.common.publickey.SshPublicKeyFileFactory;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.util.Base64;
import com.sshtools.common.util.BlankLineEntry;
import com.sshtools.common.util.CommentEntry;
import com.sshtools.common.util.Entry;

public class AuthorizedKeyFile {

	
	
	LinkedList<Entry<?>> allEntries = new LinkedList<Entry<?>>();
	LinkedList<PublicKeyEntry> keyEntries = new LinkedList<PublicKeyEntry>();

	Set<String> supportedOptions = new HashSet<String>(Arrays.asList("agent-forwarding",
		"cert-authority", "command", "environment", "from", "no-agent-forwarding",
		"no-port-forwarding", "no-pty", "no-user-rc", "no-X11-forwarding", "permitopen",
		"port-forwarding", "principals", "pty", "restrict", "tunnel", "user-rc", "X11-forwarding"));

	public AuthorizedKeyFile() {
	}
	
	public AuthorizedKeyFile(String authorized_keys) throws IOException {
		load(new ByteArrayInputStream(authorized_keys.getBytes("UTF-8")));
	}
	
	public void load(InputStream in) throws IOException {
		
		try {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line;
			while((line = reader.readLine()) != null) {
				
				if(line.trim().equals("")) {
					addBlankLine();
					continue;
				} else if(line.trim().startsWith("#")) {
					addCommentLine(line);
					continue;
				}
			
				String[] tokens = parseLine(line, ' ', false);
				if(tokens.length < 2) {
					// Error 
					addErrorEntry(line);
					continue;
				}
				if(isNumeric(tokens[0]) && tokens.length >= 3) {
					// SSH1 style public key string without any options
					try {
						addSSH1KeyEntry("", tokens[0], tokens[1], tokens[2], tokens.length > 3 ? tokens[3] : "");
					} catch (SshException e) {
						addErrorEntry(line);
					}
					
				} else if(isBase64(tokens[1]) && tokens.length >= 2){
					// SSH2 style public key string without any options
					try {
						addSSH2KeyEntry("", tokens[0], tokens[1], tokens.length > 2 ? tokens[2] : "");
					} catch (SshException e) {
						addErrorEntry(line);
					}
				} else if(isNumeric(tokens[1]) && tokens.length >= 4) {
					// SSH1 style public key string with options
					try {
						addSSH1KeyEntry(tokens[0], tokens[1], tokens[2], tokens[3], tokens.length > 4 ? tokens[4] : "");
					} catch (SshException e) {
						addErrorEntry(line);
					}
				} else if(tokens.length > 2 && isBase64(tokens[2])) {
					// SSH2 style public key string with options
					try {
						addSSH2KeyEntry(tokens[0], tokens[1], tokens[2], tokens.length > 3 ? tokens[3] : "");
					} catch (SshException e) {
						addErrorEntry(line);
					}
				}
				
			}
		
		} finally {
			try {
				in.close();
			} catch (Exception e) {
			}
		}
	}
	
	public boolean isAuthorizedKey(SshPublicKey key) {
		
		for(PublicKeyEntry k : keyEntries) {
			if(k.getValue().equals(key)) {
				return true;
			}
		}
		return false;
	}
	
	public PublicKeyEntry getKeyEntry(SshPublicKey key){
		for(PublicKeyEntry k : keyEntries) {
			if(k.getValue().equals(key)) {
				return k;
			}
		}
		return null;
	}
	
	public Collection<PublicKeyEntry> getKeys() {
		return Collections.unmodifiableCollection(keyEntries);
	}
	
	public void removeKeys(SshPublicKey... keys) {
		for(SshPublicKey key : keys) {
			try {
				PublicKeyEntry entry = getKeyEntry(key);
				removeKey(entry);
			} catch(NoSuchElementException e) {
			}
		}
	}
	
	public void removeKey(PublicKeyEntry entry) {
		keyEntries.remove(entry);
		allEntries.remove(entry);
	}
	
	public void addKey(SshPublicKey key, String comment) {
		PublicKeyEntry entry = new PublicKeyEntry(key, new LinkedList<Option<?>>(), comment);
		allEntries.addLast(entry);
		keyEntries.addLast(entry);
	}
	
	public void addKey(SshPublicKey key, String comment, Option<?>... options) {
		if(getKeyEntry(key)!=null) {
			throw new IllegalArgumentException("Public key is already present in authorized_keys file");
		}
		PublicKeyEntry entry = new PublicKeyEntry(key, 
				new LinkedList<Option<?>>(Arrays.asList(options)), 
				comment);
		allEntries.addLast(entry);
		keyEntries.addLast(entry);
	}
	
	public void setOption(PublicKeyEntry entry, Option<?> option) {
		entry.setOption(option);
	}
	
	public void setOption(SshPublicKey key, Option<?> option) {
		getKeyEntry(key).setOption(option);
	}
	
	boolean isBase64(String line) {
		return line.matches("^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$");
	}
	
	boolean isNumeric(String line) {
		try {
			Integer.parseInt(line);
			return true;
		} catch(NumberFormatException e) {
			return false;
		}
	}
	
	void addErrorEntry(String line) {
		Log.error("Failed to parse authorized_keys line: " + line);
		allEntries.add(new ErrorEntry(line));
	}
	
	void addCommentLine(String line) {
		allEntries.add(new CommentEntry(line));
	}
	
	void addBlankLine() {
		allEntries.add(new BlankLineEntry());
	}
	
	public String getFormattedFile() throws IOException {
		
		StringBuffer buf = new StringBuffer();
		for(Entry<?> e : allEntries) {
			if(buf.length() > 0) {
				buf.append("\r\n");
			}
			buf.append(e.getFormattedEntry());
		}
		return buf.toString();
	}
	
	void addSSH1KeyEntry(String options, String bitLength, String e, String n, String comment) throws SshException {
		

			BigInteger publicExponent = new BigInteger(e);
			BigInteger modulus = new BigInteger(n);

			SshPublicKey key = 
					ComponentManager.getInstance()
							.createRsaPublicKey(modulus,
									publicExponent);
			
			LinkedList<Option<?>> parsedOptions = parseOptions(options);
			
			PublicKeyEntry entry = new PublicKeyEntry(key, parsedOptions, comment);
			
			keyEntries.add(entry);
			allEntries.add(entry);
	
	}
	
	void addSSH2KeyEntry(String options, String algorithm, String encodedKey, String comment) throws SshException, IOException {
		
		SshPublicKey key = SshPublicKeyFileFactory.decodeSSH2PublicKey(Base64.decode(encodedKey));
		
		LinkedList<Option<?>> parsedOptions = parseOptions(options);
		
		PublicKeyEntry entry = new PublicKeyEntry(key, parsedOptions, comment);
		
		keyEntries.add(entry);
		allEntries.add(entry);
	}
	
	static String splitName(String option) {
		int idx = option.indexOf('=');
		if(idx==-1) {
			throw new IllegalArgumentException("Option with invalid format! " + option);
		}
		return option.substring(0,  idx);
	}
	
	static String splitValue(String option) {
		int idx = option.indexOf('=');
		if(idx==-1) {
			throw new IllegalArgumentException("Option with invalid format! " + option);
		}
		return option.substring(idx+1);
	}
	
	LinkedList<Option<?>> parseOptions(String options) {
		if(options.trim().equals("")) {
			return new LinkedList<Option<?>>();
		}
		LinkedList<Option<?>> builtOptions = new LinkedList<Option<?>>();
		String[] parsedOptions = parseLine(options, ',', true);
		for(String option : parsedOptions) {
			if(option.equalsIgnoreCase("agent-forwarding")) {
				builtOptions.add(new NoArgOption(option));
			} else if(option.equalsIgnoreCase("cert-authority")) {
				builtOptions.add(new NoArgOption(option));
			} else if(option.startsWith("command=")) {
				builtOptions.add(new CommandOption(splitValue(option)));
			} else if(option.startsWith("environment=")) {
				builtOptions.add(new EnvironmentOption(splitValue(option)));
			} else if(option.startsWith("from=")) {
				builtOptions.add(new FromOption(splitValue(option)));
			} else if(option.equalsIgnoreCase("no-agent-forwarding")) {
				builtOptions.add(new NoArgOption(option));
			} else if(option.equalsIgnoreCase("no-port-forwarding")) {
				builtOptions.add(new NoArgOption(option));
			} else if(option.equalsIgnoreCase("no-pty")) {
				builtOptions.add(new NoArgOption(option));
			} else if(option.equalsIgnoreCase("no-user-rc")) {
				builtOptions.add(new NoArgOption(option));
			} else if(option.equalsIgnoreCase("no-X11-forwarding")) {
				builtOptions.add(new NoArgOption(option));
			} else if(option.startsWith("permitopen=")) {
				builtOptions.add(new PermitOpenOption(splitValue(option)));
			} else if(option.equalsIgnoreCase("port-forwarding")) {
				builtOptions.add(new NoArgOption(option));
			} else if(option.startsWith("principals=")) {
				builtOptions.add(new PrincipalsOption(splitValue(option)));
			} else if(option.equalsIgnoreCase("pty")) {
				builtOptions.add(new NoArgOption(option));
			} else if(option.equalsIgnoreCase("restrict")) {
				builtOptions.add(new NoArgOption(option));
			} else if(option.startsWith("tunnel")) {
				builtOptions.add(new TunnelOption(splitValue(option)));
			} else if(option.equalsIgnoreCase("user-rc")) {
				builtOptions.add(new NoArgOption(option));
			} else if(option.equalsIgnoreCase("X11-forwarding")) {
				builtOptions.add(new NoArgOption(option));
			} else {
				throw new IllegalArgumentException(option + " not recognised");
			}
		}
		
		return builtOptions;
	}
	
	String[] parseLine(String line, char delim, boolean stripQuotes) {
		
		int i=0;
		StringBuffer buf = new StringBuffer();
		boolean quoted = false;
		boolean escaped = false;
		List<String> tokens = new ArrayList<String>();
		while(i < line.length()) {
			char ch = line.charAt(i);
			if(!quoted && ch == delim) {
				tokens.add(buf.toString());
				buf.setLength(0);
			} else if(ch == '\\') {
				escaped = true;
				buf.append(ch);
				i++;
				continue;
			} else if(ch == '"' && !escaped) {
				quoted = !quoted;
				if(!stripQuotes) {
					buf.append(ch);
				}
			} else {
				buf.append(ch);
			}
			i++;
			escaped = false;
		}
		
		if(buf.length() > 0) {
			tokens.add(buf.toString());
		}
		return tokens.toArray(new String[0]);
	}
	
	class ErrorEntry extends Entry<String> {

		ErrorEntry(String value) {
			super(value);
		}
		
		public String getFormattedEntry() {
			return value;
		}
	}
	
	public static void main(String[] args) {
		
		
		try {
			AuthorizedKeyFile auth = new AuthorizedKeyFile("restrict,agent-forwarding,cert-authority,command=\"ls\"," 
						+ "environment=\"VALUE=value\",from=\"127.0.0.1,192.168.0.0/24\",no-agent-forwarding,"
						+ "no-port-forwarding,no-pty,no-user-rc,no-X11-forwarding,permitopen=\"localhost:80,localhost:443\","
						+ "port-forwarding,principals=\"lee,root\",pty,tunnel=\"3\",user-rc,X11-forwarding" 
						+ " ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDRqJb3pwl7vkQAMUxYpSHPWnZGJJ5bBP0GA3fK/JWIdXplSclIleukhJC/gP4HQTVPAQ+lMl7L9dy9mScRHcRYZzpY8Cm46mji7HaYPgDrjHYnla6A6cOqdJuw8IYk3vVjmo49OZLJE7p2GwdLg0poFFwhUZa5wJQxQwy8PetehgN3oUYOB7NP6wHB4jdfY6GrMWzDeP52OX3QOZZKZfoKuVeVATmYCvn7LFYb5ysEFBve2Jr7bXcN5AFDpAerM/4ybRWcpWGt7IG7bOMLlxI2j9zEkTSwFQ5ShakyaZNA1v+qZXZJ3y54OwqETUSjFmDpA2RBGWJ3wYbrN2sk5YJt lee@kit");
		
			PublicKeyEntry e = auth.getKeys().iterator().next();
//			
//			System.out.println("Agent Forwarding : " + e.supportsAgentForwarding());
//			System.out.println("Port Forwarding  : " + e.supportsPortForwarding());
//			System.out.println("Pty              : " + e.supportsPty());
//			System.out.println("User RC          : " + e.supportsUserRc());
//			System.out.println("X11 Forwarding   : " + e.supportsX11Forwarding());
//			System.out.println("Fixed Command    : " + (e.requiresCommandExecution() ? e.getCommand() : "<No Fixed Command>"));
//			System.out.println("Can Connect      : " + e.canConnectFrom("127.0.0.1"));
//			System.out.println("Can Connect      : " + e.canConnectFrom("192.168.0.45"));
//			System.out.println("Can Connect      : " + e.canConnectFrom("localhost"));
//			System.out.println("Can Connect      : " + e.canConnectFrom("example.com"));
//			System.out.println("Can Connect      : " + e.canConnectFrom("foo.example.com"));
//			System.out.println("Can Forward To   : " + e.canForwardTo("localhost", 22));
//			System.out.println("Can Forward To   : " + e.canForwardTo("localhost", 443));
//			System.out.println("Environment      : " + e.getEnvironmentOptions().toString());
//			System.out.println("Cert Authority   : " + e.isCertAuthority());
//			System.out.println("Principals       : " + e.getPrincipals().toString());
//			System.out.println();
//			System.out.println(auth.getFormattedFile());
//			System.out.println();
//			
//			auth = new AuthorizedKeyFile(
//					"environment=\"VALUE=value\",environment=\"FOO=bar\",no-agent-forwarding,"
//					+ "no-port-forwarding,no-pty,no-user-rc,no-X11-forwarding"
//					+ " ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDRqJb3pwl7vkQAMUxYpSHPWnZGJJ5bBP0GA3fK/JWIdXplSclIleukhJC/gP4HQTVPAQ+lMl7L9dy9mScRHcRYZzpY8Cm46mji7HaYPgDrjHYnla6A6cOqdJuw8IYk3vVjmo49OZLJE7p2GwdLg0poFFwhUZa5wJQxQwy8PetehgN3oUYOB7NP6wHB4jdfY6GrMWzDeP52OX3QOZZKZfoKuVeVATmYCvn7LFYb5ysEFBve2Jr7bXcN5AFDpAerM/4ybRWcpWGt7IG7bOMLlxI2j9zEkTSwFQ5ShakyaZNA1v+qZXZJ3y54OwqETUSjFmDpA2RBGWJ3wYbrN2sk5YJt lee@kit");
//	
//			e = auth.getKeys().iterator().next();
//			
//			System.out.println("Agent Forwarding : " + e.supportsAgentForwarding());
//			System.out.println("Port Forwarding  : " + e.supportsPortForwarding());
//			System.out.println("Pty              : " + e.supportsPty());
//			System.out.println("User RC          : " + e.supportsUserRc());
//			System.out.println("X11 Forwarding   : " + e.supportsX11Forwarding());
//			System.out.println("Fixed Command    : " + (e.requiresCommandExecution() ? e.getCommand() : "<No Fixed Command>"));
//			System.out.println("Can Connect      : " + e.canConnectFrom("127.0.0.1"));
//			System.out.println("Can Connect      : " + e.canConnectFrom("192.168.0.45"));
//			System.out.println("Can Connect      : " + e.canConnectFrom("localhost"));
//			System.out.println("Can Connect      : " + e.canConnectFrom("example.com"));
//			System.out.println("Can Connect      : " + e.canConnectFrom("foo.example.com"));
//			System.out.println("Can Forward To   : " + e.canForwardTo("localhost", 22));
//			System.out.println("Can Forward To   : " + e.canForwardTo("localhost", 443));
//			System.out.println("Environment      : " + e.getEnvironmentOptions().toString());
//			System.out.println("Cert Authority   : " + e.isCertAuthority());
//			System.out.println("Principals       : " + e.getPrincipals().toString());
//			System.out.println();
//			System.out.println(auth.getFormattedFile());
//			System.out.println();
//			
//			auth = new AuthorizedKeyFile(
//					"environment=\"VALUE=value\",environment=\"FOO=bar\",no-agent-forwarding,"
//					+ "no-pty,no-user-rc,no-X11-forwarding"
//					+ " ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDRqJb3pwl7vkQAMUxYpSHPWnZGJJ5bBP0GA3fK/JWIdXplSclIleukhJC/gP4HQTVPAQ+lMl7L9dy9mScRHcRYZzpY8Cm46mji7HaYPgDrjHYnla6A6cOqdJuw8IYk3vVjmo49OZLJE7p2GwdLg0poFFwhUZa5wJQxQwy8PetehgN3oUYOB7NP6wHB4jdfY6GrMWzDeP52OX3QOZZKZfoKuVeVATmYCvn7LFYb5ysEFBve2Jr7bXcN5AFDpAerM/4ybRWcpWGt7IG7bOMLlxI2j9zEkTSwFQ5ShakyaZNA1v+qZXZJ3y54OwqETUSjFmDpA2RBGWJ3wYbrN2sk5YJt lee@kit");
//	
//			e = auth.getKeys().iterator().next();
//			
//			System.out.println("Agent Forwarding : " + e.supportsAgentForwarding());
//			System.out.println("Port Forwarding  : " + e.supportsPortForwarding());
//			System.out.println("Pty              : " + e.supportsPty());
//			System.out.println("User RC          : " + e.supportsUserRc());
//			System.out.println("X11 Forwarding   : " + e.supportsX11Forwarding());
//			System.out.println("Fixed Command    : " + (e.requiresCommandExecution() ? e.getCommand() : "<No Fixed Command>"));
//			System.out.println("Can Connect      : " + e.canConnectFrom("127.0.0.1"));
//			System.out.println("Can Connect      : " + e.canConnectFrom("192.168.0.45"));
//			System.out.println("Can Connect      : " + e.canConnectFrom("localhost"));
//			System.out.println("Can Connect      : " + e.canConnectFrom("example.com"));
//			System.out.println("Can Connect      : " + e.canConnectFrom("foo.example.com"));
//			System.out.println("Can Forward To   : " + e.canForwardTo("localhost", 22));
//			System.out.println("Can Forward To   : " + e.canForwardTo("localhost", 443));
//			System.out.println("Environment      : " + e.getEnvironmentOptions().toString());
//			System.out.println("Cert Authority   : " + e.isCertAuthority());
//			System.out.println("Principals       : " + e.getPrincipals().toString());
//			System.out.println();
//			System.out.println(auth.getFormattedFile());
//			System.out.println();
//			
//			auth = new AuthorizedKeyFile(
//					"restrict,environment=\"VALUE=value\",environment=\"FOO=bar\","
//					+ " ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDRqJb3pwl7vkQAMUxYpSHPWnZGJJ5bBP0GA3fK/JWIdXplSclIleukhJC/gP4HQTVPAQ+lMl7L9dy9mScRHcRYZzpY8Cm46mji7HaYPgDrjHYnla6A6cOqdJuw8IYk3vVjmo49OZLJE7p2GwdLg0poFFwhUZa5wJQxQwy8PetehgN3oUYOB7NP6wHB4jdfY6GrMWzDeP52OX3QOZZKZfoKuVeVATmYCvn7LFYb5ysEFBve2Jr7bXcN5AFDpAerM/4ybRWcpWGt7IG7bOMLlxI2j9zEkTSwFQ5ShakyaZNA1v+qZXZJ3y54OwqETUSjFmDpA2RBGWJ3wYbrN2sk5YJt lee@kit");
//	
//			e = auth.getKeys().iterator().next();
//			
//			System.out.println("Agent Forwarding : " + e.supportsAgentForwarding());
//			System.out.println("Port Forwarding  : " + e.supportsPortForwarding());
//			System.out.println("Pty              : " + e.supportsPty());
//			System.out.println("User RC          : " + e.supportsUserRc());
//			System.out.println("X11 Forwarding   : " + e.supportsX11Forwarding());
//			System.out.println("Fixed Command    : " + (e.requiresCommandExecution() ? e.getCommand() : "<No Fixed Command>"));
//			System.out.println("Can Connect      : " + e.canConnectFrom("127.0.0.1"));
//			System.out.println("Can Connect      : " + e.canConnectFrom("192.168.0.45"));
//			System.out.println("Can Connect      : " + e.canConnectFrom("localhost"));
//			System.out.println("Can Connect      : " + e.canConnectFrom("example.com"));
//			System.out.println("Can Connect      : " + e.canConnectFrom("foo.example.com"));
//			System.out.println("Can Forward To   : " + e.canForwardTo("localhost", 22));
//			System.out.println("Can Forward To   : " + e.canForwardTo("localhost", 443));
//			System.out.println("Environment      : " + e.getEnvironmentOptions().toString());
//			System.out.println("Cert Authority   : " + e.isCertAuthority());
//			System.out.println("Principals       : " + e.getPrincipals().toString());
//			System.out.println();
//			System.out.println(auth.getFormattedFile());
//			System.out.println();
//			
//			auth = new AuthorizedKeyFile(
//					"restrict,port-forwarding,agent-forwarding,environment=\"VALUE=value\",environment=\"FOO=bar\","
//					+ " ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDRqJb3pwl7vkQAMUxYpSHPWnZGJJ5bBP0GA3fK/JWIdXplSclIleukhJC/gP4HQTVPAQ+lMl7L9dy9mScRHcRYZzpY8Cm46mji7HaYPgDrjHYnla6A6cOqdJuw8IYk3vVjmo49OZLJE7p2GwdLg0poFFwhUZa5wJQxQwy8PetehgN3oUYOB7NP6wHB4jdfY6GrMWzDeP52OX3QOZZKZfoKuVeVATmYCvn7LFYb5ysEFBve2Jr7bXcN5AFDpAerM/4ybRWcpWGt7IG7bOMLlxI2j9zEkTSwFQ5ShakyaZNA1v+qZXZJ3y54OwqETUSjFmDpA2RBGWJ3wYbrN2sk5YJt lee@kit");
//	
//			e = auth.getKeys().iterator().next();
//			
//			System.out.println("Agent Forwarding : " + e.supportsAgentForwarding());
//			System.out.println("Port Forwarding  : " + e.supportsPortForwarding());
//			System.out.println("Pty              : " + e.supportsPty());
//			System.out.println("User RC          : " + e.supportsUserRc());
//			System.out.println("X11 Forwarding   : " + e.supportsX11Forwarding());
//			System.out.println("Fixed Command    : " + (e.requiresCommandExecution() ? e.getCommand() : "<No Fixed Command>"));
//			System.out.println("Can Connect      : " + e.canConnectFrom("127.0.0.1"));
//			System.out.println("Can Connect      : " + e.canConnectFrom("192.168.0.45"));
//			System.out.println("Can Connect      : " + e.canConnectFrom("localhost"));
//			System.out.println("Can Connect      : " + e.canConnectFrom("example.com"));
//			System.out.println("Can Connect      : " + e.canConnectFrom("foo.example.com"));
//			System.out.println("Can Forward To   : " + e.canForwardTo("localhost", 22));
//			System.out.println("Can Forward To   : " + e.canForwardTo("localhost", 443));
//			System.out.println("Environment      : " + e.getEnvironmentOptions().toString());
//			System.out.println("Cert Authority   : " + e.isCertAuthority());
//			System.out.println("Principals       : " + e.getPrincipals().toString());
//			System.out.println();
//			System.out.println(auth.getFormattedFile());
//			System.out.println();
			
			auth = new AuthorizedKeyFile(
					"from=\"!192.168.0.4?,192.168.0.0/24\",permitopen=\"localhost:22\""
					+ " ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDRqJb3pwl7vkQAMUxYpSHPWnZGJJ5bBP0GA3fK/JWIdXplSclIleukhJC/gP4HQTVPAQ+lMl7L9dy9mScRHcRYZzpY8Cm46mji7HaYPgDrjHYnla6A6cOqdJuw8IYk3vVjmo49OZLJE7p2GwdLg0poFFwhUZa5wJQxQwy8PetehgN3oUYOB7NP6wHB4jdfY6GrMWzDeP52OX3QOZZKZfoKuVeVATmYCvn7LFYb5ysEFBve2Jr7bXcN5AFDpAerM/4ybRWcpWGt7IG7bOMLlxI2j9zEkTSwFQ5ShakyaZNA1v+qZXZJ3y54OwqETUSjFmDpA2RBGWJ3wYbrN2sk5YJt lee@kit");
	
			e = auth.getKeys().iterator().next();
			e.addEnvironmentVariable("FOO", "BAR");
			
			System.out.println("Agent Forwarding : " + e.supportsAgentForwarding());
			System.out.println("Port Forwarding  : " + e.supportsPortForwarding());
			System.out.println("Pty              : " + e.supportsPty());
			System.out.println("User RC          : " + e.supportsUserRc());
			System.out.println("X11 Forwarding   : " + e.supportsX11Forwarding());
			System.out.println("Fixed Command    : " + (e.requiresCommandExecution() ? e.getCommand() : "<No Fixed Command>"));
			System.out.println("Can Connect      : " + e.canConnectFrom("127.0.0.1"));
			System.out.println("Can Connect      : " + e.canConnectFrom("192.168.0.45"));
			System.out.println("Can Connect      : " + e.canConnectFrom("localhost"));
			System.out.println("Can Connect      : " + e.canConnectFrom("example.com"));
			System.out.println("Can Connect      : " + e.canConnectFrom("foo.example.com"));
			System.out.println("Can Forward To   : " + e.canForwardTo("localhost", 22));
			System.out.println("Can Forward To   : " + e.canForwardTo("localhost", 443));
			System.out.println("Environment      : " + e.getEnvironmentOptions().toString());
			System.out.println("Cert Authority   : " + e.isCertAuthority());
			System.out.println("Principals       : " + e.getPrincipals().toString());
			System.out.println();
			System.out.println(auth.getFormattedFile());
			System.out.println();
			
			e.addConnectFrom("10.0.0.0/16");
			e.removeConnectFrom("192.168.0.0/24");
			e.removeEnvironmentVariable("FOO");
			e.addPrincipal("lee");
			e.addForwardTo("localhost:4000");
			
			System.out.println("Agent Forwarding : " + e.supportsAgentForwarding());
			System.out.println("Port Forwarding  : " + e.supportsPortForwarding());
			System.out.println("Pty              : " + e.supportsPty());
			System.out.println("User RC          : " + e.supportsUserRc());
			System.out.println("X11 Forwarding   : " + e.supportsX11Forwarding());
			System.out.println("Fixed Command    : " + (e.requiresCommandExecution() ? e.getCommand() : "<No Fixed Command>"));
			System.out.println("Can Connect      : " + e.canConnectFrom("127.0.0.1"));
			System.out.println("Can Connect      : " + e.canConnectFrom("192.168.0.45"));
			System.out.println("Can Connect      : " + e.canConnectFrom("localhost"));
			System.out.println("Can Connect      : " + e.canConnectFrom("example.com"));
			System.out.println("Can Connect      : " + e.canConnectFrom("foo.example.com"));
			System.out.println("Can Forward To   : " + e.canForwardTo("localhost", 22));
			System.out.println("Can Forward To   : " + e.canForwardTo("localhost", 443));
			System.out.println("Environment      : " + e.getEnvironmentOptions().toString());
			System.out.println("Cert Authority   : " + e.isCertAuthority());
			System.out.println("Principals       : " + e.getPrincipals().toString());
			System.out.println();
			System.out.println(auth.getFormattedFile());
			System.out.println();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Support PATTERNS * ? !
	}
}
