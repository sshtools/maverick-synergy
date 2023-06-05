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
 * Copyright (C) 2002-2023 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */
package com.sshtools.synergy.niofs;

import static com.sshtools.synergy.niofs.SftpFileSystem.toAbsolutePathString;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.AbstractList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;

public class SftpPath implements Path {

	private final ImmutableList<String> names;
    private final String root;
    private final SftpFileSystem fileSystem;

    SftpPath(SftpFileSystem fileSystem, String root, ImmutableList<String> names) {
        this.fileSystem = fileSystem;
        this.root = root;
        this.names = names;
    }

    SftpPath(SftpFileSystem fileSystem, String root, String... names) {
    	this(fileSystem, root, new ImmutableList<>(names));
    }

	@Override
    public int compareTo(Path paramPath) {
        var p2 = paramPath == null ? null : checkPath(paramPath);
        int c = compare(root, p2 == null ? null : p2.root);
        if (c != 0) {
            return c;
        }
        for (int i = 0; i < Math.min(names.size(), p2 == null ? Integer.MAX_VALUE : p2.names.size()); i++) {
            String n1 = names.get(i);
            String n2 = p2 == null ? null : p2.names.get(i);
            c = compare(n1, n2);
            if (c != 0) {
                return c;
            }
        }
        return names.size() - (p2 == null ? 0 : p2.names.size());
    }

	@Override
    public boolean endsWith(Path other) {
    	var otherFs = other instanceof SftpPath ? ((SftpPath)other).fileSystem : null;
    	var otherRoot = other instanceof SftpPath ? ((SftpPath)other).root : null;
    	var otherNames = other instanceof SftpPath ? ((SftpPath)other).names: null;
    	
//        if (other.isAbsolute()) {
//            return compareTo(other) == 0;
//        }
        return Objects.equals(getFileSystem(), otherFs)
                && Objects.equals(root, otherRoot)
                && endsWith(names, otherNames);
    }

	@Override
    public boolean endsWith(String other) {
        return endsWith(getFileSystem().getPath(other));
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Path
                && compareTo((Path) obj) == 0;
    }

    @Override
    public Path getFileName() {
        if (!names.isEmpty()) {
            return create(null, names.get(names.size() - 1));
        }
        return null;
    }

    @Override
    public SftpFileSystem getFileSystem() {
        return fileSystem;
    }

    @Override
    public Path getName(int index) {
        int maxIndex = getNameCount();
        if ((index < 0) || (index >= maxIndex)) {
            throw new IllegalArgumentException("Invalid name index " + index + " - not in range [0-" + maxIndex + "]");
        }
        return create(null, names.subList(index, index + 1));
    }

    @Override
    public int getNameCount() {
        return names.size();
    }

    @Override
    public Path getParent() {
        if (names.isEmpty() || ((names.size() == 1) && (root == null))) {
            return null;
        }
        return create(root, names.subList(0, names.size() - 1));
    }

    @Override
    public Path getRoot() {
        if (isAbsolute()) {
            return create(root);
        }
        return null;
    }

    @Override
    public int hashCode() {
        int hash = Objects.hashCode(getFileSystem());
        // use hash codes from toString() form of names
        hash = 31 * hash + Objects.hashCode(root);
        for (String name : names) {
            hash = 31 * hash + Objects.hashCode(name);
        }
        return hash;
    }

    @Override
    public boolean isAbsolute() {
        return root != null;
    }

    @Override
    public Iterator<Path> iterator() {
        return new AbstractList<Path>() {
            @Override
            public Path get(int index) {
                return getName(index);
            }

            @Override
            public int size() {
                return getNameCount();
            }
        }.iterator();
    }

