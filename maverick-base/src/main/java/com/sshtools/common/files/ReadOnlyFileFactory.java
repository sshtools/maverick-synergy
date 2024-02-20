package com.sshtools.common.files;

import java.io.IOException;

import com.sshtools.common.events.Event;
import com.sshtools.common.files.ReadOnlyFileFactory.ReadOnlyAbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;

public class ReadOnlyFileFactory implements AbstractFileFactory<ReadOnlyAbstractFile> {

	AbstractFileFactory<?> fileFactory;
	
	public ReadOnlyFileFactory(AbstractFileFactory<?> fileFactory) {
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
	
	public class ReadOnlyAbstractFile extends AbstractFileAdapter {

		@Override
		public boolean isWritable() throws IOException {
			return false;
		}

		public ReadOnlyAbstractFile(AbstractFile file) {
			super(file);
		}

		@Override
		public AbstractFileFactory<? extends AbstractFile> getFileFactory() {
			return ReadOnlyFileFactory.this;
		}
	}
}