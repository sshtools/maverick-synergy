/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.common.sftp;

import java.io.IOException;

/**
 * Thrown when an operation that requires an ordinary file is presented with a directory.
 * 
 * @author Brett Smith
 */
public class FileIsDirectoryException extends IOException {

	private static final long serialVersionUID = 244584508925942734L;
	
	/**
     * Constructs the exception.
     */
    public FileIsDirectoryException() {
        super("File is a directory.");
    }
    
	/**
     * Constructs the exception.
     * 
     * @param msg String
     */
    public FileIsDirectoryException(String msg) {
        super(msg);
    }
}
