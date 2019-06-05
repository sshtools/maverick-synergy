package com.sshtools.common.auth;

/**
 * 
 * <p>Abstract implementation of a {@link PublicKeyAuthenticationProvider} 
 * that just provides {@link #getName()} implementation.
 * </p>
 * 
 * @author Lee David Painter
 */
public abstract class AbstractPublicKeyAuthenticationProvider implements
		PublicKeyAuthenticationProvider {

	public String getName() {
		return "publickey";
	}
}
