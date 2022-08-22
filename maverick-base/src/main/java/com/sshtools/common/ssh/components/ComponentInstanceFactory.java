/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
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
