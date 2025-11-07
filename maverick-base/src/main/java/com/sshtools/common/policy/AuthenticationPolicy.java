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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import com.sshtools.common.auth.RequiredAuthenticationStrategy;
import com.sshtools.common.permissions.Permissions;

/**
 * Represents various authentication related policy.
 * 
 * Note, will be made <code>final</code> at version 3.3.0, and will only be able to
 * be constructed using the {@link AuthenticationPolicyBuilder}.
 * 
 * TODO make final at 3.3.0+
 */
public class AuthenticationPolicy extends Permissions {

	/**
	 * Build a new {@link AuthenticationPolicy}.
	 */
	public final static class AuthenticationPolicyBuilder extends AbstractPermissionBuilder<Permission, AuthenticationPolicyBuilder> {
		
		private Optional<Supplier<String>> bannerMessage = Optional.empty(); 
		private int maximumPublicKeyVerificationAttempts = 10;
		private boolean publicKeyVerificationIsFailedAuth = false;
		private RequiredAuthenticationStrategy requiredAuthenticationStrategy = RequiredAuthenticationStrategy.ONCE_PER_CONNECTION;
		private Set<String> requiredMechanisms = new LinkedHashSet<String>();
		private int maxAuthentications = 10;
		
		private AuthenticationPolicyBuilder() { }
		
		/**
		 * Create a new {@link AuthenticationPolicyBuilder} that will be used to configure
		 * and create an {@link AuthenticationPolicy}.
		 * 
		 * @return builder
		 */
		public static AuthenticationPolicyBuilder create() {
			return new AuthenticationPolicyBuilder(); 
		}
		
		/**
		 * Set the maximum number of authentication attempts allowed before disconnecting.
		 * 
		 * @param maxAuthentications maximum authentications
		 * @return this for chaining
		 */
		public AuthenticationPolicyBuilder withMaxAuthentications(int maxAuthentications) {
			this.maxAuthentications = maxAuthentications;
			return this;
		}
		
		/**
		 * Set one or more required mechanisms (clears existing mechanisms).
		 * 
		 * @param mechanisms mechanisms to add
		 * @return this for chaining
		 */
		public AuthenticationPolicyBuilder withRequiredMechanisms(Collection<String> requiredMechanisms) {
			this.requiredMechanisms.clear();
			addRequiredMechanisms(requiredMechanisms);
			return this;
		}
		
		/**
		 * Set one or more required mechanisms (clears existing mechanisms).
		 * 
		 * @param mechanisms mechanisms to add
		 * @return this for chaining
		 */
		public AuthenticationPolicyBuilder withRequiredMechanisms(String... requiredMechanisms) {
			return withRequiredMechanisms(Set.of(requiredMechanisms));
		}

		/**
		 * Add one or more required mechanisms.
		 * 
		 * @param mechanisms mechanisms to add
		 * @return this for chaining
		 */
		public AuthenticationPolicyBuilder addRequiredMechanisms(String... requiredMechanisms) {
			return addRequiredMechanisms(Set.of(requiredMechanisms));
		}

		/**
		 * Add one or more required mechanisms.
		 * 
		 * @param mechanisms mechanisms to add
		 * @return this for chaining
		 */
		public AuthenticationPolicyBuilder addRequiredMechanisms(Collection<String> requiredMechanisms) {
			this.requiredMechanisms.addAll(requiredMechanisms);
			return this;
		}
		
		/**
		 * Set the required authentication strategy.
		 * 
		 * @param requiredAuthenticationStrategy strategy
		 * @return this for chaining
		 */
		public AuthenticationPolicyBuilder withRequiredAuthenticationStrategy(RequiredAuthenticationStrategy requiredAuthenticationStrategy) {
			this.requiredAuthenticationStrategy = requiredAuthenticationStrategy;
			return this;
		}

		/**
		 * When a public key is not valid, consider this as failed authentication.
		 * 
		 * @return this for chaining
		 */
		public AuthenticationPolicyBuilder withPublicKeyVerificationIsFailedAuth() {
			return withPublicKeyVerificationIsFailedAuth(true);
		}


