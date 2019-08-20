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
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
/* HEADER */
package com.sshtools.common.ssh;

import java.io.IOException;

/**
 * This class is provided so that when a channel InputStream/OutputStream
 * interface has to throw an IOException; the real SshException cause can be
 * retrieved.
 * 
 * @author Lee David Painter
 */
public class SshIOException extends IOException {

	private static final long serialVersionUID = 6171680689279356698L;
	
	SshException realEx;

	/**
	 * Construct the exception with the real exception.
	 * 
	 * @param realEx
	 */
	public SshIOException(SshException realEx) {
		this.realEx = realEx;
	}

	/**
	 * Get the real exception
	 * 
	 * @return SshException
	 */
	public SshException getRealException() {
		return realEx;
	}
}
