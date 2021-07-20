
package com.sshtools.common.publickey.authorized;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.sshtools.common.publickey.SshPublicKeyFileFactory;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.util.Entry;

public class PublicKeyEntry extends Entry<SshPublicKey> {

	String comment;
	LinkedList<Option<?>> orderedOptions = new LinkedList<Option<?>>();
	
	PublicKeyEntry(SshPublicKey value, LinkedList<Option<?>> orderedOptions, String comment) {
		super(value);
		this.orderedOptions = orderedOptions;
		this.comment = comment;
	}

	void setOption(Option<?> o) {
		if(!(o instanceof EnvironmentOption)) {
			Option<?> current = getOption(o.getName());
			if(current!=null) {
				orderedOptions.remove(current);
			}
		}
		orderedOptions.addLast(o);
	}
	
	void removeOption(Option<?> o) {
		if(o instanceof EnvironmentOption) {
			throw new IllegalArgumentException("Incorrect use. Use removeEnvironmentVariable method");
		} else {
			Option<?> current = getOption(o.getName());
			if(current!=null) {
				orderedOptions.remove(current);
			}
		}
	}
	
	boolean hasOption(Option<?> option) {
		for(Option<?> o : orderedOptions) {
			if(o.getName().equals(option.getName())) {
				return true;
			}
		}
		return false;
	}
	
	Option<?> getOption(String name) {
		for(Option<?> o : orderedOptions) {
			if(o.getName().equals(name)) {
				return o;
			}
		}
		return null;
	}
	
	public void addEnvironmentVariable(String name, String value) {
		setOption(new EnvironmentOption(name, value));
	}
	
	public void removeEnvironmentVariable(String name) {
		EnvironmentOption e = null;
		for(Option<?> o : orderedOptions) {
			if(o instanceof EnvironmentOption) {
				e = (EnvironmentOption)o;
				if(e.getEnvironmentName().equals(name)) {
					break;
				}
			}
		}
		if(e!=null) {
			orderedOptions.remove(e);
		}
	}
	
	Map<String,String> getEnvironmentOptions() {
		
		Map<String,String> env = new HashMap<String,String>();
		for(Option<?> o : orderedOptions) {
			if(o instanceof EnvironmentOption) {
				EnvironmentOption e = (EnvironmentOption)o;
				env.put(e.getEnvironmentName(), e.getEnvironmentValue());
			}
		}
		return Collections.unmodifiableMap(env);
	}
	

	public String getFormattedEntry() throws IOException {
		
		StringBuffer buf = new StringBuffer();
		// Add options
		for(Option<?> option : orderedOptions) {
			if(buf.length() > 0) {
				buf.append(",");
			}
			buf.append(option.getFormattedOption());
		}
		if(buf.length() > 0) {
			buf.append(" ");
		}
		buf.append(new String(SshPublicKeyFileFactory.create(value, comment, SshPublicKeyFileFactory.OPENSSH_FORMAT).getFormattedKey(), "UTF-8"));

		return buf.toString();
	}
	
	protected boolean supportsRestrictedOption(Option<?> option) {
		boolean restrict = hasOption(AuthorizedKeyOptions.RESRICT);
		if(restrict) {
			return hasOption(option);
		} else {
			return !hasOption(AuthorizedKeyOptions.getNoOption(option));
		}
	}
	
	public boolean supportsPty() {
		return supportsRestrictedOption(AuthorizedKeyOptions.PTY);
	}
	
	public boolean supportsPortForwarding() {
		return supportsRestrictedOption(AuthorizedKeyOptions.PORT_FORWARDING);
	}
	
	public boolean supportsAgentForwarding() {
		return supportsRestrictedOption(AuthorizedKeyOptions.AGENT_FORWARDING);
	}
	
	public boolean supportsUserRc() {
		return supportsRestrictedOption(AuthorizedKeyOptions.USER_RC);
	}
	
	public boolean supportsX11Forwarding() {
		return supportsRestrictedOption(AuthorizedKeyOptions.X11_FORWARDING);
	}
	
