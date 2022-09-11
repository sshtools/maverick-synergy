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
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */

package com.sshtools.common.files.vfs;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.direct.AbstractFileV2;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.util.FileUtils;

public class VirtualMappedFile extends VirtualFileObject implements AbstractFileV2 {

	private String absolutePath;
	private String name;
	
	Map<String,VirtualFile> cachedChildren = null;
	
	public VirtualMappedFile(String path,
			VirtualMount parentMount, VirtualFileFactory fileFactory)
			throws IOException, PermissionDeniedException {

		super(fileFactory, parentMount);
		
		toActualPath(path);

		init(parentMount.getActualFileFactory()
				.getFile(toActualPath(path)));

		absolutePath = toVirtualPath(super.getAbsolutePath());
		// canonicalPath = toVirtualPath(super.getCanonicalPath());

		int idx = absolutePath.lastIndexOf("/");
		name = absolutePath.substring(idx + 1);

	}

	VirtualMappedFile(AbstractFile actualFile,
			VirtualMount parentMount,
			VirtualFileFactory fileFactory) throws IOException,
			PermissionDeniedException {

		super(fileFactory, parentMount);

		init(actualFile);

		absolutePath = toVirtualPath(super.getAbsolutePath());
		// canonicalPath =

		int idx = absolutePath.lastIndexOf("/");
		name = absolutePath.substring(idx + 1);
	}
	
	public AbstractFile getParentFile() throws IOException, PermissionDeniedException {
		return fileFactory.getFile(FileUtils.stripLastPathElement(getAbsolutePath()));
	}
	
	@Override
	public synchronized void refresh() {
		cachedChildren = null;
		
		try {
			getParentFile().refresh();
		} catch (IOException | PermissionDeniedException e) {
		}
	
		super.refresh();
	}
	
	public AbstractFile resolveFile() {
		return file;
	}

	@Override
	public synchronized List<AbstractFile> getChildren() throws IOException,
			PermissionDeniedException {

		if(Objects.isNull(cachedChildren)) {
			cachedChildren = fileFactory.resolveChildren(this);
		}
		
		return new ArrayList<>(cachedChildren.values());
	}

	

	public AbstractFile getMappedFile() {
		return file;
	}
	
	@Override
	public String getAbsolutePath() throws IOException,
			PermissionDeniedException {
		return absolutePath;
	}

	@Override
	public String getCanonicalPath() throws IOException,
			PermissionDeniedException {
		return toVirtualPath(super.getAbsolutePath());
	}

	@Override
	public String getName() {
		return name;
	}

	private String toVirtualPath(String actualPath) throws IOException,
			FileNotFoundException, PermissionDeniedException {

		actualPath = actualPath.replace('\\', '/');

		// ./ means home
		if (actualPath.startsWith("./")) {
			actualPath = actualPath.substring(2);
		}

		if(Log.isDebugEnabled()) {
			Log.debug("Translating Actual: {}", actualPath);
		}

		actualPath = translateCanonicalPath(actualPath, parentMount.getRoot());

		String parent = translateCanonicalPath(parentMount.getRoot(), parentMount.getRoot());
		int idx = actualPath.indexOf(parent);
		String relative = actualPath.substring(idx + parent.length());
		String virtualPath = FileUtils.addTrailingSlash(parentMount
				.getMount()) + FileUtils.removeStartingSlash(relative);

		if(Log.isDebugEnabled()) {
			Log.debug("Translate Success: {}", virtualPath);
		}

		return virtualPath.equals("/") ? virtualPath : FileUtils
				.removeTrailingSlash(virtualPath);
	}

	public AbstractFile resolveFile(String child)
			throws PermissionDeniedException, IOException {

		if (child.startsWith("/")) {
			return fileFactory.getFile(child);
		} else {
			return fileFactory
					.getFile(FileUtils.addTrailingSlash(absolutePath)
							+ child);
		}
	}

	@Override
	public void copyFrom(AbstractFile src) throws IOException,
			PermissionDeniedException {

		if (src instanceof VirtualMappedFile) {
			super.copyFrom(((VirtualMappedFile) src).file);
		} else {
			super.copyFrom(src);
		}
		
		refresh();
	}

	@Override
	public void moveTo(AbstractFile target) throws IOException,
			PermissionDeniedException {

		if (target instanceof VirtualMappedFile) {
			super.moveTo(((VirtualMappedFile) target).file);
		} else {
			super.moveTo(target);
		}
		
		target.refresh();
	}

	private String toActualPath(String virtualPath) throws IOException,
			FileNotFoundException, PermissionDeniedException {

		if (virtualPath.equals("")) {
			virtualPath = parentMount.getMount();
		} else if (virtualPath.startsWith("./")) {
			virtualPath = virtualPath.replaceFirst("./",
					FileUtils.addTrailingSlash(parentMount.getMount()));
		} else if (!virtualPath.startsWith("/")) {
			virtualPath = FileUtils.addTrailingSlash(parentMount
					.getMount()) + virtualPath;
		}

		String str;
		if (virtualPath.length() > parentMount.getMount().length()) {
			str = FileUtils.addTrailingSlash(parentMount.getRoot())
					+ FileUtils.removeStartingSlash(virtualPath.substring(parentMount.getMount().length()));
		} else {
			str = parentMount.getRoot();
		}

		return translateCanonicalPath(str, parentMount.getRoot());
	}
	