    @Override
    public Path normalize() {
        if (isNormal()) {
            return this;
        }

        Deque<String> newNames = new ArrayDeque<>();
        for (String name : names) {
            if (name.equals("..")) {
                String lastName = newNames.peekLast();
                if (lastName != null && !lastName.equals("..")) {
                    newNames.removeLast();
                } else if (!isAbsolute()) {
                    // if there's a root and we have an extra ".." that would go up above the root, ignore it
                    newNames.add(name);
                }
            } else if (!name.equals(".")) {
                newNames.add(name);
            }
        }

        return create(root, newNames);
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) throws IOException {
        throw new UnsupportedOperationException("Register to watch " + toAbsolutePath() + " N/A");
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
        throw new UnsupportedOperationException("Register to watch " + toAbsolutePath() + " N/A");
    }

    @Override
    public Path relativize(Path other) {
    	SftpPath p2 = checkPath(other);
        if (!Objects.equals(getRoot(), p2.getRoot())) {
            throw new IllegalArgumentException("Paths have different roots: " + this + ", " + other);
        }
        if (p2.equals(this)) {
            return create(null);
        }
        if (root == null && names.isEmpty()) {
            return p2;
        }
        // Common subsequence
        int sharedSubsequenceLength = 0;
        for (int i = 0; i < Math.min(names.size(), p2.names.size()); i++) {
            if (names.get(i).equals(p2.names.get(i))) {
                sharedSubsequenceLength++;
            } else {
                break;
            }
        }
        int extraNamesInThis = Math.max(0, names.size() - sharedSubsequenceLength);
        List<String> extraNamesInOther = (p2.names.size() <= sharedSubsequenceLength)
                ? Collections.<String>emptyList()
                : p2.names.subList(sharedSubsequenceLength, p2.names.size());
        List<String> parts = new ArrayList<>(extraNamesInThis + extraNamesInOther.size());
        // add .. for each extra name in this path
        parts.addAll(Collections.nCopies(extraNamesInThis, ".."));
        // add each extra name in the other path
        parts.addAll(extraNamesInOther);
        return create(null, parts);
    }

    @Override
    public Path resolve(Path other) {
        SftpPath p2 = checkPath(other);
        if (p2.isAbsolute()) {
            return p2;
        }
        if (p2.names.isEmpty()) {
            return this;
        }
        String[] names = new String[this.names.size() + p2.names.size()];
        int index = 0;
        for (String p : this.names) {
            names[index++] = p;
        }
        for (String p : p2.names) {
            names[index++] = p;
        }
        return create(root, names);
    }

    @Override
    public Path resolve(String other) {
        return resolve(getFileSystem().getPath(other));
    }

    @Override
    public Path resolveSibling(Path other) {
        Path parent = getParent();
        return parent == null ? other : parent.resolve(other);
    }

    @Override
    public Path resolveSibling(String other) {
        return resolveSibling(getFileSystem().getPath(other));
    }

    @Override
    public boolean startsWith(Path other) {
    	var otherFs = other instanceof SftpPath ? ((SftpPath)other).fileSystem : null;
    	var otherRoot = other instanceof SftpPath ? ((SftpPath)other).root : null;
    	var otherNames = other instanceof SftpPath ? ((SftpPath)other).names: null;
        return Objects.equals(getFileSystem(), otherFs)
                && Objects.equals(root, otherRoot)
                && startsWith(names, otherNames);
    }

    @Override
    public boolean startsWith(String other) {
        return startsWith(getFileSystem().getPath(other));
    }

    @Override
    public Path subpath(int beginIndex, int endIndex) {
        int maxIndex = getNameCount();
        if ((beginIndex < 0) || (beginIndex >= maxIndex) || (endIndex > maxIndex) || (beginIndex >= endIndex)) {
            throw new IllegalArgumentException("subpath(" + beginIndex + "," + endIndex + ") bad index range - allowed [0-" + maxIndex + "]");
        }
        return create(null, names.subList(beginIndex, endIndex));
    }

    @Override
    public Path toAbsolutePath() {
        if (isAbsolute()) {
            return this;
        }
        Path dir = fileSystem.getDefaultDir();
		return dir.resolve(this);
    }

