/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.common.ssh;

public class IncompatibleAlgorithm {

	public enum ComponentType { CIPHER_CS, CIPHER_SC, MAC_CS, MAC_SC, KEYEXCHANGE, PUBLICKEY, COMPRESSION_CS, COMPRESSION_SC };

	ComponentType type;
	String[] localAlgorithms;
	String[] remoteAlgorithms;
	
	public IncompatibleAlgorithm(ComponentType type, String[] localAlgorithms, String[] remoteAlgorithms) {
		this.type = type;
		this.localAlgorithms = localAlgorithms;
		this.remoteAlgorithms = remoteAlgorithms;
	}

	public ComponentType getType() {
		return type;
	}

	public String[] getLocalAlgorithms() {
		return localAlgorithms;
	}

	public String[] getRemoteAlgorithms() {
		return remoteAlgorithms;
	}
	
	
}