		/**
		 * Set when a public key is not valid, consider this as failed authentication.
		 * 
		 * @param publicKeyVerificationIsFailedAuth fail if key is invalid
		 * @return this for chaining
		 */
		public AuthenticationPolicyBuilder withPublicKeyVerificationIsFailedAuth(boolean publicKeyVerificationIsFailedAuth) {
			this.publicKeyVerificationIsFailedAuth = publicKeyVerificationIsFailedAuth;
			return this;
		}
		
		/**
		 * Set the banner message to display to users on login.
		 * 
		 * @param bannerMessage banner message
		 * @return this for chaining
		 */
		public AuthenticationPolicyBuilder withBannerMessage(String bannerMessage) {
			if(bannerMessage == null ||"".equals(bannerMessage)) {
				this.bannerMessage = Optional.empty();
			}
			else {
				withBannerMessage(() -> bannerMessage);	
			}
			return this;
		}
		
		/**
		 * Set a supplier that provides a banner message to display to users on login
		 *  
		 * @param bannerMessage banner message supplier
		 * @return this for chaining
		 */
		public AuthenticationPolicyBuilder withBannerMessage(Supplier<String> bannerMessage) {
			this.bannerMessage = Optional.of(bannerMessage);
			return this;
		}
		
		/**
		 * Set the maximum number of public key verification attempts allowed before failing.
		 * 
		 * @param maximumPublicKeyVerificationAttempts attempts
		 * @return this for chaining
		 */
		public AuthenticationPolicyBuilder withMaximumPublicKeyVerificationAttempts(int maximumPublicKeyVerificationAttempts) {
			this.maximumPublicKeyVerificationAttempts = maximumPublicKeyVerificationAttempts;
			return this;
		}
		
		/**
		 * Build a new {@link AuthenticationPolicy} given the configuration of this builder.
		 * 
		 * @return policy
		 */
		public AuthenticationPolicy build() {
			return new AuthenticationPolicy(this);
		}
	}

	/* TODO make all of these final, remove all deprecated setters at 3.3.x+ */
	private int maximumPublicKeyVerificationAttempts;
	private Optional<Supplier<String>> bannerMessage;
	private boolean publicKeyVerificationIsFailedAuth;
	private RequiredAuthenticationStrategy requiredAuthenticationStrategy = RequiredAuthenticationStrategy.ONCE_PER_CONNECTION;
	private Collection<String> required;
	private int maxAuthentications;
	
	/**
	 * Construct a new policy.
	 * 
	 * @deprecated use {@link AuthenticationPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public AuthenticationPolicy() {
		bannerMessage = Optional.empty();
		maximumPublicKeyVerificationAttempts = 10;
		publicKeyVerificationIsFailedAuth = false;
		requiredAuthenticationStrategy = RequiredAuthenticationStrategy.ONCE_PER_CONNECTION;
		required = new ArrayList<String>();
		maxAuthentications = 10;
	}
	
	private AuthenticationPolicy(AuthenticationPolicyBuilder bldr) {
		super(bldr);
		this.bannerMessage = bldr.bannerMessage;
		this.maximumPublicKeyVerificationAttempts = bldr.maximumPublicKeyVerificationAttempts;
		this.publicKeyVerificationIsFailedAuth = bldr.publicKeyVerificationIsFailedAuth;
		this.requiredAuthenticationStrategy  = bldr.requiredAuthenticationStrategy;
		this.required = Collections.unmodifiableSet(new LinkedHashSet<>(bldr.requiredMechanisms));
		this.maxAuthentications = bldr.maxAuthentications;
	}
	
	/**
	 * Get the authentication banner to display to connecting clients.
	 * 
	 * @return banner
	 * @deprecated Use {@link AuthenticationPolicy#bannerMessage()}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public String getBannerMessage() {
		return bannerMessage.map(Supplier::get).orElse("");
	}
	
	/**
	 * Return the optional banner message supplier this policy was created with.
	 * 
	 * @return banner message supplier
	 */
	public Optional<Supplier<String>> bannerMessage() {
		return bannerMessage;
	}
	
