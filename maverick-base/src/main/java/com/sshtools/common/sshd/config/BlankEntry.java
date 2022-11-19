package com.sshtools.common.sshd.config;
public class BlankEntry extends NonValidatingFileEntry {

		@Override
		public String getFormattedLine() {
			return "";
		}

		@Override
		public void setValue(String value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setCommentedOut(boolean b) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isCommentedOut() {
			return false;
		}		
	}