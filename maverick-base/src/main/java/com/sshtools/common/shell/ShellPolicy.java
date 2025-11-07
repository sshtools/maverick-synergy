package com.sshtools.common.shell;

import java.time.Duration;
import java.util.Optional;

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
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.util.UnsignedInteger32;

/**
 * Represents various shell related policy.
 * 
 * Note, will be made <code>final</code> at version 3.3.0, and will only be able to
 * be constructed using the {@link ShellPolicyBuilder}.
 * 
 * TODO make final at 3.3.0+
 */
public class ShellPolicy extends Permissions {
	
	/**
	 * Shell permissions
	 */
	public enum ShellPermission implements Permission {
		SHELL,
		EXEC,
		SUBSYSTEM;

		@Override
		public int nativeMask() {
			switch(this) {
			case SHELL:
				return ShellPolicy.SHELL;
			case EXEC:
				return ShellPolicy.EXEC;
			default:
				return ShellPolicy.SUBSYSTEM;
			}
		}
	}
	
	/**
	 * Build a new {@link ShellPolicy}.
	 */
	public final static class ShellPolicyBuilder extends AbstractPermissionBuilder<ShellPermission, ShellPolicyBuilder> {
		
		private Optional<Duration> sessionTimeout = Optional.empty();
		private long sessionMaxWindowSize = 1024000;
		private long sessionMinWindowSize = 131072;
		protected int sessionMaxPacketSize = 65536;
		
		private ShellPolicyBuilder() { }
		
		/**
		 * Create a new {@link ShellPolicyBuilder} that will be used to configure
		 * and create an {@link ShellPolicy}.
		 * 
		 * @return builder
		 */
		public static ShellPolicyBuilder create() {
			return new ShellPolicyBuilder(); 
		}
		
		/**
		 * Set the maximum session window size in bytes.
		 * 
		 * @param sessionMaxWindowSize maximum session window size
		 * @return this for chaining
		 */
		public ShellPolicyBuilder withSessionMaxWindowSize(long sessionMaxWindowSize) {
			this.sessionMaxWindowSize = sessionMaxWindowSize;
			return this;
		}
		
		/**
		 * Set the maximum session window size in bytes.
		 * 
		 * @param sessionMaxWindowSize maximum session window size
		 * @return this for chaining
		 */
		public ShellPolicyBuilder withSessionMaxWindowSize(UnsignedInteger32 sessionMaxWindowSize) {
			return withSessionMaxWindowSize(sessionMaxWindowSize.longValue());
		}
		
		/**
		 * Set the minimum session window size in bytes.
		 * 
		 * @param sessionMaxWindowSize maximum session window size
		 * @return this for chaining
		 */
		public ShellPolicyBuilder withSessionMinWindowSize(long sessionMinWindowSize) {
			this.sessionMinWindowSize = sessionMinWindowSize;
			return this;
		}
		
		/**
		 * Set the minimum session window size in bytes.
		 * 
		 * @param sessionMaxWindowSize maximum session window size
		 * @return this for chaining
		 */
		public ShellPolicyBuilder withSessionMinWindowSize(UnsignedInteger32 sessionMinWindowSize) {
			return withSessionMinWindowSize(sessionMinWindowSize.longValue());
		}
		
		/**
		 * Set the maximum session packet size in bytes.
		 * 
		 * @param sessionMaxPacketSize session max packet size
		 * @return this for chaining
		 */
		public ShellPolicyBuilder withSessionMaxPacketSize(int sessionMaxPacketSize) {
			this.sessionMaxPacketSize = sessionMaxPacketSize;
			return this;
		}
		
		/**
		 * Set the session timeout in seconds. A timeout of zero (the default) means no timeout.
		 * 
		 * @param sessionTimeout session timeout
		 * @return this for chaining
		 */
		public ShellPolicyBuilder withSessionTimeoutSeconds(int sessionTimeoutSeconds) {
			return withSessionTimeout(Duration.ofSeconds(sessionTimeoutSeconds));
		}
		
		/**
		 * Set the session timeout. A timeout of zero (the default) means no timeout.
		 * 
		 * @param sessionTimeout session timeout
		 * @return this for chaining
		 */
		public ShellPolicyBuilder withSessionTimeout(Duration sessionTimeout) {
			this.sessionTimeout = sessionTimeout.toSeconds() == 0 
					? Optional.empty() 
					: Optional.of(sessionTimeout);
			return this;
		}
		
		/**
		 * Build a new {@link ShellPolicy} given the configuration of this builder.
		 * 
		 * @return policy
		 */
		public ShellPolicy build() {
			return new ShellPolicy(this);
		}
	}

	/**
	 * @deprecated See {@link ShellPermission#SHELL}
	 */
	@Deprecated
	public static final int SHELL							  = 0x00001000;
	/**
	 * @deprecated See {@link ShellPermission#EXEC}
	 */
	@Deprecated
	public static final int EXEC							  = 0x00002000;
	/**
	 * @deprecated See {@link ShellPermission#SUBSYSTEM}
	 */
	@Deprecated
	public static final int SUBSYSTEM						  = 0x00004000;

