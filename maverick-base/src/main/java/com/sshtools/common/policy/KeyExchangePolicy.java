
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
