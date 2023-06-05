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

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.sshtools.client.sftp.SftpClient;
import com.sshtools.common.sftp.PosixPermissions;
import com.sshtools.common.sftp.PosixPermissions.PosixPermissionsBuilder;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.UnsignedInteger32;
import com.sshtools.common.util.UnsignedInteger64;

public class SftpFileAttributeViews {
	
	private SftpFileAttributeViews() {
	}
	
	static Set<String> viewNames() {
		return Set.of("basic", "sftp", "posix");
	}

	@SuppressWarnings("unchecked")
	static <V extends FileAttributeView> V get(SftpPath path, Class<V> type) {
		if (type == null)
			throw new NullPointerException();
		else if (type == BasicFileAttributeView.class)
			return (V) new BasicSftpFileAttributesView(path);
		else if (type == ExtendedSftpFileAttributeView.class)
			return (V) new ExtendedSftpFileAttributeView(path);
		else if (type == PosixFileAttributeView.class)
			return (V) new PosixSftpFileAttributeView(path);
		return null;
	}

	@SuppressWarnings("unchecked")
	static <V extends BasicFileAttributes> V getAttributes(SftpPath path, Class<V> type) throws IOException {
		if (type == null)
			throw new NullPointerException();
		else if (type == BasicFileAttributes.class)
			return (V) new BasicSftpFileAttributesView(path).readAttributes();
		else if (type == ExtendedSftpFileAttributes.class)
			return (V) new ExtendedSftpFileAttributeView(path).readAttributes();
		else if (type == PosixFileAttributes.class)
			return (V) new PosixSftpFileAttributeView(path).readAttributes();
		else
			return null;
	}

	@SuppressWarnings("unchecked")
	static <V extends SftpFileAttributeView> V get(SftpPath path, String type) {
		if (type.equals("basic"))
			return (V) new BasicSftpFileAttributesView(path);
		else if (type.equals("sftp"))
			return (V) new ExtendedSftpFileAttributeView(path);
		else if (type.equals("posix"))
			return (V) new PosixSftpFileAttributeView(path);
		else
			return null;
	}

	public static class BasicSftpFileAttributes implements BasicFileAttributes {
		
		protected final SftpFileAttributes attrs;

		BasicSftpFileAttributes(SftpFileAttributes e) {
			this.attrs = e;
		}

		@Override
		public FileTime creationTime() {
			return attrs.createTime();
		}

		@Override
		public Object fileKey() {
			/* TODO */
			return null;
		}

		@Override
		public boolean isDirectory() {
			return attrs.isDirectory();
		}

		@Override
		public boolean isOther() {
			return attrs.isSpecial();
		}

		@Override
		public boolean isRegularFile() {
			return attrs.isFile();
		}

		@Override
		public boolean isSymbolicLink() {
			return attrs.isLink();
		}

		@Override
		public FileTime lastAccessTime() {
			return attrs.lastAccessTime();
		}

		@Override
		public FileTime lastModifiedTime() {
			return attrs.lastModifiedTime();
		}

		@Override
		public long size() {
			return attrs.size().longValue();
		}

	}

	public static class ExtendedSftpFileAttributes extends BasicSftpFileAttributes {
		

		ExtendedSftpFileAttributes(SftpFileAttributes e) {
			super(e);
		}

		public String mimeType() {
			return attrs.mimeType();
		}

		public int type() {
			return attrs.type();
		}

		public int linkCount() {
			return attrs.linkCount();
		}

		public String uid() {
			return attrs.getUID();
		}

		public String gid() {
			return attrs.getGID();
		}

		public String maskString() {
			return attrs.getMaskString();
		}

		public PosixPermissions permissions() {
			return attrs.permissions();
		}

	}

	public static class PosixSftpFileAttributes extends BasicSftpFileAttributes implements PosixFileAttributes {
		

		PosixSftpFileAttributes(SftpFileAttributes e) {
			super(e);
		}


