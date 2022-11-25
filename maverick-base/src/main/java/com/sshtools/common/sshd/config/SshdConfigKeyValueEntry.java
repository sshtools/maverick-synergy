/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */
package com.sshtools.common.sshd.config;

import java.util.NoSuchElementException;
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
				throw new NoSuchElementException();
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