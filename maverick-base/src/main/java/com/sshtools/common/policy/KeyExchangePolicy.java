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
package com.sshtools.common.policy;

import com.sshtools.common.permissions.Permissions;

public class KeyExchangePolicy extends Permissions {

	int minDHGroupExchangeKeySize = 2048;
	int maxDHGroupExchangeKeySize = 8192;
	
	public int getMinDHGroupExchangeKeySize() {
		return minDHGroupExchangeKeySize;
	}

	public void setMinDHGroupExchangeKeySize(int minDHGroupExchangeKeySize) {
		this.minDHGroupExchangeKeySize = minDHGroupExchangeKeySize;
	}

	public int getMaxDHGroupExchangeKeySize() {
		return maxDHGroupExchangeKeySize;
	}

	public void setMaxDHGroupExchangeKeySize(int maxDHGroupExchangeKeySize) {
		this.maxDHGroupExchangeKeySize = maxDHGroupExchangeKeySize;
	}
}
