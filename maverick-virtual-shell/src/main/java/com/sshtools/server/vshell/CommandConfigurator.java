package com.sshtools.server.vshell;

public interface CommandConfigurator<T> {
	void configure(T command);
}
