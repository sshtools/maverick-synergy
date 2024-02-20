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
		this.mount = mount.equals("/") ? mount : FileUtils.removeTrailingSlash(mount);
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