	public boolean isCertAuthority() {
		return hasOption(AuthorizedKeyOptions.CERT_AUTHORITY);
	}
	
	public boolean requiresCommandExecution() {
		return hasOption(CommandOption.class);
	}
	
	boolean hasOption(Class<? extends Option<?>> clz) {
		for(Option<?> o : orderedOptions) {
			if(o.getClass().isAssignableFrom(clz)) {
				return true;
			}
		}
		return false;
	}
	
	Option<?> getOption(Class<? extends Option<?>> clz) {
		for(Option<?> o : orderedOptions) {
			if(o.getClass().isAssignableFrom(clz)) {
				return o;
			}
		}
		return null;
	}

	public String getCommand() {
		if(hasOption(CommandOption.class)) {
			return (String) getOption(CommandOption.class).getValue();
		}
		return null;
	}
	
	public void setCommand(String command) {
		setOption(new CommandOption(command));
	}

	public void addConnectFrom(String remoteAddress) {
		
		if(!hasOption(FromOption.class)) {
			setOption(new FromOption(remoteAddress));
		} else {
			FromOption o = (FromOption) getOption(FromOption.class);
			o.getValue().add(remoteAddress);
		}
	}
	
	public void removeConnectFrom(String remoteAddress) {
		if(hasOption(FromOption.class)) {
			FromOption o = (FromOption) getOption(FromOption.class);
			o.getValue().remove(remoteAddress);
		}
	}
	
	@SuppressWarnings("unchecked")
	public boolean canConnectFrom(String remoteAddress) throws IOException {
		
		if(hasOption(FromOption.class)) {
			return Patterns.matchesWithCIDR((Collection<String>)getOption(FromOption.class).getValue(), remoteAddress);
		}
		return true;
	}
	

	public void addForwardTo(String forwardTo) {
		
		if(!hasOption(PermitOpenOption.class)) {
			setOption(new PermitOpenOption(forwardTo));
		} else {
			PermitOpenOption o = (PermitOpenOption) getOption(PermitOpenOption.class);
			o.getValue().add(forwardTo);
		}
	}
	
	public void removeForwardTo(String forwardTo) {
		if(hasOption(PermitOpenOption.class)) {
			PermitOpenOption o = (PermitOpenOption) getOption(PermitOpenOption.class);
			o.getValue().remove(forwardTo);
		}
	}
	
	@SuppressWarnings("unchecked")
	public boolean canForwardTo(String hostname, int port) {
		if(!supportsPortForwarding()) {
			return false;
		}
		if(hasOption(PermitOpenOption.class)) {
			for(String rule : (Collection<String>) getOption(PermitOpenOption.class).getValue()) {
				int idx = rule.indexOf(':');
				if(idx==-1) {
					throw new IllegalArgumentException("Invalid permitopen rule " + rule);
				}
				String permitHostname = rule.substring(0, idx);
				String permitPort = rule.substring(idx+1);
				if(permitPort.equals("*")) {
					if(hostname.equalsIgnoreCase(permitHostname)) {
						return true;
					}
				} else {
					if(hostname.equalsIgnoreCase(permitHostname)
							&& port == Integer.parseInt(permitPort)) {
						return true;
					}
				}
			}
			return false;
		}
		return true;
	}
	
	@SuppressWarnings("unchecked")
	public Collection<String> getPrincipals() {
		if(!hasOption(PrincipalsOption.class)) {
			return Collections.<String>emptyList();
		}
		return Collections.unmodifiableCollection(
				(Collection<String>) getOption(PrincipalsOption.class).getValue());
	}
	
	public void addPrincipal(String principal) {
		
		if(!hasOption(PrincipalsOption.class)) {
			setOption(new PrincipalsOption(principal));
		} else {
			PrincipalsOption o = (PrincipalsOption) getOption(PrincipalsOption.class);
			o.getValue().add(principal);
		}
	}
	
	public void removePrincipal(String principal) {
		if(hasOption(PrincipalsOption.class)) {
			PrincipalsOption o = (PrincipalsOption) getOption(PrincipalsOption.class);
			o.getValue().remove(principal);
		}
	}

}