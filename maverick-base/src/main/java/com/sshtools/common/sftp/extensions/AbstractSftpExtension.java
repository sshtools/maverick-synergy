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
package com.sshtools.common.sftp.extensions;

import com.sshtools.common.sftp.SftpExtension;

public abstract class AbstractSftpExtension implements SftpExtension {

	boolean declaredInVersion;
	String name;

	protected AbstractSftpExtension(String name, boolean declaredInVersion) {
		this.declaredInVersion = declaredInVersion;
		this.name = name;
	}
	
	@Override
	public boolean isDeclaredInVersion() {
		return declaredInVersion;
	}

	@Override
	public byte[] getDefaultData() {
		if(declaredInVersion) {
			return generateDefaultData();
		}
		throw new UnsupportedOperationException();
	}
	
	protected byte[] generateDefaultData() {
		return new byte[0];
	}

	@Override
	public String getName() {
		return name;
	}

}