	static String canonicalise(String path) {
		String[] pathElements = path.replace('\\', '/').split("/");
		List<String> canonicalisedPathEls = new ArrayList<String>();
		for(int i = 0 ; i < pathElements.length; i++) {
			if(pathElements[i].equals("..") && canonicalisedPathEls.size() > 0) 
				canonicalisedPathEls.remove(canonicalisedPathEls.size() - 1);
			else if(!pathElements[i].equals("."))
				canonicalisedPathEls.add(pathElements[i]);
		}
		return String.join("/", canonicalisedPathEls);
	}

	protected String translateCanonicalPath(String path, String securemount)
			throws FileNotFoundException, IOException,
			PermissionDeniedException {
		try {
			if(Log.isTraceEnabled()) {
				Log.trace("     Translating Canonical: {}", path);
				Log.trace("                     Mount: {} ", securemount);
			}

			String canonical = canonicalise(path);

			AbstractFile f2 = parentMount.getActualFileFactory().getFile(
					securemount);
			
			/**
			 * LDP - Ensure both paths are treated the same for canonical purposes
			 * as some systems may replace links with actual target. So if one
			 * requires canonical translation we should process the other the same.
			 */
//			containsDotDot = securemount.indexOf("..") > -1
			
			String canonical2 = canonicalise(f2.getAbsolutePath());

			if (!canonical2.endsWith("/")) {
				canonical2 += "/";
			}

			if (!canonical.endsWith("/")) {
				canonical += "/";
			}

			// Verify that the canonical path does not exit out of the mount
			if (canonical.startsWith(canonical2)) {
				if(Log.isTraceEnabled()) {
					Log.trace("          Translate Success: {}", FileUtils.removeTrailingSlash(canonical));
				}
				return FileUtils.removeTrailingSlash(canonical);
			}

			if(Log.isTraceEnabled()) {
				Log.trace("          Translate Failed: {}", FileUtils.removeTrailingSlash(canonical));
			}

			throw new FileNotFoundException("Path " + path
					+ " could not be found");

		} catch (IOException ex) {
			Log.debug(ex.getMessage(), ex);
			throw new FileNotFoundException("Path " + path
					+ " could not be found");
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(parentMount, absolutePath);
	}

	@Override
	public boolean equals(Object obj) {
		
		if(obj == this) {
			return true;
		}
		if(Objects.isNull(obj)) {
			return false;
		}
		if(getClass() != obj.getClass()) {
			return false;
		}
		VirtualMappedFile other = getClass().cast(obj);
		return Objects.equals(other.absolutePath, this.absolutePath)
				&& Objects.equals(other.parentMount, this.parentMount);
	}

	@Override
	public String readSymbolicLink() throws IOException, PermissionDeniedException {
		String linkPath = super.readSymbolicLink();
		return toVirtualPath(linkPath);
	}

	@Override
	public void symlinkTo(String target) throws IOException, PermissionDeniedException {
		super.symlinkTo(toActualPath(target));
	}
	
	public VirtualMount getParentMount() {
		return parentMount;
	}

	@Override
	public boolean isMount() {
		return false;
	}

	@Override
	public boolean createFolder() throws IOException, PermissionDeniedException {
		try {
			return super.createFolder();
		} finally {
			refresh();
		}
	}

	@Override
	public boolean createNewFile() throws PermissionDeniedException, IOException {
		try {
			return super.createNewFile();
		} finally {
			refresh();
		}
	}

	@Override
	public void setAttributes(SftpFileAttributes attrs) throws IOException {
		try {
			super.setAttributes(attrs);
		} finally {
			refresh();
		}
	}

	@Override
	public OutputStream getOutputStream() throws IOException, PermissionDeniedException {
		return new RefreshOutputStream(super.getOutputStream());
	}

	@Override
	public boolean delete(boolean recursive) throws IOException, PermissionDeniedException {
		try {
			return super.delete(recursive);
		} finally {
			refresh();
		}
	}

	@Override
	public OutputStream getOutputStream(boolean append) throws IOException, PermissionDeniedException {
		return new RefreshOutputStream(super.getOutputStream(append));
	}
	
	class RefreshOutputStream extends OutputStream {
		
		OutputStream out;
		RefreshOutputStream(OutputStream out) {
			this.out = out;
		}
		
		@Override
		public void close() throws IOException {
			try {
				out.close();
			} finally {
				refresh();
			}
		}

		@Override
		public void write(byte[] buf, int off, int len) throws IOException {
			out.write(buf, off, len);
		}

		@Override
		public void write(int b) throws IOException {
			out.write(b);
		}
	}
}
