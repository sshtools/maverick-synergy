package com.sshtools.client.events;

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
