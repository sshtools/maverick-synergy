package com.sshtools.common.files.nio;
import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class AbstractFileAttributeView implements BasicFileAttributeView {
	private static enum Attribute {
		attributes, certificates, contentEncoding, contentType, creationTime, fileKey, isDirectory, isOther, isRegularFile, isSymbolicLink, lastAccessTime, lastModifiedTime, size
	};

	@SuppressWarnings("unchecked")
	static <V extends FileAttributeView> V get(AbstractFilePath path, Class<V> type) {
		if (type == null)
			throw new NullPointerException();
		if (type == BasicFileAttributeView.class)
			try {
				return (V) new AbstractFileBasicAttributes(path.getAbstractFile());
			} catch (IOException e) {
			}
		return null;
	}
	
	static AbstractFileAttributeView get(AbstractFilePath path, String type) {
		if (type == null)
			throw new NullPointerException();
		if (type.equals("basic"))
			return new AbstractFileAttributeView(path);
		return null;
	}

	private final AbstractFilePath path;

	private AbstractFileAttributeView(AbstractFilePath path) {
		this.path = path;
	}

	@Override
	public String name() {
		return "basic";
	}

	@Override
	public AbstractFileBasicAttributes readAttributes() throws IOException {
		return path.getAttributes();
	}

	@Override
	public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
		// TODO
	}

	Object attribute(Attribute id, AbstractFileBasicAttributes attributes) {
		switch (id) {
		case size:
			return attributes.size();
		case creationTime:
			return attributes.creationTime();
		case lastAccessTime:
			return attributes.lastAccessTime();
		case lastModifiedTime:
			return attributes.lastModifiedTime();
		case isDirectory:
			return attributes.isDirectory();
		case isRegularFile:
			return attributes.isRegularFile();
		case isSymbolicLink:
			return attributes.isSymbolicLink();
		case isOther:
			return attributes.isOther();
		case fileKey:
			return attributes.fileKey();
		default:
		}
		return null;
	}

	Map<String, Object> readAttributes(String attributes) throws IOException {
		AbstractFileBasicAttributes zfas = readAttributes();
		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		if ("*".equals(attributes)) {
			for (Attribute id : Attribute.values()) {
				try {
					map.put(id.name(), attribute(id, zfas));
				} catch (IllegalArgumentException x) {
				}
			}
		} else {
			String[] as = attributes.split(",");
			for (String a : as) {
				try {
					map.put(a, attribute(Attribute.valueOf(a), zfas));
				} catch (IllegalArgumentException x) {
				}
			}
		}
		return map;
	}

	void setAttribute(String attribute, Object value) throws IOException {
		try {
			if (Attribute.valueOf(attribute) == Attribute.lastModifiedTime)
				setTimes((FileTime) value, null, null);
			return;
		} catch (IllegalArgumentException x) {
		}
		throw new UnsupportedOperationException("'" + attribute + "' is unknown or read-only attribute");
	}
}
