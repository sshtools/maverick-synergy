package com.sshtools.common.policy;

import java.util.Arrays;

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

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import com.sshtools.common.permissions.Policy;

/**
 * Represents various signature related policy.
 * 
 * Note, will be made <code>final</code> at version 3.3.0, and will only be able to
 * be constructed using the {@link SignaturePolicyBuilder}.
 * 
 * TODO make final at 3.3.0+
 */
public class SignaturePolicy implements Policy {
	
	/**
	 * Build a new {@link SignaturePolicy}.
	 */
	public final static class SignaturePolicyBuilder {

		private Set<String> supportedSignatures = new TreeSet<>();
		private boolean strictMode;

		private SignaturePolicyBuilder() { }
		
		/**
		 * Create a new {@link SignaturePolicyBuilder} that will be used to configure
		 * and create a {@link SignaturePolicy}.
		 * 
		 * @return builder
		 */
		public static SignaturePolicyBuilder create() {
			return new SignaturePolicyBuilder(); 
		}
		
		/**
		 * Set whether strict mode should be used.
		 * 
		 * @param strictMode strict mode
		 * @return this for chaining
		 */
		public SignaturePolicyBuilder withStrictMode(boolean strictMode) {
			this.strictMode = strictMode;
			return this;
		}
		
		/**
		 * Set the supported signature algorithms
		 * 
		 * @param supportedSignatures supported signature algorithms
		 * @return this for chaining
		 */
		public SignaturePolicyBuilder withSupportedSignatures(String... supportedSignatures) {
			return withSupportedSignatures(Arrays.asList(supportedSignatures));
		}
		
		/**
		 * Set the supported signature algorithms
		 * 
		 * @param supportedSignatures supported signature algorithms
		 * @return this for chaining
		 */
		public SignaturePolicyBuilder withSupportedSignatures(Collection<String> supportedSignatures) {
			this.supportedSignatures.clear();
			return addSupportedSignatures(supportedSignatures);
		}
		
		/**
		 * Add supported signature algorithms
		 * 
		 * @param supportedSignatures supported signature algorithms
		 * @return this for chaining
		 */
		public SignaturePolicyBuilder addSupportedSignatures(String... supportedSignatures) {
			return addSupportedSignatures(Arrays.asList(supportedSignatures));
		}
		
		/**
		 * Add supported signature algorithms
		 * 
		 * @param supportedSignatures supported signature algorithms
		 * @return this for chaining
		 */
		public SignaturePolicyBuilder addSupportedSignatures(Collection<String> supportedSignatures) {
			this.supportedSignatures.addAll(supportedSignatures);
			return this;
		}
		
		/**
		 * Remove previously added supported signature algorithms
		 * 
		 * @param supportedSignatures supported signature algorithms
		 * @return this for chaining
		 */
		public SignaturePolicyBuilder removeSupportedSignatures(String... supportedSignatures) {
			return removeSupportedSignatures(Arrays.asList(supportedSignatures));
		}
		
		/**
		 * Remove previously added supported signature algorithms
		 * 
		 * @param supportedSignatures supported signature algorithms
		 * @return this for chaining
		 */
		public SignaturePolicyBuilder removeSupportedSignatures(Collection<String> supportedSignatures) {
			this.supportedSignatures.removeAll(supportedSignatures);
			return this;
		}
		
		/**
		 * Base this builders attributes on an existing policy. The <code>policy</code>
		 * argument may be <code>null</code>, in which case no changes take place.
		 * 
		 * @param policy policy to base on
		 * @return this for chaining
		 */
		public SignaturePolicyBuilder fromPolicy(SignaturePolicy policy) {
			if(policy != null) {
				withSupportedSignatures(policy.supportedSignatures);
				withStrictMode(policy.strictMode);
			}
			return this;
		}
		
		/**
		 * Build a new {@link SignaturePolicy} given the configuration of this builder.
		 * 
		 * @return policy
		 */
		public SignaturePolicy build() {
			return new SignaturePolicy(this);
		}
	}

	/* TODO make all of these private + final, remove all deprecated setters at 3.3.x+ */
	private Set<String> supportedSignatures;
	private boolean strictMode;

	/**
	 * Construct a new policy
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public SignaturePolicy() {
		supportedSignatures = new TreeSet<>();
		strictMode = false;
	}

	/**
	 * Construct a new policy
	 * 
	 * @param supportedSignatures supported signatures
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public SignaturePolicy(Collection<String> supportedSignatures) {
		this(supportedSignatures, false);
	}

	/**
	 * Construct a new policy
	 * 
	 * @param supportedSignatures supported signatures
	 * @param strictMode strict mode
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public SignaturePolicy(Collection<String> supportedSignatures, boolean strictMode) {
		this();
		this.supportedSignatures.addAll(supportedSignatures);
		this.strictMode = strictMode;
	}
	
	private SignaturePolicy(SignaturePolicyBuilder bldr) {
		this.supportedSignatures = Collections.unmodifiableSet(bldr.supportedSignatures);
		this.strictMode = bldr.strictMode;
	}

	public Set<String> getSupportedSignatures() {
		/* TODO just return supportedSignatures directly when deprecations are removed, it is 
		 * already unmodifiable */
		return Collections.unmodifiableSet(supportedSignatures);
	}
	
	/**
	 * Set supported signatures.
	 * 
	 * @param supportedSignatures signatures
	 * @deprecated will become immutable, use {@link SignaturePolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setSupportedSignatures(Collection<String> supportedSignatures) {
		this.supportedSignatures = new TreeSet<>(supportedSignatures);
	}
	
	/**
	 * Get if strict mode is enabled.
	 * 
	 * @return strict mode
	 */
	public boolean isStrictMode() {
		return strictMode;
	}

	/**
	 * Set strict mode.
	 * 
	 * @param strictMode strict mode
	 * @deprecated will become immutable, use {@link SignaturePolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setStrictMode(boolean strictMode) {
		this.strictMode = strictMode;
	}

	@Override
	public final Class<? extends Policy> type() {
		return SignaturePolicy.class;
	}
}
