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

public class ShellDefaultMatcher implements ShellMatcher {

	public Continue matches(String line, String pattern) {
		
		boolean match = false;
		for(int i=0;i<line.length();i++) {
			match = line.charAt(i) == pattern.charAt(i);
		}
		
		if(match && pattern.length() == line.length()) {
			return Continue.CONTENT_MATCHES;
		} else if(match) {
			return Continue.MORE_CONTENT_NEEDED;
		} else {
			return Continue.CONTENT_DOES_NOT_MATCH;
		}
	}

}
