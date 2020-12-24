/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.common.files;

import java.io.IOException;

import com.sshtools.common.events.Event;
import com.sshtools.common.files.ReadOnlyFileFactoryAdapter.ReadOnlyAbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;

public class ReadOnlyFileFactoryAdapter implements AbstractFileFactory<ReadOnlyAbstractFile> {

	AbstractFileFactory<?> fileFactory;
	
	public ReadOnlyFileFactoryAdapter(AbstractFileFactory<?> fileFactory) {
		this.fileFactory = fileFactory;
	}
	
	@Override
	public ReadOnlyAbstractFile getFile(String path) throws PermissionDeniedException, IOException {
		return new ReadOnlyAbstractFile(fileFactory.getFile(path));
	}

	@Override
	public Event populateEvent(Event evt) {
		return evt;
	}

	@Override
	public ReadOnlyAbstractFile getDefaultPath() throws PermissionDeniedException, IOException {
		return new ReadOnlyAbstractFile(fileFactory.getDefaultPath());
	}
	
	public static class ReadOnlyAbstractFile extends AbstractFileAdapter {

		@Override
		public boolean isWritable() throws IOException {
			return false;
		}

		public ReadOnlyAbstractFile(AbstractFile file) {
			super(file);
		}
	}
}
