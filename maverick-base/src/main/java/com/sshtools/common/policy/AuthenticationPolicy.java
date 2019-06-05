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
}
