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
package com.sshtools.server.vshell.commands.admin;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.cli.CommandLine;

import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;
import com.sshtools.server.vshell.terminal.Console;

public class Threads extends ShellCommand {
	public Threads() {
		super("threads", SUBSYSTEM_JVM, "[<threadId>[,<threadId-2>,..]]");
		setDescription("List all running threads");
		setBuiltIn(false);
		getOptions().addOption("d", false, "Attempt to kill the specified thread(s)");
		getOptions().addOption("s", false, "Attempt to stop the specified thread(s)");
		getOptions().addOption("z", false, "Attempt to suspend the specified thread(s)");
		getOptions().addOption("i", false, "Attempt to interrupt the specified thread(s)");
		getOptions().addOption("j", false, "Attempt to join the specified thread(s)");
		getOptions().addOption("r", false, "Attempt to resume the specified thread(s)");
	}

	@SuppressWarnings("deprecation")
	public void run(CommandLine args, VirtualProcess process) throws IOException {
		Console console = process.getConsole();
		List<?> argList = args.getArgList();
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
						if (args.hasOption('i')) {
							thread.interrupt();
						} else if (args.hasOption('z')) {
							thread.suspend();
						} else if (args.hasOption('1')) {
							thread.setPriority(Thread.MIN_PRIORITY);
						} else if (args.hasOption('2')) {
							thread.setPriority(Thread.NORM_PRIORITY);
						} else if (args.hasOption('3')) {
							thread.setPriority(Thread.MAX_PRIORITY);
						} else if (args.hasOption('s')) {
							thread.stop();
						} else if (args.hasOption('d')) {
							thread.destroy();
						} else if (args.hasOption('r')) {
							thread.resume();
						} else if (args.hasOption('j')) {
							try {
								thread.join();
							} catch (InterruptedException e) {
								console.printStringNewline(e.getMessage());
							}
						} else {
							printThreadInfo(console, thread, "");
							for (StackTraceElement el : thread.getStackTrace()) {
								console.printStringNewline(el.toString());
							}
						}
					}
				}
			}
		}
	}

	private static void printThreadInfo(Console console, Thread t, String indent) throws IOException {
		if (t == null)
			return;
		console.printStringNewline(indent + "Thread: " + (String.format("%8d", t.getId())) + " " + t.getName() + "  Priority: "
			+ t.getPriority() + (t.isDaemon() ? " Daemon" : "") + (t.isAlive() ? "" : " Not Alive"));
	}

	private static void printGroupInfo(Console console, ThreadGroup g, String indent) throws IOException {
		if (g == null)
			return;
		int numThreads = g.activeCount();
		int numGroups = g.activeGroupCount();
		Thread[] threads = new Thread[numThreads];
		ThreadGroup[] groups = new ThreadGroup[numGroups];

		g.enumerate(threads, false);
		g.enumerate(groups, false);

		console.printStringNewline(indent + "Thread Group: " + g.getName() + "  Max Priority: " + g.getMaxPriority()
			+ (g.isDaemon() ? " Daemon" : ""));

		for (int i = 0; i < numThreads; i++)
			printThreadInfo(console, threads[i], indent + "    ");
		for (int i = 0; i < numGroups; i++)
			printGroupInfo(console, groups[i], indent + "    ");
	}

	public static void listAllThreads(Console console) throws IOException {
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
