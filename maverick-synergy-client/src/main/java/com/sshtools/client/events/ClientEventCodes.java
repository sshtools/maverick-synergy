/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
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