	/**
	 * Set the banner message that is displayed to all connecing clients prior
	 * to authentication.
	 * 
	 * If this method is used then
	 * com.maverick.sshd.NoneAuthentication.getBannerForUser(String) should not
	 * be overridden.
	 * 
	 * @param authenticationBanner
	 * @deprecated will become immutable, use {@link AuthenticationPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setBannerMessage(String authenticationBanner) {
		this.bannerMessage = authenticationBanner == null || authenticationBanner.equals("") ? Optional.empty() : Optional.of(() -> authenticationBanner);
	}
	
	/**
	 * Get the number of public keys that each user can attempt to verify for
	 * public key authentication. If the user exceeds this limit the connection
	 * is terminated.
	 * 
	 * @return attempts
	 */
	public int getMaximumPublicKeyVerificationAttempts() {
		return maximumPublicKeyVerificationAttempts;
	}

	/**
	 * Set the number of public keys that a user can verify for public key
	 * authentication. If the user exceeds this limit the connection is
	 * terminated.
	 * 
	 * @param maximumPublicKeyVerificationAttempts attempts
	 * @deprecated will become immutable, use {@link AuthenticationPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setMaximumPublicKeyVerificationAttempts(
			int maximumPublicKeyVerificationAttempts) {
		this.maximumPublicKeyVerificationAttempts = maximumPublicKeyVerificationAttempts;
	}
	
	/**
	 * @param publicKeyVerificationIsFailedAuth public key verification is failed auth
	 * @deprecated will become immutable, use {@link AuthenticationPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setPublicKeyVerificationIsFailedAuth(
			boolean publicKeyVerificationIsFailedAuth) {
		this.publicKeyVerificationIsFailedAuth = publicKeyVerificationIsFailedAuth;
	}
	
	/**
	 * Get if when a public key cannot be verified, that should be considered a failed authentication attempt.
	 * 
	 * @return public key verification failed  is failed auth
	 */
	public boolean isPublicKeyVerificationFailedAuth() {
		return publicKeyVerificationIsFailedAuth;
	}
	
	/**
	 * Set the required authentication strategy.
	 * 
	 * @param requiredAuthenticationStrategy strategy 
	 * @deprecated will become immutable, use {@link AuthenticationPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setRequiredAuthenticationStrategy(RequiredAuthenticationStrategy requiredAuthenticationStrategy) {
		this.requiredAuthenticationStrategy = requiredAuthenticationStrategy;
	}
	
	/**
	 * Get the required authentication strategy.
	 * 
	 * @return strategy
	 */
	public RequiredAuthenticationStrategy getRequiredAuthenticationStrategy() {
		return requiredAuthenticationStrategy;
	}

	/**
	 * Add a reuired mechanism.
	 * 
	 * @param auth authentication mechanism
	 * @deprecated will become immutable, use {@link AuthenticationPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void addRequiredMechanism(String auth) {
		required.add(auth);
	}
	
	/**
	 * Get the required mechanisms.
	 * 
	 * @return required mechanisms
	 * @deprecated see {@link #requiredMechanisms()}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public Collection<String> getRequiredMechanisms() {
		return Collections.unmodifiableCollection(required);
	}
	
	/**
	 * Get the required mechanisms.
	 * 
	 * @return required mechanisms
	 */
	@SuppressWarnings("unchecked")
	public Set<String> requiredMechanisms() {
		/* TODO just return 'required' when deprecated methods are removed (3.3.0+) */
		return required instanceof Set req ? req : Set.of(required.toArray(new String[0]));
	}
	
	/**
	 * Get the maximum number of failed authentications allowed for each
	 * connection.
	 * 
	 * @return int
	 */
	public int getMaxAuthentications() {
		return maxAuthentications;
	}

	/**
	 * Set the maximum number of failed authentications allowed for each
	 * connection.
	 * 
	 * @param maxAuthentications 
	 * @deprecated will become immutable, use {@link AuthenticationPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setMaxAuthentications(int maxAuthentications) {
		this.maxAuthentications = maxAuthentications;
	}
}
