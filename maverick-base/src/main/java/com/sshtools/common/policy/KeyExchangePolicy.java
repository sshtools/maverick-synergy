package com.sshtools.common.policy;

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

import com.sshtools.common.permissions.Permissions;
import com.sshtools.common.scp.ScpPolicy;

/**
 * Represents various key exchange related policy.
 * 
 * Note, will be made <code>final</code> at version 3.3.0, and will only be able to
 * be constructed using the {@link KeyExchangePolicyBuilder}.
 * 
 * TODO make final at 3.3.0+
 */
public class KeyExchangePolicy extends Permissions {
	
	/**
	 * Build a new {@link ScpPolicy}.
	 */
	public final static class KeyExchangePolicyBuilder extends AbstractPermissionBuilder<Permission, KeyExchangePolicyBuilder> {

		private int minDHGroupExchangeKeySize = 2048;
		private int maxDHGroupExchangeKeySize = 8192;

		private KeyExchangePolicyBuilder() { }
		
		/**
		 * Create a new {@link KeyExchangePolicyBuilder} that will be used to configure
		 * and create a {@link KeyExchangePolicy}.
		 * 
		 * @return builder
		 */
		public static KeyExchangePolicyBuilder create() {
			return new KeyExchangePolicyBuilder(); 
		}
		
		/**
		 * Set the minimum diffie-hellman group exchange key size
		 * 
		 * @param minDHGroupExchangeKeySize  minimum diffie-hellman group exchange key size
		 * @return this for chaining
		 */
		public KeyExchangePolicyBuilder withMinDHGroupExchangeKeySize(int minDHGroupExchangeKeySize) {
			this.minDHGroupExchangeKeySize = minDHGroupExchangeKeySize;
			return this;
		}
		
		/**
		 * Set the maximum diffie-hellman group exchange key size
		 * 
		 * @param maxDHGroupExchangeKeySize  maximum diffie-hellman group exchange key size
		 * @return this for chaining
		 */
		public KeyExchangePolicyBuilder withMaxDHGroupExchangeKeySize(int maxDHGroupExchangeKeySize) {
			this.maxDHGroupExchangeKeySize = maxDHGroupExchangeKeySize;
			return this;
		}
		
		/**
		 * Build a new {@link KeyExchangePolicy} given the configuration of this builder.
		 * 
		 * @return policy
		 */
		public KeyExchangePolicy build() {
			return new KeyExchangePolicy(this);
		}
	}

	/* TODO make all of these private + final, remove all deprecated setters at 3.3.x+ */
	private int minDHGroupExchangeKeySize = 2048;
	private int maxDHGroupExchangeKeySize = 8192;

	/**
	 * Construct a new policy
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public KeyExchangePolicy() {
		minDHGroupExchangeKeySize = 2048;
		maxDHGroupExchangeKeySize = 8192;
	}
	
	private KeyExchangePolicy(KeyExchangePolicyBuilder bldr) {
		super(bldr);
		minDHGroupExchangeKeySize = bldr.minDHGroupExchangeKeySize;
		maxDHGroupExchangeKeySize = bldr.maxDHGroupExchangeKeySize;
	}

	/**
	 * Get the minimum diffie-hellman group exchange key size
	 * 
	 * @return minimum diffie-hellman group exchange key size
	 */
	public int getMinDHGroupExchangeKeySize() {
		return minDHGroupExchangeKeySize;
	}

	/**
	 * Set the minimum diffie-hellman group exchange key size
	 * 
	 * @param minDHGroupExchangeKeySize minimum diffie-hellman group exchange key size
	 * @deprecated will become immutable, use {@link KeyExchangePolicy}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setMinDHGroupExchangeKeySize(int minDHGroupExchangeKeySize) {
		this.minDHGroupExchangeKeySize = minDHGroupExchangeKeySize;
	}

	/**
	 * Get the maximum diffie-hellman group exchange key size
	 * 
	 * @return maximum diffie-hellman group exchange key size
	 */
	public int getMaxDHGroupExchangeKeySize() {
		return maxDHGroupExchangeKeySize;
	}

	/**
	 * Set the maximum diffie-hellman group exchange key size
	 * 
	 * @param maxDHGroupExchangeKeySize maximum diffie-hellman group exchange key size
	 * @deprecated will become immutable, use {@link KeyExchangePolicy}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setMaxDHGroupExchangeKeySize(int maxDHGroupExchangeKeySize) {
		this.maxDHGroupExchangeKeySize = maxDHGroupExchangeKeySize;
	}
}
