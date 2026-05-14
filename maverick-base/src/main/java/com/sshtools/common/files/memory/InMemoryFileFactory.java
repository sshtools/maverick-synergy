package com.sshtools.common.files.memory;

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

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.permissions.PermissionDeniedException;

/**
 * An in-memory {@link AbstractFileFactory} backed by a simple tree of nodes.
 * Thread-safe: all structural mutations synchronize on the factory instance.
 */
public class InMemoryFileFactory implements AbstractFileFactory<InMemoryFile> {

	/** A single node in the in-memory filesystem tree. */
	static final class Node {
		final String name;
		final boolean directory;
		byte[] content = new byte[0];
		final Map<String, Node> children;
		long lastModified = System.currentTimeMillis();
		Node parent;

		Node(String name, boolean directory, Node parent) {
			this.name = name;
			this.directory = directory;
			this.parent = parent;
			this.children = directory ? new LinkedHashMap<>() : null;
		}
	}

	private final Node root;
	private final String home;

	/** Creates a factory whose home (default) path is {@code /}. */
	public InMemoryFileFactory() {
		this("/");
	}

	/**
	 * Creates a factory with the given absolute home path. Intermediate
	 * directories are created automatically.
	 *
	 * @param home absolute home path, e.g. {@code "/home/user"}
	 */
	public InMemoryFileFactory(String home) {
		this.root = new Node("", true, null); // root node, name="" represents "/"
		this.home = normalizePath(home);
		if (!"/".equals(this.home)) {
			ensureDir(this.home);
		}
	}

	@Override
	public InMemoryFile getFile(String path) throws PermissionDeniedException, IOException {
		if (path == null || path.isEmpty()) {
			return new InMemoryFile(home, this);
		}
		String absolutePath = path.startsWith("/")
				? normalizePath(path)
				: normalizePath(home + "/" + path);
		return new InMemoryFile(absolutePath, this);
	}

	@Override
	public InMemoryFile getDefaultPath() throws PermissionDeniedException, IOException {
		return getFile("");
	}

	// -------------------------------------------------------------------------
	// Package-private tree operations (all synchronized on this)
	// -------------------------------------------------------------------------

	/** Returns the node at the given absolute path, or {@code null} if absent. */
	synchronized Node resolve(String absolutePath) {
		if ("/".equals(absolutePath) || absolutePath.isEmpty()) {
			return root;
		}
		String[] parts = absolutePath.substring(1).split("/", -1);
		Node current = root;
		for (String part : parts) {
			if (part.isEmpty()) continue;
			current = current.children.get(part);
			if (current == null) return null;
		}
		return current;
	}

	/**
	 * Ensures every component along {@code absolutePath} exists as a directory,
	 * creating any that are missing. Returns the leaf directory node.
	 */
	synchronized Node ensureDir(String absolutePath) {
		if ("/".equals(absolutePath) || absolutePath.isEmpty()) {
			return root;
		}
		String[] parts = absolutePath.substring(1).split("/", -1);
		Node current = root;
		for (String part : parts) {
			if (part.isEmpty()) continue;
			Node next = current.children.get(part);
			if (next == null) {
				next = new Node(part, true, current);
				current.children.put(part, next);
				current.lastModified = System.currentTimeMillis();
			} else if (!next.directory) {
				throw new IllegalStateException(
						"Path component is a file, not a directory: " + part);
			}
			current = next;
		}
		return current;
	}

	/**
	 * Creates a file node at the given absolute path. The parent directory must
	 * already exist. Returns the existing node if one is already present.
	 */
	synchronized Node createFile(String absolutePath) throws IOException {
		String parentPath = getParentPath(absolutePath);
		String name = getName(absolutePath);
		Node parent = resolve(parentPath);
		if (parent == null) {
			throw new IOException("Parent directory does not exist: " + parentPath);
		}
		if (!parent.directory) {
			throw new IOException("Parent is not a directory: " + parentPath);
		}
		Node existing = parent.children.get(name);
		if (existing != null) {
			return existing;
		}
		Node node = new Node(name, false, parent);
		parent.children.put(name, node);
		parent.lastModified = System.currentTimeMillis();
		return node;
	}

	/**
	 * Removes the node at the given absolute path from its parent.
	 * Returns {@code true} if a node was removed.
	 */
	synchronized boolean delete(String absolutePath) {
		Node node = resolve(absolutePath);
		if (node == null || node == root) return false;
		Node parent = node.parent;
		if (parent != null) {
			parent.children.remove(node.name);
			parent.lastModified = System.currentTimeMillis();
		}
		return true;
	}

	// -------------------------------------------------------------------------
	// Static path helpers
	// -------------------------------------------------------------------------

	/** Normalises an absolute or relative path into an absolute canonical path. */
	static String normalizePath(String path) {
		if (path == null || path.isEmpty()) return "/";
		String[] parts = path.split("/", -1);
		Deque<String> stack = new ArrayDeque<>();
		for (String part : parts) {
			if (part.isEmpty() || ".".equals(part)) continue;
			if ("..".equals(part)) {
				if (!stack.isEmpty()) stack.pollLast();
			} else {
				stack.addLast(part);
			}
		}
		if (stack.isEmpty()) return "/";
		StringBuilder sb = new StringBuilder();
		for (String part : stack) {
			sb.append('/').append(part);
		}
		return sb.toString();
	}

	/** Returns the parent path of an absolute path, or {@code null} for {@code /}. */
	static String getParentPath(String absolutePath) {
		if ("/".equals(absolutePath)) return null;
		int idx = absolutePath.lastIndexOf('/');
		if (idx <= 0) return "/";
		return absolutePath.substring(0, idx);
	}

	/** Returns the last name component of an absolute path. */
	static String getName(String absolutePath) {
		if ("/".equals(absolutePath)) return "";
		int idx = absolutePath.lastIndexOf('/');
		return absolutePath.substring(idx + 1);
	}
}
