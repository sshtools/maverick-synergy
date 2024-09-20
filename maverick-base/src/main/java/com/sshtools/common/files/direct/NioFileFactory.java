package com.sshtools.common.files.direct;

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
		 * Configure the current directory to be the directory to use as the home of this
		 * file factory, i.e. that returned by <code>System.getProperty("user.dir");</code>.
		 * 
		 *  @return this for chaining
		 */
		public NioFileFactoryBuilder withCurrentDirectoryAsHome() {
			return withHome(Paths.get(System.getProperty("user.dir")));
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
	
	Path home() {
		return home;
	}
	
	boolean isSandboxed() {
		return sandbox;
	}

}
