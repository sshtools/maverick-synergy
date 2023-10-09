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
			
		}

		@Override
		public boolean isCommentedOut() {
			return false;
		}

		@Override
		public String getValue() {
			throw new UnsupportedOperationException();
		}
	
	}