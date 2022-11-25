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
			throw new UnsupportedOperationException();
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