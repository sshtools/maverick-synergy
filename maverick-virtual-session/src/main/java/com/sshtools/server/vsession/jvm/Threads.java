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
import java.util.Iterator;
import java.util.List;

import com.sshtools.server.vsession.CliHelper;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.UsageHelper;
import com.sshtools.server.vsession.VirtualConsole;

public class Threads extends ShellCommand {
	public Threads() {
		super("threads", SUBSYSTEM_JVM, UsageHelper.build("threads <threadId>..",
				"-1     Set specified thread(s) priority to MINIMUM",
				"-2     Set specified thread(s) priority to NORMAL",
				"-3     Set specified thread(s) priority to MAXIMUM"),"List all running threads");
		setBuiltIn(false);
	}

	public void run(String[] args, VirtualConsole console) throws IOException {

		List<?> argList = new ArrayList<>(Arrays.asList(args));
		argList.remove(0);
		if (argList.size() == 0) {
			listAllThreads(console);
		} else {
			ThreadGroup root = getRootThread();
			Thread[] threadArray = new Thread[2000];
			int threads = root.enumerate(threadArray);
			for (int i = 0; i < threads; i++) {
				for (Iterator<?> it = argList.iterator(); it.hasNext();) {
					long id = Long.parseLong((String) it.next());
					Thread thread = threadArray[i];
					if (thread.getId() == id) {
						if (CliHelper.hasShortOption(args, '1')) {
							thread.setPriority(Thread.MIN_PRIORITY);
						} else if (CliHelper.hasShortOption(args, '2')) {
							thread.setPriority(Thread.NORM_PRIORITY);
						} else if (CliHelper.hasShortOption(args, '3')) {
							thread.setPriority(Thread.MAX_PRIORITY);
						} else {
							printThreadInfo(console, thread, "");
							for (StackTraceElement el : thread.getStackTrace()) {
								console.println(el.toString());
							}
						}
					}
				}
			}
		}
	}

	private static void printThreadInfo(VirtualConsole console, Thread t, String indent) throws IOException {
		if (t == null)
			return;
		console.println(indent + "Thread: " + (String.format("%8d", t.getId())) + " " + t.getName() + "  Priority: "
			+ t.getPriority() + (t.isDaemon() ? " Daemon" : "") + (t.isAlive() ? "" : " Not Alive"));
	}

	private static void printGroupInfo(VirtualConsole console, ThreadGroup g, String indent) throws IOException {
		if (g == null)
			return;
		int numThreads = g.activeCount();
		int numGroups = g.activeGroupCount();
		Thread[] threads = new Thread[numThreads];
		ThreadGroup[] groups = new ThreadGroup[numGroups];

		g.enumerate(threads, false);
		g.enumerate(groups, false);

		console.println(indent + "Thread Group: " + g.getName() + "  Max Priority: " + g.getMaxPriority()
			+ (g.isDaemon() ? " Daemon" : ""));

		for (int i = 0; i < numThreads; i++)
			printThreadInfo(console, threads[i], indent + "    ");
		for (int i = 0; i < numGroups; i++)
			printGroupInfo(console, groups[i], indent + "    ");
	}

	public static void listAllThreads(VirtualConsole console) throws IOException {
		ThreadGroup rootThreadGroup = getRootThread();
		printGroupInfo(console, rootThreadGroup, "");
	}

	protected static ThreadGroup getRootThread() {

		ThreadGroup currentThreadGroup = Thread.currentThread().getThreadGroup();

		ThreadGroup rootThreadGroup = currentThreadGroup;
		ThreadGroup parent;
		parent = rootThreadGroup.getParent();
		while (parent != null) {
			rootThreadGroup = parent;
			parent = parent.getParent();
		}
		return rootThreadGroup;
	}
}
