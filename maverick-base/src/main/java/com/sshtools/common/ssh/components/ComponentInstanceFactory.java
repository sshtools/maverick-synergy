package com.sshtools.common.ssh.components;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * Responsible for creating instances of components such as public key, kex, digest, compression
 * algorithms. Each component implementation has an associated {@link ComponentInstanceFactory}
 * specialisation that can create an instance of the component. 
 * <p>
 * The factory should also declare whether is enabled by default, and the keys that are used
 * to identifiy it.  
 *
 * @param <T> type of component
 */
public interface ComponentInstanceFactory<T extends Component> {
	
	/**
	 * Get if the component should be enabled by default. Disabled components are usually
	 * obsolete or insecure components.
	 * 
	 *  @return enabled by default
	 */
	default boolean isEnabledByDefault() {
		return true;
	}

	/**
	 * Create a new instance of the component.
	 * 
	 * @return component
	 * @throws NoSuchAlgorithmException if no such algorithm exists
	 * @throws IOException any other error
	 */
	T create() throws NoSuchAlgorithmException, IOException;
	
	/**
	 * Get the internal keys used to identify this component. The same component may be identified by
	 * more than one key, for example <code>SHA256</code> and <code>SHA-256</code>. The first
	 * key in the array is the algorithms primary key.
	 * 
	 * @return key
	 */
	String[] getKeys();
}
