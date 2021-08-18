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

import java.util.HashMap;
import java.util.Map;

import com.sshtools.common.util.FileUtils;

public class AbstractMount {

	private Map<String, Object> attributes = new HashMap<String, Object>();

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof AbstractMount)) {
			return false; 
		}

		return ((AbstractMount) obj).mount.equals(mount)
				&& ((AbstractMount) obj).path.equals(path);
	}

	@Override
	public int hashCode() {
		return mount.hashCode();
	}

	protected String mount;
	protected String path;
	private boolean filesystemRoot;
	private boolean isDefault;
	protected boolean isImaginary;

	protected AbstractMount(String mount, String path) {
		this(mount, path, false, false);
	}

	protected AbstractMount(String mount, String path, boolean isDefault,
			boolean isImaginary) {

		this.filesystemRoot = mount.equals("/");
		this.mount = mount.equals("/") ? mount : FileUtils
				.removeTrailingSlash(mount);
		this.path = FileUtils.removeTrailingSlash(path);
		this.isDefault = isDefault;
		this.isImaginary = isImaginary;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.maverick.sshd.sftp.FileSystemMountI#getMount()
	 */
	public String getMount() {
		return mount;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.maverick.sshd.sftp.FileSystemMountI#getRoot()
	 */
	public String getRoot() {
		return path;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.maverick.sshd.sftp.FileSystemMountI#isFilesystemRoot()
	 */
	public boolean isFilesystemRoot() {
		return filesystemRoot;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.maverick.sshd.sftp.FileSystemMountI#isDefault()
	 */
	public boolean isDefault() {
		return isDefault;
	}

	public boolean isImaginary() {
		return isImaginary;
	}

	public Object getAttribute(String key, Object defaultValue) {
		if (attributes.containsKey(key)) {
			return attributes.get(key);
		} else {
			return defaultValue;
		}
	}

	public void setAttribute(String key, Object value) {
		attributes.put(key, value);
	}
	
	public String toString() {
		return getMount() + " on " + getRoot();
	}
}