    @Override
    public File toFile() {
        throw new UnsupportedOperationException("To file " + toAbsolutePath() + " N/A");
    }

    @Override
	public Path toRealPath(LinkOption... options) throws IOException {
    	var opts = Arrays.asList(options);
		var absolute = toAbsolutePath();
		var fs = getFileSystem();
		var provider = fs.provider();
		if(!opts.contains(LinkOption.NOFOLLOW_LINKS)) {
			try {
				var sftpPath = toAbsolutePathString(this);
				var linkTarget = fileSystem.getSftp().getSymbolicLinkTarget(sftpPath);
				absolute = absolute.getParent().resolve(linkTarget);
			} catch (SftpStatusException | SshException e) {
				/* Assume not a link. Saves making 2 calls */
			}
		}
		provider.checkAccess(absolute);
		return absolute;
	}

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (root != null) {
            sb.append(root);
        }
        
        String separator = getFileSystem().getSeparator();
        for (String name : names) {
            if ((sb.length() > 0) && (sb.charAt(sb.length() - 1) != '/')) {
                sb.append(separator);
            }
            sb.append(name);
        }
        return sb.toString();
    }

    @Override
    public URI toUri() {
        return getFileSystem().toUri().resolve(toAbsolutePath().toString());
    }

    protected SftpPath checkPath(Path paramPath) {
        if (paramPath.getClass() != getClass()) {
            throw new ProviderMismatchException("Path is not of this class: " + paramPath + "[" + paramPath.getClass().getSimpleName() + "]");
        }
        SftpPath t = (SftpPath) paramPath;

        FileSystem SftpFileSystem = t.getFileSystem();
        if (SftpFileSystem.provider() != this.fileSystem.provider()) {
            throw new ProviderMismatchException("Mismatched providers for " + t);
        }
        return t;
    }

    protected int compare(String s1, String s2) {
        if (s1 == null) {
            return s2 == null ? 0 : -1;
        } else {
            return s2 == null ? +1 : s1.compareTo(s2);
        }
    }

    protected SftpPath create(String root, Collection<String> names) {
        return create(root, new ImmutableList<>(names.toArray(new String[names.size()])));
    }

    protected SftpPath create(String root, ImmutableList<String> names) {
        return fileSystem.create(root, names);
    }

    protected SftpPath create(String root, String... names) {
        return create(root, new ImmutableList<>(names));
    }

    protected boolean endsWith(List<?> list, List<?> other) {
        return other.size() <= list.size() && list.subList(list.size() - other.size(), list.size()).equals(other);
    }

    protected boolean isNormal() {
        int count = getNameCount();
        if ((count == 0) || ((count == 1) && !isAbsolute())) {
            return true;
        }
        boolean foundNonParentName = isAbsolute(); // if there's a root, the path doesn't start with ..
        boolean normal = true;
        for (String name : names) {
            if (name.equals("..")) {
                if (foundNonParentName) {
                    normal = false;
                    break;
                }
            } else {
                if (name.equals(".")) {
                    normal = false;
                    break;
                }
                foundNonParentName = true;
            }
        }
        return normal;
    }

    protected boolean startsWith(List<?> list, List<?> other) {
        return list.size() >= other.size() && list.subList(0, other.size()).equals(other);
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
		var zfv = SftpFileAttributeViews.get(this, view);
		if (zfv == null) {
			throw new UnsupportedOperationException("view not supported");
		}
		return zfv.readAttributes(attrs);
	}

    void setAttribute(String attribute, Object value, LinkOption... options) throws IOException {
		String type = null;
		String attr = null;
		int colonPos = attribute.indexOf(':');
		if (colonPos == -1) {
			type = "basic";
			attr = attribute;
		} else {
			type = attribute.substring(0, colonPos++);
			attr = attribute.substring(colonPos);
		}
		var view = SftpFileAttributeViews.get(this, type);
		if (view == null)
			throw new UnsupportedOperationException("view <" + view + "> is not supported");
		view.setAttribute(attr, value);
	}

}
