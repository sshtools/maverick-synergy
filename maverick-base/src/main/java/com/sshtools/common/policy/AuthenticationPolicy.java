/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.sshtools.common.auth.RequiredAuthenticationStrategy;
import com.sshtools.common.permissions.Permissions;

public class AuthenticationPolicy extends Permissions {

	int maximumPublicKeyVerificationAttempts = 10;
	String bannerMessage = "";
	boolean publicKeyVerificationIsFailedAuth = false;
	RequiredAuthenticationStrategy requiredAuthenticationStrategy = RequiredAuthenticationStrategy.ONCE_PER_CONNECTION;
	List<String> required = new ArrayList<String>();
	protected int maxAuthentications = 10;
	
	/**
	 * Get the authentication banner to display to connecting clients.
	 * 
	 * @return String
	 */
	public String getBannerMessage() {
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
	 */
	public void setBannerMessage(String authenticationBanner) {
		this.bannerMessage = authenticationBanner;
	}
	
	/**
	 * Get the number of public keys that each user can attempt to verify for
	 * public key authentication. If the user exceeds this limit the connection
	 * is terminated.
	 * 
	 * @return int
	 */
	public int getMaximumPublicKeyVerificationAttempts() {
		return maximumPublicKeyVerificationAttempts;
	}

	/**
	 * Set the number of public keys that a user can verify for public key
	 * authentication. If the user exceeds this limit the connection is
	 * terminated.
	 * 
	 * @param maximumPublicKeyVerificationAttempts
	 *            int
	 */
	public void setMaximumPublicKeyVerificationAttempts(
			int maximumPublicKeyVerificationAttempts) {
		this.maximumPublicKeyVerificationAttempts = maximumPublicKeyVerificationAttempts;
	}
	
	public void setPublicKeyVerificationIsFailedAuth(
			boolean publicKeyVerificationIsFailedAuth) {
		this.publicKeyVerificationIsFailedAuth = publicKeyVerificationIsFailedAuth;
	}

	public boolean isPublicKeyVerificationFailedAuth() {
		return publicKeyVerificationIsFailedAuth;
	}
	

	public void setRequiredAuthenticationStrategy(RequiredAuthenticationStrategy requiredAuthenticationStrategy) {
		this.requiredAuthenticationStrategy = requiredAuthenticationStrategy;
	}
	
	public RequiredAuthenticationStrategy getRequiredAuthenticationStrategy() {
		return requiredAuthenticationStrategy;
	}

	public void addRequiredMechanism(String auth) {
		required.add(auth);
	}
	
	public Collection<String> getRequiredMechanisms() {
		return Collections.unmodifiableCollection(required);
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
	 */
	public void setMaxAuthentications(int maxAuthentications) {
		this.maxAuthentications = maxAuthentications;
	}
}
