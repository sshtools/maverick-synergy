package com.sshtools.common.scp;

import java.nio.charset.Charset;

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


/**
 * Represents various SCP related policy.
 * 
 * Note, will be made <code>final</code> at version 3.3.0, and will only be able to
 * be constructed using the {@link ScpPolicyBuilder}.
 * 
 * TODO make final at 3.3.0+
 */
public class ScpPolicy extends Permissions {
	
	/**
	 * Build a new {@link ScpPolicy}.
	 */
	public final static class ScpPolicyBuilder extends AbstractPermissionBuilder<Permission, ScpPolicyBuilder> {
		
		private boolean readWriteEvents;
		private Charset charsetEncoding = Charset.forName("UTF-8");

		private ScpPolicyBuilder() { }
		
		/**
		 * Create a new {@link ScpPolicyBuilder} that will be used to configure
		 * and create a {@link ScpPolicy}.
		 * 
		 * @return builder
		 */
		public static ScpPolicyBuilder create() {
			return new ScpPolicyBuilder(); 
		}
		
		/**
		 * Enable read / write events for SCP file transfers
		 * 
		 * @param readWriteEvents enable read / write events
		 * @return this for chaining
		 */
		public ScpPolicyBuilder withReadWriteEvents() {
			return withReadWriteEvents(true);
		}
		
		/**
		 * Set whether read / write events are enabled for SCP file transfers
		 * 
		 * @param readWriteEvents enable read / write events
		 * @return this for chaining
		 */
		public ScpPolicyBuilder withReadWriteEvents(boolean readWriteEvents) {
			this.readWriteEvents = readWriteEvents;
			return this;
		}
		
		/**
		 * Set the character set encoding used for SCP (filenames etc)
		 * 
		 * @param charsetEncoding character set encoding
		 * @return this for chaining
		 */
		public ScpPolicyBuilder withCharsetEncoding(Charset charsetEncoding) {
			this.charsetEncoding  = charsetEncoding;
			return this;
		}
		
		/**
		 * Set the character set encoding used for SCP (filenames etc)
		 * 
		 * @param charsetEncoding character set encoding
		 * @return this for chaining
		 */
		public ScpPolicyBuilder withCharsetEncoding(String charsetEncoding) {
			return withCharsetEncoding(Charset.forName(charsetEncoding));
		}
		
		/**
		 * Build a new {@link ScpPolicy} given the configuration of this builder.
		 * 
		 * @return policy
		 */
		public ScpPolicy build() {
			return new ScpPolicy(this);
		}
	}

	/* TODO make all of these private + final, remove all deprecated setters at 3.3.x+ */
	private boolean scpReadWriteEvents;
	private Charset scpCharsetEncoding;

	/**
	 * Construct a new policy
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public ScpPolicy() {
		scpReadWriteEvents = false;
		scpCharsetEncoding = Charset.forName("UTF-8");
	}


	private ScpPolicy(ScpPolicyBuilder bldr) {
		super(bldr);
		scpReadWriteEvents = bldr.readWriteEvents; 
		scpCharsetEncoding = bldr.charsetEncoding;
	}


	/**
	 * Get whether to fire read/write events for SCP transfers.
	 * 
	 * @return read / write events
	 */
	public boolean isSCPReadWriteEvents() {
		return scpReadWriteEvents;
	}

	/**
	 * Sets whether to fire read/write events for SCP transfers.
	 * 
	 * @param scpReadWriteEvents read / write events
	 * @deprecated will become immutable, use {@link ScpPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setSCPReadWriteEvents(boolean scpReadWriteEvents) {
		this.scpReadWriteEvents = scpReadWriteEvents;
	}

	/**
	 * Get the SCP character set encoding.
	 * 
	 * @return character set encoding
	 * @deprecated see {@link #scpCharsetEncoding()}
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public String getSCPCharsetEncoding() {
		return scpCharsetEncoding.name();
	}


	/**
	 * Get the SCP character set encoding.
	 * 
	 * @return character set encoding
	 */
	public Charset scpCharsetEncoding() {
		return scpCharsetEncoding;
	}

	/**
	 * Sets SCP character set encoding
	 * 
	 * @param scpCharsetEncoding character set encoding
	 * @deprecated will become immutable, use {@link ScpPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setSCPCharsetEncoding(String scpCharsetEncoding) {
		this.scpCharsetEncoding = Charset.forName(scpCharsetEncoding);
	}
}
