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
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.server.vsession.commands.sftp;

import java.util.Arrays;

public class SftpClientArguments {

	private int port = 22;
	private boolean compression;
	private String identityFile;
	private String[] ciphers;

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
	public boolean isCompression() {
		return compression;
	}

	public void setCompression(boolean compression) {
		this.compression = compression;
	}

	public String getIdentityFile() {
		return identityFile;
	}

	public void setIdentityFile(String identityFile) {
		this.identityFile = identityFile;
	}
	
	public String[] getCiphers() {
		return ciphers;
	}

	public void setCiphers(String[] ciphers) {
		this.ciphers = ciphers;
	}

	@Override
	public String toString() {
		return "SftpClientArguments [port=" + port + ", compression=" + compression + ", identityFile=" + identityFile
				+ ", ciphers=" + Arrays.toString(ciphers) + "]";
	}

}
