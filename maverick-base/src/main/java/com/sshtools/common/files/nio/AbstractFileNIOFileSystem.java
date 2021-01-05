/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.common.files.nio;

import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;

public class AbstractFileNIOFileSystem extends FileSystem {

	URI uri;
	SshConnection con;
	AbstractFileNIOProvider provider;
	
	static Set<String> supportedViews = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("basic")));
	
	public AbstractFileNIOFileSystem(SshConnection con, URI uri, AbstractFileNIOProvider provider) {
		this.con = con;
		this.uri = uri;
		this.provider = provider;
	}

	@Override
	public FileSystemProvider provider() {
		return provider;
	}

	@Override
	public void close() throws IOException {
	}

	@Override
	public boolean isOpen() {
		return true;
	}

	@Override
	public boolean isReadOnly() {
		return false;
	}

	@Override
	public String getSeparator() {
		return AbstractFilePath.SEPARATOR;
	}

	@Override
	public Iterable<Path> getRootDirectories() {
		return new ArrayList<Path>(Arrays.asList(getRootPath()));
	}

	@Override
	public Iterable<FileStore> getFileStores() {
		return new ArrayList<FileStore>(Arrays.asList(new FileStore() {
			
			@Override
			public String type() {
				return "abfs";
			}
			
			@Override
			public boolean supportsFileAttributeView(String name) {
				return "basic".equals(name);
			}
			
			@Override
			public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
				return type.getName().equals("basic");
			}
			
			@Override
			public String name() {
				return "abfs";
			}
			
			@Override
			public boolean isReadOnly() {
				return false;
			}
			
			@Override
			public long getUsableSpace() throws IOException {
				return 0;
			}
			
			@Override
			public long getUnallocatedSpace() throws IOException {
				return 0;
			}
			
			@Override
			public long getTotalSpace() throws IOException {
				return 0;
			}
			
			@Override
			public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
				return null;
			}
			
			@Override
			public Object getAttribute(String attribute) throws IOException {
				// TODO Auto-generated method stub
				return null;
			}
		}));
	}

	public Iterator<Path> iterator(Path path, Filter<? super Path> filter) throws IOException {
		try {
			AbstractFile obj = AbstractFileNIOProvider.toAbstractFilePath(path).getAbstractFile();
			List<AbstractFile> children = obj.getChildren();
			return new Iterator<Path>() {
				int index;

				@Override
				public boolean hasNext() {
					return index < children.size();
				}

				@Override
				public Path next() {
					return path.resolve(children.get(index++).getName());
				}
			};
		} catch (PermissionDeniedException e) {
			throw new IOException(e);
		}
	}
	
	@Override
	public Set<String> supportedFileAttributeViews() {
		return supportedViews;
	}

	@Override
	public Path getPath(String first, String... more) {
		
		boolean absolute = first.startsWith(AbstractFilePath.SEPARATOR);
		if(absolute) {
			first = first.substring(1);
		}
		List<String> elements = new ArrayList<>();
		elements.add(first);
		if(more.length > 0) {
			elements.addAll(Arrays.asList(more));
		}
		return new AbstractFilePath(this, elements, absolute);
	}

	@Override
	public PathMatcher getPathMatcher(String syntaxAndPattern) {
		
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UserPrincipalLookupService getUserPrincipalLookupService() {
		throw new UnsupportedOperationException();
	}

	@Override
	public WatchService newWatchService() throws IOException {
		throw new UnsupportedOperationException();
	}

	public void assertOpen() {

	}

	public AbstractFilePath getRootPath() {
		return new AbstractFilePath(this, new ArrayList<>(), true);
	}

	public SshConnection getConnection() {
		return con;
	}

}
