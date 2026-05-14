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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sshtools.client.sftp.SftpClient;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.Utils;

public class SftpFileSystem extends FileSystem {

	private final SftpFileSystemProvider fileSystemProvider;
	private final SftpClient sftp;
	private final Path rootPath;
	private final boolean closeSftpOnFileSystemClose;
	private final URI uri;
	private boolean closed;

	SftpFileSystem(SftpClient sftp, SftpFileSystemProvider fileSystemProvider, Optional<String> rootPath,
			boolean closeSftpOnFileSystemClose, URI uri) {
		this.fileSystemProvider = fileSystemProvider;
		this.sftp = sftp;
		this.rootPath = new SftpPath(this, rootPath.orElseGet(() -> {
			try {
				return sftp.getDefaultDirectory();
			} catch (SftpStatusException  | SshException e) {
				throw new UncheckedIOException(new IOException(e));
			}
		}));
		this.closeSftpOnFileSystemClose = closeSftpOnFileSystemClose;
		this.uri = uri;
	}

	@Override
	public void close() throws IOException {
		if (!closed) {
			closed = true;
			try {
				if (closeSftpOnFileSystemClose)
					sftp.close();
			} finally {
				if (rootPath != null)
					fileSystemProvider.remove(uri);
			}
		}
	}

	static String toAbsolutePathString(Path path) {
		return path.toAbsolutePath().toString();
	}

	public Path getDefaultDir() {
		return rootPath;
	}

	@Override
	public Iterable<FileStore> getFileStores() {
		return Arrays.asList(new SftpFileStore(sftp, rootPath.toString()));
	}

	@Override
	public Path getPath(String first, String... more) {
		StringBuilder sb = new StringBuilder();
		if (first != null && first.length() > 0) {
			appendDedupSep(sb, first.replace('\\', '/')); // in case we are running on Windows
		}

		if (more.length > 0) {
			for (String segment : more) {
				if ((sb.length() > 0) && (sb.charAt(sb.length() - 1) != '/')) {
					sb.append('/');
				}
				// in case we are running on Windows
				appendDedupSep(sb, segment.replace('\\', '/'));
			}
		}

		if ((sb.length() > 1) && (sb.charAt(sb.length() - 1) == '/')) {
			sb.setLength(sb.length() - 1);
		}

		String path = sb.toString();
		String root = null;
		if (path.startsWith("/")) {
			root = "/";
			path = path.substring(1);
		}
		if(path.equals("")) {
			return create(root);
		}
		else {
			String[] names = path.split("/");
			return create(root, names);
		}
	}

	@Override
	public PathMatcher getPathMatcher(String syntaxAndPattern) {
		int colonIndex = syntaxAndPattern.indexOf(':');
		if ((colonIndex <= 0) || (colonIndex == syntaxAndPattern.length() - 1)) {
			throw new IllegalArgumentException(
					"syntaxAndPattern must have form \"syntax:pattern\" but was \"" + syntaxAndPattern + "\"");
		}

		String syntax = syntaxAndPattern.substring(0, colonIndex);
		String pattern = syntaxAndPattern.substring(colonIndex + 1);
		String expr;
		switch (syntax) {
		case "glob":
			expr = globToRegex(pattern);
			break;
		case "regex":
			expr = pattern;
			break;
		default:
			throw new UnsupportedOperationException("Unsupported path matcher syntax: \'" + syntax + "\'");
		}
		final Pattern regex = Pattern.compile(expr);
		return new PathMatcher() {
			@Override
			public boolean matches(Path path) {
				String str = path.toString();
				Matcher m = regex.matcher(str);
				return m.matches();
			}
		};
	}

	@Override
	public Iterable<Path> getRootDirectories() {
		return Collections.<Path>singleton(create("/"));
	}

	@Override
	public String getSeparator() {
		return "/";
	}

	@Override
	public UserPrincipalLookupService getUserPrincipalLookupService() {
		throw new UnsupportedOperationException("UserPrincipalLookupService is not supported.");
	}

	@Override
	public boolean isOpen() {
		return !closed;
	}

	@Override
	public boolean isReadOnly() {
		return false;
	}

	@Override
	public WatchService newWatchService() throws IOException {
		throw new UnsupportedOperationException("Watch service N/A");
	}

	@Override
	public SftpFileSystemProvider provider() {
		return fileSystemProvider;
	}

	@Override
	public Set<String> supportedFileAttributeViews() {
		return SftpFileAttributeViews.viewNames();
	}

	SftpClient getSftp() {
		return sftp;
	}

	protected void appendDedupSep(StringBuilder sb, CharSequence s) {
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			if ((ch != '/') || (sb.length() == 0) || (sb.charAt(sb.length() - 1) != '/')) {
				sb.append(ch);
			}
		}
	}

	protected SftpPath create(String root, ImmutableList<String> names) {
		return new SftpPath(this, root, names);
	}

	protected Path create(String root, String... names) {
		return create(root, new ImmutableList<>(names));
	}

	protected String globToRegex(String pattern) {
		StringBuilder sb = new StringBuilder(pattern.length());
		int inGroup = 0;
		int inClass = 0;
		boolean inQE = false;
		int firstIndexInClass = -1;
		char[] arr = pattern.toCharArray();
		for (int i = 0; i < arr.length; i++) {
			char ch = arr[i];
			switch (ch) {
			case '\\':
				if (++i >= arr.length) {
					sb.append('\\');
				} else {
					char next = arr[i];
					switch (next) {
					case ',':
						// escape not needed
						break;
					case 'Q':
						inQE = true;
						sb.append("\\");
						break;
					case 'E':
						// extra escape needed
						inQE = false;
						sb.append("\\");
						break;
					default:
						sb.append('\\');
						break;
					}
					sb.append(next);
				}
				break;
			default:
				if (inQE)
					sb.append(ch);
				else {
					switch (ch) {

					case '*':
						sb.append(inClass == 0 ? ".*" : "*");
						break;
					case '?':
						sb.append(inClass == 0 ? '.' : '?');
						break;
					case '[':
						inClass++;
						firstIndexInClass = i + 1;
						sb.append('[');
						break;
					case ']':
						inClass--;
						sb.append(']');
						break;
					case '.':
					case '(':
					case ')':
					case '+':
					case '|':
					case '^':
					case '$':
					case '@':
					case '%':
						if (inClass == 0 || (firstIndexInClass == i && ch == '^')) {
							sb.append('\\');
						}
						sb.append(ch);
						break;
					case '!':
						sb.append(firstIndexInClass == i ? '^' : '!');
						break;
					case '{':
						inGroup++;
						sb.append('(');
						break;
					case '}':
						inGroup--;
						sb.append(')');
						break;
					case ',':
						sb.append(inGroup > 0 ? '|' : ',');
						break;
					default:
						sb.append(ch);
					}
				}
				break;
			}
		}
		return sb.toString();
	}

	public URI toUri() {
		return URI.create(String.format("sftp://%s/%s",
				Utils.formatHostnameAndPort(sftp.getSubsystemChannel().getConnection().getRemoteIPAddress(),
						sftp.getSubsystemChannel().getConnection().getRemotePort()),
				rootPath));
	}

}
