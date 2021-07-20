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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileAdapter;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.util.FileUtils;

public class VirtualMappedFile extends VirtualFileObject {

	private VirtualMount parentMount;
	private String absolutePath;
	private String name;
	
	public VirtualMappedFile(String path,
			VirtualMount parentMount, VirtualFileFactory fileFactory)
			throws IOException, PermissionDeniedException {

		super(fileFactory);
		
		this.parentMount = parentMount;
		this.fileFactory = fileFactory;

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

		super(fileFactory);
		
		this.parentMount = parentMount;
		this.fileFactory = fileFactory;
		init(actualFile);

		absolutePath = toVirtualPath(super.getAbsolutePath());
		// canonicalPath =

		int idx = absolutePath.lastIndexOf("/");
		name = absolutePath.substring(idx + 1);
	}

	@Override
	public List<AbstractFile> getChildren() throws IOException,
			PermissionDeniedException {

		List<AbstractFile> files = new ArrayList<AbstractFile>();

		// First check to see if this file is the file system root and if so
		// list out all the mounts.
		
		VirtualMountManager mgr = fileFactory.getMountManager();
		
		if (absolutePath.equals("/")) {
			files.addAll(getVirtualMounts().values());

			for (AbstractFile f : super.getChildren()) {
				VirtualFile f2 = new VirtualMappedFile(f, parentMount, fileFactory);
				if (!mgr.isMounted(f2.getAbsolutePath())) {
					files.add(f2);
				}
			}

		} else {
			for (AbstractFile f : super.getChildren()) {
				files.add(new VirtualMappedFile(f, parentMount,
						fileFactory));
			}
		}

		return files;
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
	}

	@Override
	public void moveTo(AbstractFile target) throws IOException,
			PermissionDeniedException {

		if (target instanceof VirtualMappedFile) {
			super.moveTo(((VirtualMappedFile) target).file);
		} else {
			super.moveTo(target);
		}
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
	
	
}
