package com.sshtools.client.shell;

/*-
 * #%L
 * Client API
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

public interface ShellMatcher {

	enum Continue {
		MORE_CONTENT_NEEDED,
		CONTENT_MATCHES,
		CONTENT_DOES_NOT_MATCH
	}
	
    /**
     * Match a command output line against a defined pattern.
     * @param line the line of output to search
     * @param pattern the pattern required
     * @return boolean
     */
    public Continue matches(String line, String pattern);
}
