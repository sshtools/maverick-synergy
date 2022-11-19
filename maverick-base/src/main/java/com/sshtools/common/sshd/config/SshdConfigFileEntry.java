package com.sshtools.common.sshd.config;
public abstract class SshdConfigFileEntry {
		
		public abstract String getFormattedLine();

		public abstract void setValue(String value);

		public abstract void setCommentedOut(boolean b);

		public abstract boolean isCommentedOut();

	}