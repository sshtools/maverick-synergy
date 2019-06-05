/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.common.ssh.components;

import com.sshtools.common.ssh.SshException;

/**
 * This interface should be implemented by all secure random number generator
 * implementations.
 * @author Lee David Painter
 *
 */
public interface SshSecureRandomGenerator {

	void nextBytes(byte[] bytes);
	
	void nextBytes(byte[] bytes, int off, int len) throws SshException;
	
	int nextInt();
}