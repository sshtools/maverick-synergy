package com.sshtools.common.files.direct;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import com.sshtools.common.events.Event;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.permissions.PermissionDeniedException;

public final class NioFileFactory implements AbstractFileFactory<NioFile> {
	
	public final static class NioFileFactoryBuilder {

		private Optional<Path> home = Optional.empty();
		private boolean sandbox = true;
		
		/**
		 * Create a new {@link NioFileFactoryBuilder}.
		 * 
		 * @return build
		 */
		public static NioFileFactoryBuilder create() {
			return new NioFileFactoryBuilder();
		}
		
		private NioFileFactoryBuilder() {
		}
		
		/**
		 * Configure whether all file operations should be restricted 
		 * to the default home directory or that provided. By default
		 * this is <code>true</code>.
		 * 
		 * @param sandbox sandbox
		 * @return this for chaining
		 */
		public NioFileFactoryBuilder withSandbox(boolean sandbox) {
			this.sandbox = sandbox;
			return this;
		}
		
		/**
		 * Remove the sandbox, so any files may be accessed outside of the configured home.
		 * 
		 * @return this for chaining
		 */
		public NioFileFactoryBuilder withoutSandbox() {
			return withSandbox(false);
		}
		
		/**
		 * Configure the directory to use as the home of this
		 * file factory. If not provided, the current user's home
		 * directory will be used, i.e. that returned by <code>System.getProperty("user.home");</code>). 
		 * 
		 *  @param home
		 *  @return this for chaining
		 */
		public NioFileFactoryBuilder withHome(Path home) {
			this.home = Optional.of(home);
			return this;
		}
		
		/**
		 * Configure the directory to use as the home of this
		 * file factory. If not provided, the current user's home
		 * directory will be used, i.e. that returned by <code>System.getProperty("user.home");</code>).
		 * 
		 *  @param home
		 *  @return this for chaining
		 */
		public NioFileFactoryBuilder withHome(File home) {
			return withHome(home.toPath());
		}
		
		/**
		 * Build a new {@link NioFileFactory}.
		 * 
		 * @return factory
		 */
		public NioFileFactory build() {
			return new NioFileFactory(this);
		}
		
	}

	private final Path home;
	private final boolean sandbox;


	private NioFileFactory(NioFileFactoryBuilder nioFileFactoryBuilder) {
		this.home = nioFileFactoryBuilder.home.orElseGet(() -> Paths.get(System.getProperty("user.home")));
		this.sandbox = nioFileFactoryBuilder.sandbox;
	}

	@Override
	public NioFile getFile(String path) throws PermissionDeniedException, IOException {
		return new NioFile(path, this, home, sandbox);
	}

	@Override
	public Event populateEvent(Event evt) {
		return evt;
	}

	@Override
	public NioFile getDefaultPath() throws PermissionDeniedException, IOException {
		return getFile("");
	}

}