		@Override
		public UserPrincipal owner() {
			return new UserPrincipal() {
				@Override
				public String getName() {
					return attrs.getUID();
				}
			};
		}


		@Override
		public GroupPrincipal group() {
			return new GroupPrincipal() {
				@Override
				public String getName() {
					return attrs.getGID();
				}
			};
		}


		@Override
		public Set<PosixFilePermission> permissions() {
			return attrs.getPosixPermissions().asPermissions();
		}

	}
	
	public interface SftpFileAttributeView extends BasicFileAttributeView {

		void setAttribute(String attribute, Object value) throws IOException;

		Map<String, Object> readAttributes(String attributes) throws IOException;
	}

	public static class BasicSftpFileAttributesView implements SftpFileAttributeView {

		static enum BasicAttribute {
			creationTime, fileKey, isDirectory, isOther, isRegularFile, isSymbolicLink, lastAccessTime,
			lastModifiedTime, size
		};

		protected final SftpPath path;

		private BasicSftpFileAttributesView(SftpPath path) {
			this.path = path;
		}

		@Override
		public String name() {
			return "basic";
		}

		@Override
		public BasicSftpFileAttributes readAttributes() throws IOException {
			var pathStr = toAbsolutePathString(path);
			try {
				return new BasicSftpFileAttributes(getSftp().stat(pathStr));
			} catch (SftpStatusException e) {
				try {
					throw SftpFileSystemProvider.translateException(e);
				} catch (NoSuchFileException nsfe) {
					try {
						return new BasicSftpFileAttributes(getSftp().statLink(pathStr));
					} catch (Exception e1) {
						throw SftpFileSystemProvider.translateException(e);
					}
				}
			} catch (SshException e) {
				throw SftpFileSystemProvider.translateException(e);
			}
		}

		@Override
		public final void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime)
				throws IOException {
			try {
				var sftpPath = toAbsolutePathString(path);
				var stat = getSftp().stat(sftpPath);

				var atime = lastAccessTime == null ? null : new UnsignedInteger64(lastAccessTime.to(TimeUnit.SECONDS));
				var mtime = lastModifiedTime == null ? null
						: new UnsignedInteger64(lastModifiedTime.to(TimeUnit.SECONDS));
				var ctime = createTime == null ? null : new UnsignedInteger64(createTime.to(TimeUnit.SECONDS));

				var atimeNano = atime == null ? null
						: new UnsignedInteger32(
								lastAccessTime.to(TimeUnit.NANOSECONDS) - TimeUnit.SECONDS.toNanos(atime.longValue()));
				var mtimeNano = mtime == null ? null
						: new UnsignedInteger32(lastModifiedTime.to(TimeUnit.NANOSECONDS)
								- TimeUnit.SECONDS.toNanos(mtime.longValue()));
				var ctimeNano = ctime == null ? null
						: new UnsignedInteger32(
								createTime.to(TimeUnit.NANOSECONDS) - TimeUnit.SECONDS.toNanos(ctime.longValue()));

				stat.setTimes(atime == null ? stat.getAccessedTime() : atime, atimeNano, mtime, mtimeNano, ctime,
						ctimeNano);

				getSftp().getSubsystemChannel().setAttributes(sftpPath, stat);

			} catch (Exception e) {
				throw SftpFileSystemProvider.translateException(e);
			} 

		}

