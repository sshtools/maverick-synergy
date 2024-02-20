package com.sshtools.common.ssh;

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
