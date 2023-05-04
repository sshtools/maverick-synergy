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