		protected Object attribute(BasicAttribute id, BasicSftpFileAttributes attributes) {
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
			default: /* Done like this for coverage */
				return attributes.fileKey();
			}
		}

		protected final SftpFileSystem getFileSystem() {
			return path.getFileSystem();
		}

		protected final SftpClient getSftp() {
			return getFileSystem().getSftp();
		}

		@Override
		public Map<String, Object> readAttributes(String attributes) throws IOException {
			var zfas = readAttributes();
			var map = new LinkedHashMap<String, Object>();
			if ("*".equals(attributes)) {
				for (var id : BasicAttribute.values()) {
						map.put(id.name(), attribute(id, zfas));
				}
			} else {
				var as = attributes.split(",");
				for (var a : as) {
					try {
						map.put(a, attribute(BasicAttribute.valueOf(a), zfas));
					} catch (IllegalArgumentException x) {
					}
				}
			}
			return map;
		}

		@Override
		public void setAttribute(String attribute, Object value) throws IOException {
			try {
				var attr = BasicAttribute.valueOf(attribute);
				switch (attr) {
				case lastModifiedTime:
					setTimes((FileTime) value, null, null);
					break;
				case lastAccessTime:
					setTimes(null, (FileTime) value, null);
					break;
				case creationTime:
					setTimes(null, null, (FileTime) value);
					break;
				default:
					break;
				}
				return;
			} catch (IllegalArgumentException x) {
			}
			throw new UnsupportedOperationException("'" + attribute + "' is unknown or read-only attribute");
		}
	}
	
	public static final class PosixSftpFileAttributeView extends BasicSftpFileAttributesView implements PosixFileAttributeView {
		private static enum PosixAttribute {
			owner, group, permissions
		};

		private PosixSftpFileAttributeView(SftpPath path) {
			super(path);
		}

		@Override
		public String name() {
			return "posix";
		}

		@Override
		public PosixSftpFileAttributes readAttributes() throws IOException {
			var pathStr = toAbsolutePathString(path);
			try {
				return new PosixSftpFileAttributes(getSftp().stat(pathStr));
			} catch (SftpStatusException e) {
				try {
					throw SftpFileSystemProvider.translateException(e);
				} catch (NoSuchFileException nsfe) {
					try {
						return new PosixSftpFileAttributes(getSftp().statLink(pathStr));
					} catch (Exception e1) {
						throw SftpFileSystemProvider.translateException(e);
					}
				}
			} catch (SshException e) {
				throw SftpFileSystemProvider.translateException(e);
			}
		}

		protected Object attribute(PosixAttribute id, PosixSftpFileAttributes attributes) {
			switch (id) {
			case owner:
				return attributes.owner();
			case group:
				return attributes.group();
			case permissions:
			default: /* Done like this for coverage */
				return attributes.permissions();
			}
		}

		@Override
		public Map<String, Object> readAttributes(String attributes) throws IOException {
			var zfas = readAttributes();
			var map = super.readAttributes(attributes);
			if ("*".equals(attributes)) {
				for (var id : PosixAttribute.values()) {
					map.put(id.name(), attribute(id, zfas));
				}
			} else {
				String[] as = attributes.split(",");
				for (var a : as) {
					try {
						map.put(a, attribute(PosixAttribute.valueOf(a), zfas));
					} catch (IllegalArgumentException x) {
					}
				}
			}
			return map;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void setAttribute(String attribute, Object value) throws IOException {
			try {
				var attr = PosixAttribute.valueOf(attribute);
				switch (attr) {
				case group: {
					setGroup((GroupPrincipal)value);
					return;
				}
				case owner: {
					setOwner((UserPrincipal)value);
					return;
				}
				default:
				case permissions: {
					setPermissions((Set<PosixFilePermission>)value);
					return;
				}
				}
			} catch (IllegalArgumentException x) {
			}
			super.setAttribute(attribute, value);
		}

		@Override
		public UserPrincipal getOwner() throws IOException {
			return readAttributes().owner();
		}

		@Override
		public void setOwner(UserPrincipal owner) throws IOException {
			try {
				var sftpPath = toAbsolutePathString(path);
				var stat = getSftp().stat(sftpPath);
				stat.setUsername(owner.getName());
				getSftp().getSubsystemChannel().setAttributes(sftpPath, stat);
			} catch (Exception e) {
				throw SftpFileSystemProvider.translateException(e);
			}
		}

		@Override
		public void setPermissions(Set<PosixFilePermission> perms) throws IOException {
			try {
				var sftpPath = toAbsolutePathString(path);
				var stat = getSftp().stat(sftpPath);
				stat.setPermissions(PosixPermissionsBuilder.create().fromPermissions(perms.toArray(new PosixFilePermission[0])).build());
				getSftp().getSubsystemChannel().setAttributes(sftpPath, stat);
			} catch (Exception e) {
				throw SftpFileSystemProvider.translateException(e);
			}
			
		}

		@Override
		public void setGroup(GroupPrincipal group) throws IOException {
			try {
				var sftpPath = toAbsolutePathString(path);
				var stat = getSftp().stat(sftpPath);
				stat.setGroup(group.getName());
				getSftp().getSubsystemChannel().setAttributes(sftpPath, stat);
			} catch(Exception e) {
				throw SftpFileSystemProvider.translateException(e);
			}
			
		}
	}

	public static final class ExtendedSftpFileAttributeView extends BasicSftpFileAttributesView {
		private static enum ExtendedAttribute {
			uid, gid, maskString, permissions, mimeType, type, linkCount
		};

		private ExtendedSftpFileAttributeView(SftpPath path) {
			super(path);
		}

		@Override
		public String name() {
			return "sftp";
		}

		@Override
		public ExtendedSftpFileAttributes readAttributes() throws IOException {
			var pathStr = toAbsolutePathString(path);
			try {
				return new ExtendedSftpFileAttributes(getSftp().stat(pathStr));
			} catch (SftpStatusException e) {
				try {
					throw SftpFileSystemProvider.translateException(e);
				} catch (NoSuchFileException nsfe) {
					try {
						return new ExtendedSftpFileAttributes(getSftp().statLink(pathStr));
					} catch (Exception e1) {
						throw SftpFileSystemProvider.translateException(e);
					}
				} 
			} catch (SshException e) {
				throw SftpFileSystemProvider.translateException(e);
			}
		}

		protected Object attribute(ExtendedAttribute id, ExtendedSftpFileAttributes attributes) {
			switch (id) {
			case uid:
				return attributes.uid();
			case gid:
				return attributes.gid();
			case maskString:
				return attributes.maskString();
			case permissions:
				return attributes.permissions();
			case type:
				return attributes.type();
			case linkCount:
				return attributes.linkCount();
			case mimeType:
			default: /* For coverage */
				return attributes.mimeType();
			}
		}

		@Override
		public Map<String, Object> readAttributes(String attributes) throws IOException {
			var zfas = readAttributes();
			var map = super.readAttributes(attributes);
			if ("*".equals(attributes)) {
				for (var id : ExtendedAttribute.values()) {
					map.put(id.name(), attribute(id, zfas));
				}
			} else {
				String[] as = attributes.split(",");
				for (var a : as) {
					try {
						map.put(a, attribute(ExtendedAttribute.valueOf(a), zfas));
					} catch (IllegalArgumentException x) {
					}
				}
			}
			return map;
		}

		@Override
		public void setAttribute(String attribute, Object value) throws IOException {
			try {
				var attr = ExtendedAttribute.valueOf(attribute);
				switch (attr) {
				case gid: {
					var sftpPath = getFileSystem().toAbsolutePathString(path);
					var stat = getSftp().stat(sftpPath);
					stat.setUsername(String.valueOf(value));
					getSftp().getSubsystemChannel().setAttributes(sftpPath, stat);
					return;
				}
				case uid: {
					var sftpPath = getFileSystem().toAbsolutePathString(path);
					var stat = getSftp().stat(sftpPath);
					stat.setUsername(String.valueOf(value));
					getSftp().getSubsystemChannel().setAttributes(sftpPath, stat);
					return;
				}
				case permissions:
				default: /* For coverage */			
					var sftpPath = getFileSystem().toAbsolutePathString(path);
					var stat = getSftp().stat(sftpPath);
					stat.setPermissions((PosixPermissions)value);
					getSftp().getSubsystemChannel().setAttributes(sftpPath, stat);
					break;
				}
				return;
			} catch (SftpStatusException | SshException e) {
				throw SftpFileSystemProvider.translateException(e);
			} catch (IllegalArgumentException x) {
			}
			super.setAttribute(attribute, value);
		}
	}
}
