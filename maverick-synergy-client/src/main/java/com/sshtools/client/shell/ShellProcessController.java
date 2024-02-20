package com.sshtools.client.shell;


public class ShellProcessController extends ShellController {

	ShellProcess process;
	
	
	public ShellProcessController(ShellProcess process) {
		this(process, new ShellDefaultMatcher());
	}
	
	public ShellProcessController(ShellProcess process, ShellMatcher matcher) {
		super(process.getShell(), matcher, process.getInputStream());
		this.process = process;
	}

	@Override
	public boolean isActive() {
		return process.isActive();
	}

	public ShellProcess getProcess() {
		return process;
	}
	
    
}
