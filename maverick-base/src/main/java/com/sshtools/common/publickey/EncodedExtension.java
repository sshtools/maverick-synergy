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
package com.sshtools.common.publickey;

public class EncodedExtension {

	String name;
	byte[] value;
	boolean known;
	
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	protected void setKnown(boolean known) {
		this.known = known;
	}

	protected byte[] getStoredValue() {
		if(value!=null) {
			return value;
		}
		return new byte[0];
	}
	
	public boolean isKnown() {
		return known;
	}
	
	protected void setStoredValue(byte[] value) {
		this.value = value;
	}

	public String getValue() {
		return null;
	}
}
