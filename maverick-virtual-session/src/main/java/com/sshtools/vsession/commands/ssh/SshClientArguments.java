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

package com.sshtools.vsession.commands.ssh;

import java.util.Arrays;
import java.util.Objects;

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
	
	public boolean hasConnection() {
		return Objects.nonNull(con);
	}
	
	public void setConnection(SshConnection con) {
		this.con = con;
	}
	
	public SshConnection getConnection() {
		return con;
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
