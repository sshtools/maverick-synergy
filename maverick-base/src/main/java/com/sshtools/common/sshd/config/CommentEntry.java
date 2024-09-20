package com.sshtools.common.sshd.config;

/*-
 * #%L
 * Base API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
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
