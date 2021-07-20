
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
