package com.sshtools.agent.exceptions;

/*-
 * #%L
 * Key Agent
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

import com.sshtools.common.ssh.SshException;

public class InvalidMessageException extends SshException {

	private static final long serialVersionUID = -5307916551875123863L;

	/**
     * <p>
     * Constructs the message.
     * </p>
     *
     * @param msg the error description
     *
     * @since 0.2.0
     */
    public InvalidMessageException(String msg,int reason) {
        super(msg,reason);
    }
}
