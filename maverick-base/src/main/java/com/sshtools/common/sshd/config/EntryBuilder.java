package com.sshtools.common.sshd.config;

public interface EntryBuilder<T, P> {
	P end();
	SshdConfigFileCursor cursor();
}