package com.sshtools.vsession.commands.ssh;

/*-
 * #%L
 * Virtual Sessions
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.sshtools.client.ClientStateListener;
import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.components.SshKeyPair;

public class SshClientArguments {

	private int port = 22;
	private String destination;
	private AbstractFile identityFile;
	private SshKeyPair identity;
	private String password;
	private String loginName;
	private String command;
	private String[] ciphers;
	private String[] hmacs;
	private String securityLevel;
	private String[] configOptions;
	private boolean compression;
	private SshConnection con;
	private List<ClientStateListener> listeners = new ArrayList<>();
	
	public boolean hasConnection() {
		return Objects.nonNull(con);
	}
	
	public void setConnection(SshConnection con) {
		this.con = con;
	}
	
	public SshConnection getConnection() {
		return con;
	}

	public void addListener(ClientStateListener listener) {
		listeners.add(listener);
	}
	
	public Collection<ClientStateListener> getListeners() {
		return listeners;
	}
	
	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public AbstractFile getIdentityFile() {
		return identityFile;
	}

	public void setIdentityFile(AbstractFile identityFile) {
		this.identityFile = identityFile;
	}

	public String getLoginName() {
		return loginName;
	}

	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String[] getCiphers() {
		return ciphers;
	}

	public void setCiphers(String[] ciphers) {
		this.ciphers = ciphers;
	}

	public String[] getHmacs() {
		return hmacs;
	}

	public void setHmacs(String[] hmacs) {
		this.hmacs = hmacs;
	}

	public String getSecurityLevel() {
		return securityLevel;
	}

	public void setSecurityLevel(String securityLevel) {
		this.securityLevel = securityLevel;
	}

	public String[] getConfigOptions() {
		return configOptions;
	}

	public void setConfigOptions(String[] configOptions) {
		this.configOptions = configOptions;
	}

	public boolean isCompression() {
		return compression;
	}

	public void setCompression(boolean compression) {
		this.compression = compression;
	}

	public SshKeyPair getIdentity() {
		return identity;
	}

	public void setIdentity(SshKeyPair identity) {
		this.identity = identity;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public String toString() {
		return "SshClientArguments [port=" + port + ", destination=" + destination + ", identityFile=" + identityFile
				+ ", loginName=" + loginName + ", command=" + command + ", ciphers=" + Arrays.toString(ciphers)
				+ ", hmacs=" + Arrays.toString(hmacs) + ", securityLevel=" + securityLevel + ", configOptions="
				+ Arrays.toString(configOptions) + ", compression=" + compression + "]";
	}

}
