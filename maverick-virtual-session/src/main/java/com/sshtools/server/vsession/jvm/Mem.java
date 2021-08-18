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

package com.sshtools.server.vsession.jvm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sshtools.server.vsession.CliHelper;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.VirtualConsole;

public class Mem extends ShellCommand {
	public Mem() {
		super("mem", SUBSYSTEM_JVM, "mem", "Displays JVM memory information");
	}

	public void run(String[] args, VirtualConsole console) throws IOException {
		Map<String, Long> memory = new HashMap<String, Long>();
		long free = Runtime.getRuntime().freeMemory();
		long max = Runtime.getRuntime().maxMemory();
		long total = Runtime.getRuntime().totalMemory();
		long used = Runtime.getRuntime().totalMemory();
		memory.put("free", free);
		memory.put("max", max);
		memory.put("total", total);
		memory.put("used", used);
		List<String> argList = new ArrayList<>(Arrays.asList(args));
		argList.remove(0);
		
		boolean filtered = argList.contains("free") || argList.contains("max") || argList.contains("total") || argList.contains("used");
		for (String name : memory.keySet()) {
			if (!filtered || argList.contains(name)) {
				console.println(name + "=" + format(args, memory.get(name)));
			}
		}
	}

	static String format(String[] args, long value) {
		if (CliHelper.hasShortOption(args, 'm')) {
			return (value / 1024 / 1024) + " MB";
		} else if (CliHelper.hasShortOption(args, 'k')) {
			return (value / 1024) + " KB";
		}
		return value + " Bytes";
	}
}
