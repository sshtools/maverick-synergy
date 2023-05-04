package com.sshtools.common.sshd.config;

import java.util.NoSuchElementException;

public abstract class SshdConfigFileEntry {
		
		public abstract String getFormattedLine();

		public abstract void setValue(String value);

		public abstract void setCommentedOut(boolean b);

		public abstract boolean isCommentedOut();

		public boolean hasNext() {
			return false;
		}

		public SshdConfigFileEntry getNext() {
			throw new NoSuchElementException();
		}
		
		public void setNext(SshdConfigFileEntry entry) {
			throw new NoSuchElementException();
		}

		public abstract String getValue();
	}