	/* TODO make all of these private + final, remove all deprecated setters at 3.3.x+ */
	private Optional<Duration> sessionTimeout;
	protected int sessionMaxPacketSize = 65536;
	protected UnsignedInteger32 sessionMaxWindowSize = new UnsignedInteger32(1024000);
	protected UnsignedInteger32 sessionMinWindowSize = new UnsignedInteger32(131072);
	
	/**
	 * Construct a new policy
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public ShellPolicy() {
		permissions = SHELL
		| EXEC
		| SUBSYSTEM;	
		sessionTimeout = Optional.empty();
		sessionMaxPacketSize = 65536;
		sessionMaxWindowSize = new UnsignedInteger32(1024000);
		sessionMinWindowSize = new UnsignedInteger32(131072);
	}
	
	private ShellPolicy(ShellPolicyBuilder bldr) {
		super(bldr);
		
		this.sessionTimeout = bldr.sessionTimeout;
		this.sessionMaxPacketSize = bldr.sessionMaxPacketSize;
		sessionMaxWindowSize = new UnsignedInteger32(bldr.sessionMaxWindowSize);
		sessionMinWindowSize = new UnsignedInteger32(bldr.sessionMinWindowSize);
	}

	@Deprecated(since = "3.2.0", forRemoval = true)
	protected boolean assertPermission(SshConnection con, int perm, String... args) {
		return check(perm);
	}
	
	@Deprecated(since = "3.2.0", forRemoval = true)
	public final boolean checkPermission(SshConnection con, int perm, String... args) {
		return assertPermission(con, perm, args);
	}
	
	/**
	 * Get the optional session timeout
	 * 
	 * @return session timeout
	 */
	public Optional<Duration> sessionTimeout() {
		return sessionTimeout;
	}
	
	/**
	 * Returns the session timeout in seconds
	 * 
	 * @return session timeout in seconds
	 * @deprecated see {@link #sessionTimeout()}
	 */
	public int getSessionTimeout() {
		return sessionTimeout.map(Duration::toSeconds).orElse(Long.valueOf(0)).intValue();
	}

	/**
	 * Sets the session timeout in seconds
	 * 
	 * @param sessionTimeoutSeconds session timeout in seconds
	 * @deprecated will become immutable, use {@link ShellPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setSessionTimeout(int sessionTimeoutSeconds) {
		this.sessionTimeout = sessionTimeoutSeconds == 0 ? Optional.empty() : Optional.of(Duration.ofSeconds(sessionTimeoutSeconds));
	}
	
	/**
	 * Returns the session timeout in seconds
	 * 
	 * @return session timeout in seconds
	 * @deprecated see {@link #sessionTimeout()}
	 */
	public int getSessionTimeoutSeconds() {
		return getSessionTimeout();
	}

	/**
	 * Sets the session timeout in seconds
	 * 
	 * @param sessionTimeoutSeconds session timeout in seconds
	 * @deprecated will become immutable, use {@link ShellPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setSessionTimeoutSeconds(int sessionTimeoutSeconds) {
		setSessionTimeout(sessionTimeoutSeconds);
	}

	/**
	 * Get the maximum session packet size
	 * 
	 * @return maximum session packet size
	 */
	public int getSessionMaxPacketSize() {
		return sessionMaxPacketSize;
	}

	/**
	 * Sets the maximum session packet size
	 * 
	 * @param sessionMaxPacketSize session packet sioze
	 * @deprecated will become immutable, use {@link ShellPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setSessionMaxPacketSize(int sessionMaxPacketSize) {
		this.sessionMaxPacketSize = sessionMaxPacketSize;
	}

	/**
	 * Get the maximum session window size
	 * 
	 * @return maximum session window size
	 */
	public UnsignedInteger32 getSessionMaxWindowSize() {
		return sessionMaxWindowSize;
	}

	/**
	 * Sets the maximum session window size
	 * 
	 * @param sessionMaxWindowSize session maximum window size
	 * @deprecated will become immutable, use {@link ShellPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setSessionMaxWindowSize(UnsignedInteger32 sessionMaxWindowSize) {
		this.sessionMaxWindowSize = sessionMaxWindowSize;
	}

	/**
	 * Get the minimum session window size
	 * 
	 * @return minimum session window size
	 */
	public UnsignedInteger32 getSessionMinWindowSize() {
		return sessionMinWindowSize;
	}

	/**
	 * Sets the minimum session window size
	 * 
	 * @param sessionMinWindowSize session minimum window size
	 * @deprecated will become immutable, use {@link ShellPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setSessionMinWindowSize(UnsignedInteger32 sessionMinWindowSize) {
		this.sessionMinWindowSize = sessionMinWindowSize;
	}
	
}
