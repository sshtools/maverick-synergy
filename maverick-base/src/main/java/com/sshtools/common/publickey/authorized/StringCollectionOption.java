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
package com.sshtools.common.publickey.authorized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

abstract class StringCollectionOption extends Option<Collection<String>> {

	StringCollectionOption(String name, String values) {
		super(name, new ArrayList<String>(Arrays.asList(values.split(","))));
	}
	
	StringCollectionOption(String name, Collection<String> values) {
		super(name, values);
	}

	@Override
	public String getFormattedOption() {
		
		StringBuffer buf = new StringBuffer();
		buf.append(getName());
		buf.append("=");
		buf.append('"');
		int len = buf.length();
		for(String v : getValue()) {
			if(buf.length() > len) {
				buf.append(',');
			}
			buf.append(v);
		}
		
		buf.append('"');
		return buf.toString();
	}
}
