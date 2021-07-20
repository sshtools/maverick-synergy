
package com.sshtools.server.vsession.jvm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sshtools.common.util.Utils;
import com.sshtools.server.vsession.CliHelper;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.UsageHelper;
import com.sshtools.server.vsession.VirtualConsole;

public class ThreadDump extends ShellCommand {
	public ThreadDump() {
		super("thread-dump", SUBSYSTEM_JVM, UsageHelper.build("thread-dump [options]",
				"-t       Show TIMED_WAITING threads", 
				"-b       Show BLOCKED threads",
				"-w       Show WAITING threads",
				"-r       Show RUNNABLE threads",
				"-n       Show NEW threads"), 
				"Generate and print out a thread dump.");
		setBuiltIn(false);
	}

	public void run(String[] args, VirtualConsole process) throws IOException {

		List<Thread.State> states = new ArrayList<>();
		if(args.length > 1) {
			if(CliHelper.hasShortOption(args, 'b')) {
				states.add(Thread.State.BLOCKED);
			}
			if(CliHelper.hasShortOption(args, 't')) {
				states.add(Thread.State.TIMED_WAITING);
			}
			if(CliHelper.hasShortOption(args, 'w')) {
				states.add(Thread.State.WAITING);
			}
			if(CliHelper.hasShortOption(args, 'r')) {
				states.add(Thread.State.RUNNABLE);
			}
			if(CliHelper.hasShortOption(args, 'n')) {
				states.add(Thread.State.NEW);
			}
		}
		
		String dump = Utils.generateThreadDump(states.toArray(new Thread.State[0]));
		if(Utils.isNotBlank(dump)) {
			process.println(dump);
		} else {
			process.println("There were no threads with the states requested");
		}
		
	}
}
