package com.sshtools.common.files;

import java.io.IOException;

import com.sshtools.common.events.Event;
import com.sshtools.common.files.ReadOnlyFileFactoryAdapter.ReadOnlyAbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;

public class ReadOnlyFileFactoryAdapter implements AbstractFileFactory<ReadOnlyAbstractFile> {

	AbstractFileFactory<?> fileFactory;
	
	public ReadOnlyFileFactoryAdapter(AbstractFileFactory<?> fileFactory) {
		this.fileFactory = fileFactory;
	}
	
	@Override
	public ReadOnlyAbstractFile getFile(String path, SshConnection con) throws PermissionDeniedException, IOException {
		return new ReadOnlyAbstractFile(fileFactory.getFile(path, con));
	}

	@Override
	public Event populateEvent(Event evt) {
		return evt;
	}

	@Override
	public ReadOnlyAbstractFile getDefaultPath(SshConnection con) throws PermissionDeniedException, IOException {
		return new ReadOnlyAbstractFile(fileFactory.getDefaultPath(con));
	}
	
	static class ReadOnlyAbstractFile extends AbstractFileAdapter {

		@Override
		public boolean isWritable() throws IOException {
			return false;
		}

		public ReadOnlyAbstractFile(AbstractFile file) {
			super(file);
		}
	}
}
