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
package com.sshtools.client.events;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * List of unique Client Event codes 
 */
public class ClientEventCodes  {

	


	public static Map<Integer,String> messageCodes=new HashMap<Integer,String>();
	public static Map<String,String> messageAttributes=new HashMap<String,String>();
	
	static {
		
		// Utility for looking up event name
		Class<?> mavevent=ClientEventCodes.class;
		Field[] fields=mavevent.getFields();
		for(int i=0;i<fields.length;i++) {
			int modifiers=fields[i].getModifiers();
			if(Modifier.isFinal(modifiers) && Modifier.isStatic(modifiers)) {
				try {
					String fieldName=fields[i].getName();
					if(fieldName.startsWith("EVENT_")) {
						messageCodes.put((Integer)fields[i].get(null), fieldName.substring(6));
					} else {
						messageAttributes.put((String)fields[i].get(null), fieldName.substring(10));
					}
				} catch (IllegalArgumentException e) {
				} catch (IllegalAccessException e) {
				}
			}
		}
	}
}
