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
package com.sshtools.agent.exceptions;

/**
 * @desc   Throws when the agent is not available
 * @author Aruna Abesekara
 * @version 1.0
 */
public class AgentNotAvailableException extends Exception {

	private static final long serialVersionUID = -3802779893612697396L;

	/**
     * Creates a new AgentNotAvailableException object.
     */
    public AgentNotAvailableException() {
        super("An agent could not be found");
    }
    
    public AgentNotAvailableException(String msg) {
        super(msg);
    }
}
