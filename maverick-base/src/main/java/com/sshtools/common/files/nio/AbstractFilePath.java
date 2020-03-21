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
package com.sshtools.common.files.nio;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.policy.FileSystemPolicy;
import com.sshtools.common.ssh.SshConnection;


public class AbstractFilePath implements Path {

	private static final String CURRENT_DIR = ".";
	private static final String PARENT_DIR = "..";
	public static final String SEPARATOR = "/";
	
	private final AbstractFileNIOFileSystem fileSystem;
	private final List<String> elements;
	private final boolean absolute;
	private final SshConnection con;
	
	AbstractFilePath(AbstractFileNIOFileSystem fileSystem, List<String> elements, boolean absolute) {
		fileSystem.assertOpen();
		this.fileSystem = Objects.requireNonNull(fileSystem);
		this.elements = Collections.unmodifiableList(elements);
		this.absolute = Objects.requireNonNull(absolute);
		this.con = fileSystem.getConnection();
	}

	static AbstractFilePath castAndAssertAbsolute(Path path) {
		AbstractFilePath result = cast(path);
		if (!result.isAbsolute()) {
			throw new IllegalArgumentException("Path must be absolute but was " + path);
		}
		return result;
	}

	static AbstractFilePath cast(Path path) {
		if (path instanceof AbstractFilePath) {
			AbstractFilePath AbstractFilePath = (AbstractFilePath) path;
			AbstractFilePath.getFileSystem().assertOpen();
			return AbstractFilePath;
		} else {
			throw new ProviderMismatchException("Used a path from different provider: " + path);
		}
	}

	@Override
	public AbstractFileNIOFileSystem getFileSystem() {
		return fileSystem;
	}

	@Override
	public boolean isAbsolute() {
		fileSystem.assertOpen();
		return absolute;
	}

	@Override
	public AbstractFilePath getRoot() {
		fileSystem.assertOpen();
		return absolute ? fileSystem.getRootPath() : null;
	}

	@Override
	public AbstractFilePath getFileName() {
		fileSystem.assertOpen();
		int elementCount = getNameCount();
		if (elementCount == 0) {
			return null;
		} else {
			return getName(elementCount - 1);
		}
	}

	@Override
	public AbstractFilePath getParent() {
		fileSystem.assertOpen();
		int elementCount = getNameCount();
		if (elementCount > 1) {
			List<String> elems = elements.subList(0, elementCount - 1);
			return copyWithElements(elems);
		} else if (elementCount == 1) {
			return getRoot();
		} else {
			return null; // only root and the "empty" path don't have a parent
		}
	}

	@Override
	public int getNameCount() {
		fileSystem.assertOpen();
		return elements.size();
	}

	@Override
	public AbstractFilePath getName(int index) {
		fileSystem.assertOpen();
		return subpath(index, index + 1);
	}

	@Override
	public AbstractFilePath subpath(int beginIndex, int endIndex) {
		fileSystem.assertOpen();
		return new AbstractFilePath(fileSystem, elements.subList(beginIndex, endIndex), false);
	}

	@Override
	public boolean startsWith(Path path) {
		fileSystem.assertOpen();
		if (!this.getFileSystem().equals(path.getFileSystem())) {
			return false;
		}
		AbstractFilePath other = cast(path);
		boolean matchesAbsolute = this.isAbsolute() == other.isAbsolute();
		if (matchesAbsolute && other.elements.size() <= this.elements.size()) {
			return this.elements.subList(0, other.elements.size()).equals(other.elements);
		} else {
			return false;
		}
	}

	@Override
	public boolean startsWith(String other) {
		fileSystem.assertOpen();
		return startsWith(fileSystem.getPath(other));
	}

	@Override
	public boolean endsWith(Path path) {
		fileSystem.assertOpen();
		if (!this.getFileSystem().equals(path.getFileSystem())) {
			return false;
		}
		AbstractFilePath other = cast(path);
		if (other.elements.size() <= this.elements.size()) {
			return this.elements.subList(this.elements.size() - other.elements.size(), this.elements.size()).equals(other.elements);
		} else {
			return false;
		}
	}

	@Override
	public boolean endsWith(String other) {
		fileSystem.assertOpen();
		return endsWith(fileSystem.getPath(other));
	}

	@Override
	public AbstractFilePath normalize() {
		fileSystem.assertOpen();
		LinkedList<String> normalized = new LinkedList<>();
		for (String elem : elements) {
			String lastElem = normalized.peekLast();
			if (elem.isEmpty() || CURRENT_DIR.equals(elem)) {
				continue;
			} else if (PARENT_DIR.equals(elem) && lastElem != null && !PARENT_DIR.equals(lastElem)) {
				normalized.removeLast();
			} else {
				normalized.add(elem);
			}
		}
		return copyWithElements(normalized);
	}

	@Override
	public AbstractFilePath resolve(Path path) {
		fileSystem.assertOpen();
		AbstractFilePath other = cast(path);
		if (other.isAbsolute()) {
			return other;
		} else {
			List<String> joined = new ArrayList<>();
			joined.addAll(this.elements);
			joined.addAll(other.elements);
			return copyWithElements(joined);
		}
	}

	@Override
	public AbstractFilePath resolve(String other) {
		fileSystem.assertOpen();
		return resolve(fileSystem.getPath(other));
	}

	@Override
	public AbstractFilePath resolveSibling(Path path) {
		fileSystem.assertOpen();
		AbstractFilePath parent = getParent();
		AbstractFilePath other = cast(path);
		if (parent == null || other.isAbsolute()) {
			return other;
		} else {
			return parent.resolve(other);
		}
	}

