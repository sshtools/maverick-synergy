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
package com.sshtools.client.shell;

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
