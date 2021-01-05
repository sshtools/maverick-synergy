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
package com.sshtools.vsession.commands.ssh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandUtil {
	
	public static boolean isNotEmpty(String str) {
		return str != null && str.trim().length() > 0;
	}
	
	public static boolean isEmpty(String str) {
		return !isNotEmpty(str);
	}
	
	public static boolean isNotEmpty(String[] array) {
		return array != null && array.length > 0;
	}
	
	public static boolean isEmpty(String[] array) {
		return !isNotEmpty(array);
	}

	public static String csv(List<String> strings) {
		return String.join(",", strings.toArray(new String[0]));
	}
	
	public static String[] toStringFromCsvs(String[] strings) {
		List<String> list = new ArrayList<String>();
		for (String string : strings) {
			String[] parts = string.split(",");
			list.addAll(Arrays.asList(parts));
		}
		
		return list.toArray(new String[0]);
	}
}
