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

import java.util.Objects;

public class SshdConfigKeyValueEntry extends SshdConfigFileEntry {
		
		private String key;
		private String value;
		private boolean commentedOut;
		private boolean indented;
		private SshdConfigKeyValueEntry next;
		
		public SshdConfigKeyValueEntry(String key, String value) {
			this(key, value, false, false);
		}
		
		public SshdConfigKeyValueEntry(String key, String value, boolean commentedOut, boolean indented) {
			this.key = key;
			this.value = value;
			this.commentedOut = commentedOut;
			this.indented = indented;
		}

		@Override
		public String getFormattedLine() {
			if(indented) {
				if(commentedOut) {
					return String.format("#\t%s %s", this.key, this.value);
				}
				return String.format("\t%s %s", this.key, this.value);
			}
			if(commentedOut) {
				return String.format("#%s %s", this.key, this.value);
			}
			return String.format("%s %s", this.key, this.value);
		}

		public boolean hasNext() {
			return Objects.nonNull(next);
		}
		
		public SshdConfigKeyValueEntry getNext() {
			if(!hasNext()) {
				return null;
			}
			return next;
		}
		
		public void setNext(SshdConfigFileEntry next) {
			if(!((SshdConfigKeyValueEntry)next).getKey().equals(getKey())) {
				throw new IllegalArgumentException("Next value and this entry must have the same key " + getKey());
			}
			this.next = (SshdConfigKeyValueEntry) next;
		}
		
		@Override
		public boolean isCommentedOut() {
			return commentedOut;
		}
		
		@Override
		public String toString() {
			return getFormattedLine();
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
		
		public boolean hasParts() {
			return this.value.contains(",") || this.value.contains(" ");
		}
		
		public boolean hasCommaSV() {
			return this.value.contains(",");
		}
		
		public boolean hasSpaceSV() {
			return this.value.contains(" ");
		}
		
		public String[] getValueParts() {
			if (this.value.contains(",")) {
				return this.value.split(",");
			} else if (this.value.contains(" ")) {
				return this.value.split("\\s");
			} else {
				return new String[] {this.value};
			}
		}

		@Override
		public void setCommentedOut(boolean commentedOut) {
			this.commentedOut = commentedOut;
		}
		
	}
