package com.sshtools.common.publickey;

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

/**
 *
 * <p>Thrown by an {@link SshPrivateKeyFile} when it detects that the
 * passphrase supplied was invalid.</p>
 *
 * @author Lee David Painter
 */
public class InvalidPassphraseException extends Exception {

	private static final long serialVersionUID = -1458660635959624570L;

	public InvalidPassphraseException() {
        super("The passphrase supplied was invalid!");
    }
    
    public InvalidPassphraseException(Exception ex) {
    	super(ex.getMessage());
    }
}
