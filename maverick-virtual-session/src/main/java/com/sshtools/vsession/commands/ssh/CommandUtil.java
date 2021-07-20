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
