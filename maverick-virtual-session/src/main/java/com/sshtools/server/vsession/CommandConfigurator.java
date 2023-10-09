package com.sshtools.server.vsession;

public interface CommandConfigurator<T> {
	void configure(T command);
}