	@Override
	public AbstractFilePath resolveSibling(String other) {
		fileSystem.assertOpen();
		return resolveSibling(fileSystem.getPath(other));
	}

	@Override
	public AbstractFilePath relativize(Path path) {
		fileSystem.assertOpen();
		AbstractFilePath normalized = this.normalize();
		AbstractFilePath other = cast(path).normalize();
		if (normalized.isAbsolute() == other.isAbsolute()) {
			int commonPrefix = countCommonPrefixElements(normalized, other);
			int stepsUp = this.getNameCount() - commonPrefix;
			List<String> elems = new ArrayList<>();
			elems.addAll(Collections.nCopies(stepsUp, PARENT_DIR));
			elems.addAll(other.elements.subList(commonPrefix, other.getNameCount()));
			return copyWithElementsAndAbsolute(elems, false);
		} else {
			throw new IllegalArgumentException("Can't relativize an absolute path relative to a relative path.");
		}
	}

	private int countCommonPrefixElements(AbstractFilePath p1, AbstractFilePath p2) {
		int n = Math.min(p1.getNameCount(), p2.getNameCount());
		for (int i = 0; i < n; i++) {
			if (!p1.elements.get(i).equals(p2.elements.get(i))) {
				return i;
			}
		}
		return n;
	}

	@Override
	public URI toUri() {
		fileSystem.assertOpen();
		return AbstractFileURI.create(fileSystem.getConnection(), elements.toArray(new String[elements.size()]));
	}

	@Override
	public AbstractFilePath toAbsolutePath() {
		fileSystem.assertOpen();
		if (isAbsolute()) {
			return this;
		} else {
			return copyWithAbsolute(true);
		}
	}

	@Override
	public AbstractFilePath toRealPath(LinkOption... options) throws IOException {
		fileSystem.assertOpen();
		AbstractFilePath normalized = normalize().toAbsolutePath();
		if (!Arrays.asList(options).contains(LinkOption.NOFOLLOW_LINKS)) {
			return normalized.resolveAllSymlinksInPath();
		} else {
			return normalized;
		}
	}

	private AbstractFilePath resolveAllSymlinksInPath() throws IOException {
		throw new UnsupportedOperationException("Method not implemented.");
	}

	@Override
	public File toFile() {
		fileSystem.assertOpen();
		throw new UnsupportedOperationException();
	}

	@Override
	public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
		fileSystem.assertOpen();
		throw new UnsupportedOperationException("Method not implemented.");
	}

	@Override
	public WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) throws IOException {
		fileSystem.assertOpen();
		throw new UnsupportedOperationException("Method not implemented.");
	}

	@Override
	public Iterator<Path> iterator() {
		fileSystem.assertOpen();
		return new Iterator<Path>() {

			private int idx = 0;

			@Override
			public boolean hasNext() {
				return idx < getNameCount();
			}

			@Override
			public Path next() {
				return getName(idx++);
			}
		};
	}

	@Override
	public int compareTo(Path path) {
		AbstractFilePath other = (AbstractFilePath) path;
		if (this.isAbsolute() != other.isAbsolute()) {
			return this.isAbsolute() ? -1 : 1;
		}
		for (int i = 0; i < Math.min(this.getNameCount(), other.getNameCount()); i++) {
			int result = this.elements.get(i).compareTo(other.elements.get(i));
			if (result != 0) {
				return result;
			}
		}
		return this.getNameCount() - other.getNameCount();
	}

	@Override
	public int hashCode() {
		int hash = 0;
		hash = 31 * hash + fileSystem.hashCode();
		hash = 31 * hash + elements.hashCode();
		hash = 31 * hash + (absolute ? 1 : 0);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AbstractFilePath) {
			AbstractFilePath other = (AbstractFilePath) obj;
			return this.fileSystem.equals(other.fileSystem) //
					&& this.compareTo(other) == 0;
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		String prefix = absolute ? SEPARATOR : "";
		return prefix + String.join(SEPARATOR, elements);
	}

	private AbstractFilePath copyWithElements(List<String> elements) {
		return new AbstractFilePath(fileSystem, elements, absolute);
	}

	private AbstractFilePath copyWithAbsolute(boolean absolute) {
		return new AbstractFilePath(fileSystem, elements, absolute);
	}

	private AbstractFilePath copyWithElementsAndAbsolute(List<String> elements, boolean absolute) {
		return new AbstractFilePath(fileSystem, elements, absolute);
	}

	public AbstractFile getAbstractFile() throws IOException{
		try {
			return con.getContext().getPolicy(
						FileSystemPolicy.class).getFileFactory(con)
							.getFile(toString());
		} catch (PermissionDeniedException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	public AbstractFileBasicAttributes getAttributes() throws IOException {
		return new AbstractFileBasicAttributes(getAbstractFile());
	}
	
	Map<String, Object> readAttributes(String attributes, LinkOption... options) throws IOException {
		String view = null;
		String attrs = null;
		int colonPos = attributes.indexOf(':');
		if (colonPos == -1) {
			view = "basic";
			attrs = attributes;
		} else {
			view = attributes.substring(0, colonPos++);
			attrs = attributes.substring(colonPos);
		}
		AbstractFileAttributeView zfv = AbstractFileAttributeView.get(this, view);
		if (zfv == null) {
			throw new UnsupportedOperationException("view not supported");
		}
		return zfv.readAttributes(attrs);
	}

}