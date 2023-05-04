package com.sshtools.common.sshd.config;
public class CommentEntry extends NonValidatingFileEntry {
		
		String comment;
		boolean loaded;
		
		public CommentEntry(String comment) {
			this.comment = comment;
			this.loaded = false;
		}
		
		public boolean isLoaded() {
			return this.loaded;
		}
		
		public boolean isNotLoaded() {
			return !this.isLoaded();
		}

		@Override
		public String getFormattedLine() {
			return String.format("# %s", comment);
		}

		@Override
		public void setValue(String value) {
			this.comment = value;
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
			return comment;
		}
	
	}