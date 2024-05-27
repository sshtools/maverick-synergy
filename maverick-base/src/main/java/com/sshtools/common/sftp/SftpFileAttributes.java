package com.sshtools.common.sftp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.sshtools.common.logger.Log;
import com.sshtools.common.sftp.PosixPermissions.PosixPermissionsBuilder;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.common.util.UnsignedInteger32;
import com.sshtools.common.util.UnsignedInteger64;

/**
 * This class represents the ATTRS structure defined in the
 * draft-ietf-secsh-filexfer-02.txt which is used by the protocol to store file
 * attribute information.
 * 
 * @author Lee David Painter
 * @param <SSH_FILEXFER_TYPE_FIFO>
 */
public class SftpFileAttributes {

	public final static class SftpFileAttributesBuilder {

		public static SftpFileAttributesBuilder create() {
			return new SftpFileAttributesBuilder();
		}

		public static SftpFileAttributesBuilder ofType(int type, String charsetEncoding) {
			return new SftpFileAttributesBuilder().withType(type).withCharsetEncoding(charsetEncoding);
		}

		public static SftpFileAttributesBuilder of(ByteArrayReader bar, int version, String charsetEncoding)
				throws IOException {
			return new SftpFileAttributesBuilder().asVersion(version).withCharsetEncoding(charsetEncoding)
					.fromPacket(bar);
		}

		private Optional<FileTime> lastAccessTime = Optional.empty();
		private Optional<FileTime> createTime = Optional.empty();
		private Optional<FileTime> lastModifiedTime = Optional.empty();
		private Optional<FileTime> lastAttributesModifiedTime = Optional.empty();
		private Optional<UnsignedInteger64> size = Optional.empty();
		private Optional<UnsignedInteger64> allocationSize = Optional.empty();
		private int type = 0;
		private long flags = 0;
		private Optional<String> charsetEncoding = Optional.empty();
		private int version = 4;
		private Optional<Long> supportedAttributeMask = Optional.empty();
		private Optional<Long> supportedAttributeBits = Optional.empty();
		private Optional<PosixPermissions> permissions = Optional.empty();
		private Optional<Integer> uid = Optional.empty();
		private Optional<String> username = Optional.empty();
		private Optional<Integer> gid = Optional.empty();
		private Optional<String> group = Optional.empty();
		private Optional<UnsignedInteger32> aclFlags = Optional.empty();
		private final List<ACL> acls = new ArrayList<>();
		private final Map<String, byte[]> extendedAttributes = new HashMap<String, byte[]>();
		private Optional<UnsignedInteger32> attributeBits = Optional.empty();
		private Optional<String> mimeType = Optional.empty();
		private Optional<Byte> textHint = Optional.empty();
		private Optional<UnsignedInteger32> attributeBitsValid = Optional.empty();
		private Optional<Integer> linkCount = Optional.empty();
		private Optional<String> untranslatedName = Optional.empty();

		private SftpFileAttributesBuilder() {
		}

		public SftpFileAttributesBuilder addAcls(ACL... acls) {
			return addAcls(Arrays.asList(acls));
		}

		public SftpFileAttributesBuilder addAcls(Collection<ACL> acls) {
			this.acls.addAll(acls);
			flags |= SSH_FILEXFER_ATTR_UIDGID;
			return this;
		}

		public SftpFileAttributesBuilder addExtendedAttribute(String key, byte[] value) {
			extendedAttributes.put(key, value);
			flags |= SSH_FILEXFER_ATTR_EXTENDED;
			return this;
		}

		public SftpFileAttributesBuilder addExtendedAttributes(Map<String, byte[]> extendedAttributes) {
			this.extendedAttributes.putAll(extendedAttributes);
			flags |= SSH_FILEXFER_ATTR_EXTENDED;
			return this;
		}

		public SftpFileAttributesBuilder asVersion(int version) {
			this.version = version;
			return this;
		}

		public SftpFileAttributes build() {
			return new SftpFileAttributes(this);
		}

		public SftpFileAttributesBuilder removeExtendedAttribute(String key) {
			extendedAttributes.remove(key);
			flags |= SSH_FILEXFER_ATTR_EXTENDED;
			return this;
		}

		public SftpFileAttributesBuilder withAclFlags(long aclFlags) {
			return withAclFlags(new UnsignedInteger32(aclFlags));
		}

		public SftpFileAttributesBuilder withAclFlags(Optional<UnsignedInteger32> aclFlags) {
			this.aclFlags = aclFlags;
			flags |= SSH_FILEXFER_ATTR_UIDGID;
			return this;
		}

		public SftpFileAttributesBuilder withAclFlags(UnsignedInteger32 aclFlags) {
			return withAclFlags(Optional.of(aclFlags));
		}

		public SftpFileAttributesBuilder withAcls(ACL... acls) {
			return withAcls(Arrays.asList(acls));
		}

		public SftpFileAttributesBuilder withAcls(Collection<ACL> acls) {
			this.acls.clear();
			return addAcls(acls);
		}

		public SftpFileAttributesBuilder withAppendOnly(boolean value) {
			setAttributeBit(SSH_FILEXFER_ATTR_FLAGS_APPEND_ONLY, value);
			return this;
		}

		public SftpFileAttributesBuilder withArchive(boolean value) {
			setAttributeBit(SSH_FILEXFER_ATTR_FLAGS_ARCHIVE, value);
			return this;
		}

		public SftpFileAttributesBuilder withAttributeBits(int attributeBits) {
			return withAttributeBits(new UnsignedInteger32(attributeBits));
		}

		public SftpFileAttributesBuilder withAttributeBits(Optional<UnsignedInteger32> attributeBits) {
			this.attributeBits = attributeBits;
			return this;
		}

		public SftpFileAttributesBuilder withAttributeBits(UnsignedInteger32 attributeBits) {
			return withAttributeBits(Optional.of(attributeBits));
		}

		public SftpFileAttributesBuilder withCaseSensitive(boolean value) {
			setAttributeBit(SSH_FILEXFER_ATTR_FLAGS_CASE_INSENSITIVE, value);
			return this;
		}

		public SftpFileAttributesBuilder withCharsetEncoding(Charset charsetEncoding) {
			return withCharsetEncoding(charsetEncoding.name());
		}

		public SftpFileAttributesBuilder withCharsetEncoding(Optional<String> charsetEncoding) {
			this.charsetEncoding = charsetEncoding;
			return this;
		}

		public SftpFileAttributesBuilder withCharsetEncoding(String charsetEncoding) {
			return withCharsetEncoding(Optional.ofNullable(charsetEncoding));
		}

		public SftpFileAttributesBuilder withCompressed(boolean value) {
			setAttributeBit(SSH_FILEXFER_ATTR_FLAGS_COMPRESSED, value);
			return this;
		}

		public SftpFileAttributesBuilder withCreateTime(FileTime createTime) {
			return withCreateTime(Optional.ofNullable(createTime));
		}

		public SftpFileAttributesBuilder withCreateTime(long createTimeMs) {
			return withCreateTime(FileTime.fromMillis(createTimeMs));
		}

		public SftpFileAttributesBuilder withCreateTime(Optional<FileTime> createTime) {
			this.createTime = createTime;
			if (this.createTime.isPresent())
				flags |= SSH_FILEXFER_ATTR_CREATETIME;
			else
				flags &= ~SSH_FILEXFER_ATTR_CREATETIME;
			return this;
		}

		public SftpFileAttributesBuilder withEncrypted(boolean value) {
			setAttributeBit(SSH_FILEXFER_ATTR_FLAGS_ENCRYPTED, value);
			return this;
		}

		public SftpFileAttributesBuilder withExtendedAttributes(Map<String, byte[]> extendedAttributes) {
			this.extendedAttributes.clear();
			return addExtendedAttributes(extendedAttributes);
		}

		public SftpFileAttributesBuilder withFileAttributes(SftpFileAttributes attributes) {
			this.size = attributes.size;
			this.type = attributes.type;
			this.charsetEncoding = Optional.of(attributes.charsetEncoding);
			this.supportedAttributeBits = attributes.supportedAttributeBits;
			this.supportedAttributeMask = attributes.supportedAttributeMask;
			this.flags = attributes.flags;
			this.allocationSize = attributes.allocationSize;
			this.uid = attributes.uid;
			this.gid = attributes.gid;
			this.username = attributes.username;
			this.group = attributes.group;
			this.permissions = attributes.permissions;
			this.lastAccessTime = attributes.lastAccessTime;
			this.createTime = attributes.createTime;
			this.lastModifiedTime = attributes.lastModifiedTime;
			this.lastAttributesModifiedTime = attributes.lastAttributesModifiedTime;
			this.acls.clear();
			this.acls.addAll(attributes.acls);
			this.aclFlags = attributes.aclFlags;
			this.extendedAttributes.clear();
			this.extendedAttributes.putAll(attributes.extendedAttributes);
			this.attributeBits = attributes.attributeBits;
			this.mimeType = attributes.mimeType;
			this.textHint = attributes.textHint;
			this.attributeBitsValid = attributes.attributeBitsValid;
			this.linkCount = attributes.linkCount;
			this.untranslatedName = attributes.untranslatedName;
			return this;
		}

		public SftpFileAttributesBuilder withFlags(long flags) {
			this.flags = flags;
			return this;
		}

		public SftpFileAttributesBuilder withGid(int gid) {
			return withGid(Optional.of(gid));
		}

		public SftpFileAttributesBuilder withGid(Optional<Integer> gid) {
			this.gid = gid;
			if (this.gid.isPresent() || this.uid.isPresent())
				flags |= SSH_FILEXFER_ATTR_UIDGID;
			else
				flags &= ~SSH_FILEXFER_ATTR_UIDGID;
			return this;
		}
		
		public SftpFileAttributesBuilder withUidOrUsername(Optional<String> attribute) {
			if(attribute.isEmpty()) {
				username = Optional.empty();
				uid = Optional.empty();
			}
			else {
				try {
					uid = Optional.of(Integer.parseInt(attribute.get()));
					username = Optional.empty();
				} catch(NumberFormatException iae) {
					uid = Optional.empty();
					username = Optional.ofNullable(attribute.get());
				}
			}
			return this;
		}

		public SftpFileAttributesBuilder withUidOrUsername(String attribute) {
			return withUidOrUsername(Optional.ofNullable(attribute));
		}
		
		public SftpFileAttributesBuilder withGidOrGroup(Optional<String> attribute) {
			if(attribute.isEmpty()) {
				group = Optional.empty();
				gid = Optional.empty();
			}
			else {
				try {
					gid = Optional.of(Integer.parseInt(attribute.get()));
					group = Optional.empty();
				} catch(NumberFormatException iae) {
					gid = Optional.empty();
					group = Optional.ofNullable(attribute.get());
				}
			}
			return this;
		}

		public SftpFileAttributesBuilder withGidOrGroup(String attribute) {
			return withGidOrGroup(Optional.ofNullable(attribute));
		}

		public SftpFileAttributesBuilder withGroup(Optional<String> group) {
			this.group = group;
			if (this.username.isPresent() || this.group.isPresent())
				flags |= SSH_FILEXFER_ATTR_OWNERGROUP;
			else
				flags &= ~SSH_FILEXFER_ATTR_OWNERGROUP;
			return this;
		}

		public SftpFileAttributesBuilder withGroup(String group) {
			return withGroup(Optional.of(group));
		}

		public SftpFileAttributesBuilder withHidden(boolean value) {
			setAttributeBit(SSH_FILEXFER_ATTR_FLAGS_HIDDEN, value);
			return this;
		}

		public SftpFileAttributesBuilder withImmutable(boolean value) {
			setAttributeBit(SSH_FILEXFER_ATTR_FLAGS_IMMUTABLE, value);
			return this;
		}

		public SftpFileAttributesBuilder withLastAccessTime(FileTime atime) {
			return withLastAccessTime(Optional.ofNullable(atime));
		}

		public SftpFileAttributesBuilder withLastAccessTime(long atimeMs) {
			return withLastAccessTime(FileTime.fromMillis(atimeMs));
		}

		public SftpFileAttributesBuilder withLastAccessTime(Optional<FileTime> atime) {
			this.lastAccessTime = atime;
			if (this.lastAccessTime.isPresent())
				flags |= SSH_FILEXFER_ATTR_ACCESSTIME;
			else
				flags &= ~SSH_FILEXFER_ATTR_ACCESSTIME;
			return this;
		}

		public SftpFileAttributesBuilder withLastModifiedTime(FileTime lastModifiedTime) {
			return withLastModifiedTime(Optional.ofNullable(lastModifiedTime));
		}

		public SftpFileAttributesBuilder withLastModifiedTime(long lastModifiedTimeMs) {
			return withLastModifiedTime(FileTime.fromMillis(lastModifiedTimeMs));
		}

		public SftpFileAttributesBuilder withLastModifiedTime(Optional<FileTime> lastModifiedTime) {
			this.lastModifiedTime = lastModifiedTime;
			if (this.lastModifiedTime.isPresent())
				flags |= SSH_FILEXFER_ATTR_MODIFYTIME;
			else
				flags &= ~SSH_FILEXFER_ATTR_MODIFYTIME;
			return this;
		}

		public SftpFileAttributesBuilder withPermissions(Collection<PosixFilePermission> permissions) {
			return withPermissions(PosixPermissionsBuilder.create().withPermissions(permissions).build());
		}

		public SftpFileAttributesBuilder withPermissions(Optional<PosixPermissions> permissions) {
			this.permissions = permissions;
			if (this.permissions.isPresent())
				flags |= SSH_FILEXFER_ATTR_PERMISSIONS;
			else
				flags &= ~SSH_FILEXFER_ATTR_PERMISSIONS;
			return this;
		}

		public SftpFileAttributesBuilder withPermissions(PosixFilePermission... permissions) {
			return withPermissions(Arrays.asList(permissions));
		}

		public SftpFileAttributesBuilder withPermissions(PosixPermissions permissions) {
			return withPermissions(Optional.of(permissions));
		}

		public SftpFileAttributesBuilder withReadOnly(boolean value) {
			setAttributeBit(SSH_FILEXFER_ATTR_FLAGS_READONLY, value);
			return this;
		}

		public SftpFileAttributesBuilder withSize(long size) {
			return withSize(new UnsignedInteger64(size));
		}

		public SftpFileAttributesBuilder withSize(Optional<UnsignedInteger64> size) {
			this.size = size;
			if (this.size.isPresent())
				flags |= SSH_FILEXFER_ATTR_SIZE;
			else
				flags &= ~SSH_FILEXFER_ATTR_SIZE;
			return this;
		}

		public SftpFileAttributesBuilder withSize(UnsignedInteger64 size) {
			return withSize(Optional.of(size));
		}

		public SftpFileAttributesBuilder withSparse(boolean value) {
			setAttributeBit(SSH_FILEXFER_ATTR_FLAGS_SPARSE, value);
			return this;
		}

		public SftpFileAttributesBuilder withSubSecondsTimes(boolean subSecondTimes) {
			if (subSecondTimes)
				flags |= SSH_FILEXFER_ATTR_SUBSECOND_TIMES;
			else
				flags &= ~SSH_FILEXFER_ATTR_SUBSECOND_TIMES;
			return this;
		}

		public SftpFileAttributesBuilder withSupportedAttributeBits(long supportedAttributeBits) {
			return withSupportedAttributeBits(Optional.of(supportedAttributeBits));
		}

		public SftpFileAttributesBuilder withSupportedAttributeBits(Optional<Long> supportedAttributeBits) {
			this.supportedAttributeBits = supportedAttributeBits;
			return this;
		}

		public SftpFileAttributesBuilder withSupportedAttributeMask(long supportedAttributeMask) {
			return withSupportedAttributeMask(Optional.of(supportedAttributeMask));
		}

		public SftpFileAttributesBuilder withSupportedAttributeMask(Optional<Long> supportedAttributeMask) {
			this.supportedAttributeMask = supportedAttributeMask;
			return this;
		}

		public SftpFileAttributesBuilder withSync(boolean value) {
			setAttributeBit(SSH_FILEXFER_ATTR_FLAGS_SYNC, value);
			return this;
		}

		public SftpFileAttributesBuilder withSystem(boolean value) {
			setAttributeBit(SSH_FILEXFER_ATTR_FLAGS_SYSTEM, value);
			return this;
		}

		public SftpFileAttributesBuilder withType(int type) {
			this.type = type;
			return this;
		}

		public SftpFileAttributesBuilder withUid(int uid) {
			return withUid(Optional.of(uid));
		}

		public SftpFileAttributesBuilder withUid(Optional<Integer> uid) {
			this.uid = uid;
			if (this.uid.isPresent() || this.gid.isPresent())
				flags |= SSH_FILEXFER_ATTR_UIDGID;
			else
				flags &= ~SSH_FILEXFER_ATTR_UIDGID;
			return this;
		}

		public SftpFileAttributesBuilder withUsername(Optional<String> username) {
			this.username = username;
			if (this.username.isPresent() || this.group.isPresent())
				flags |= SSH_FILEXFER_ATTR_OWNERGROUP;
			else
				flags &= ~SSH_FILEXFER_ATTR_OWNERGROUP;
			return this;
		}

		public SftpFileAttributesBuilder withUsername(String username) {
			return withUsername(Optional.of(username));
		}

		String calcCharset() {
			return charsetEncoding.map(e -> {
				try {
					"1234567890".getBytes(e);
					return e;
				} catch (UnsupportedEncodingException ex) {
					if (Log.isDebugEnabled())
						Log.debug(e + " is not a supported character set encoding. Defaulting to ISO-8859-1");
					return "ISO-8859-1";
				}
			}).orElse("ISO-8859-1");
		}

		SftpFileAttributesBuilder fromPacket(ByteArrayReader bar) throws IOException {

			var charsetEncoding = calcCharset();

			if (bar.available() >= 4)
				flags = bar.readInt();
			else
				flags = 0;

			// Work out the type from the permissions field later if we're not using
			// version
			// 4 of the protocol
			type = 0;
			if (version > 3) {
				// Get the type if were using version 4+ of the protocol
				if (bar.available() > 0)
					type = bar.read();
			}

			// if ATTR_SIZE flag is set then read size
			if (isFlagSet(SSH_FILEXFER_ATTR_SIZE, version) && bar.available() >= 8) {
				byte[] raw = new byte[8];
				bar.read(raw);
				size = Optional.of(new UnsignedInteger64(raw));
			} else {
				size = Optional.empty();
			}

			if (isFlagSet(SSH_FILEXFER_ATTR_ALLOCATION_SIZE, version) && bar.available() >= 8) {
				byte[] raw = new byte[8];
				bar.read(raw);
				allocationSize = Optional.of(new UnsignedInteger64(raw));
			} else {
				allocationSize = Optional.empty();
			}

			if (version <= 3 && isFlagSet(SSH_FILEXFER_ATTR_UIDGID, version) && bar.available() >= 8) {

				uid = Optional.of((int) bar.readInt());
				gid = Optional.of((int) bar.readInt());
			} else if (version > 3 && isFlagSet(SSH_FILEXFER_ATTR_OWNERGROUP, version) && bar.available() > 0) {
				username = Optional.of(bar.readString(charsetEncoding));
				group = Optional.of(bar.readString(charsetEncoding));
				uid = Optional.ofNullable(username.map(u -> {
					try {
						return Integer.parseInt(u);
					} catch (Exception e) {
						return null;
					}
				}).orElse(null));
				gid = Optional.ofNullable(group.map(g -> {
					try {
						return Integer.parseInt(g);
					} catch (Exception e) {
						return null;
					}
				}).orElse(null));
			} else {
				username = Optional.empty();
				uid = Optional.empty();
				group = Optional.empty();
				gid = Optional.empty();
			}

			if (isFlagSet(SSH_FILEXFER_ATTR_PERMISSIONS, version) && bar.available() >= 4) {
				var fullPermissionsMask = bar.readInt();
				permissions = Optional
						.of(PosixPermissionsBuilder.create().withBitmaskFlags(fullPermissionsMask).build());
				if (version <= 3) {
					int ifmt = (int) fullPermissionsMask & S_IFMT;
					if (ifmt > 0) {
						if (ifmt == S_IFREG) {
							type = SSH_FILEXFER_TYPE_REGULAR;
						} else if (ifmt == S_IFLNK) {
							type = SSH_FILEXFER_TYPE_SYMLINK;
						} else if (ifmt == S_IFCHR) {
							type = SSH_FILEXFER_TYPE_CHAR_DEVICE;
						} else if (ifmt == S_IFBLK) {
							type = SSH_FILEXFER_TYPE_BLOCK_DEVICE;
						} else if (ifmt == S_IFDIR) {
							type = SSH_FILEXFER_TYPE_DIRECTORY;
						} else if (ifmt == S_IFIFO) {
							type = SSH_FILEXFER_TYPE_FIFO;
						} else if (ifmt == S_IFSOCK) {
							type = SSH_FILEXFER_TYPE_SOCKET;
						} else if (ifmt == S_IFMT) {
							type = SSH_FILEXFER_TYPE_SPECIAL;
						} else {
							type = SSH_FILEXFER_TYPE_UNKNOWN;
						}
					}
				}
			} else
				permissions = Optional.empty();

			if (type == 0) {
				type = SSH_FILEXFER_TYPE_UNKNOWN;
			}

			lastModifiedTime = Optional.empty();
			createTime = Optional.empty();
			lastAccessTime = Optional.empty();
			lastAttributesModifiedTime = Optional.empty();

			if (version <= 3 && isFlagSet(SSH_FILEXFER_ATTR_ACCESSTIME, version) && bar.available() >= 8) {
				lastAccessTime = Optional.of(FileTime.from(bar.readInt(), TimeUnit.SECONDS));
				lastModifiedTime = Optional.of(FileTime.from(bar.readInt(), TimeUnit.SECONDS));
			} else if (version > 3 && bar.available() > 0) {
				if (isFlagSet(SSH_FILEXFER_ATTR_ACCESSTIME, version) && bar.available() >= 8) {
					var atimeSeconds = bar.readUINT64().longValue();
					if (isFlagSet(SSH_FILEXFER_ATTR_SUBSECOND_TIMES, version) && bar.available() >= 4) {
						lastAccessTime = Optional.of(FileTime
								.from(Instant.ofEpochSecond(atimeSeconds).plusNanos(bar.readUINT32().longValue())));
					} else {
						lastAccessTime = Optional.of(FileTime.from(atimeSeconds, TimeUnit.SECONDS));
					}
				} else
					lastAccessTime = Optional.empty();
			}

			if (version > 3 && bar.available() > 0) {
				if (isFlagSet(SSH_FILEXFER_ATTR_CREATETIME, version) && bar.available() >= 8) {
					var ctimeSeconds = bar.readUINT64().longValue();
					if (isFlagSet(SSH_FILEXFER_ATTR_SUBSECOND_TIMES, version) && bar.available() >= 4) {
						createTime = Optional.of(FileTime
								.from(Instant.ofEpochSecond(ctimeSeconds).plusNanos(bar.readUINT32().longValue())));
					} else {
						createTime = Optional.of(FileTime.from(ctimeSeconds, TimeUnit.SECONDS));
					}
				}
			}

			if (version > 3 && bar.available() > 0) {
				if (isFlagSet(SSH_FILEXFER_ATTR_MODIFYTIME, version) && bar.available() >= 8) {
					var mtimeSeconds = bar.readUINT64().longValue();
					if (isFlagSet(SSH_FILEXFER_ATTR_SUBSECOND_TIMES, version) && bar.available() >= 4) {
						lastModifiedTime = Optional.of(FileTime
								.from(Instant.ofEpochSecond(mtimeSeconds).plusNanos(bar.readUINT32().longValue())));
					} else {
						lastModifiedTime = Optional.of(FileTime.from(mtimeSeconds, TimeUnit.SECONDS));
					}
				}
			}

			if (version >= 6 && bar.available() > 0) {
				if (isFlagSet(SSH_FILEXFER_ATTR_CTIME, version) && bar.available() >= 8) {
					var ctimeSeconds = bar.readUINT64().longValue();
					if (isFlagSet(SSH_FILEXFER_ATTR_SUBSECOND_TIMES, version) && bar.available() >= 4) {
						lastAttributesModifiedTime = Optional.of(FileTime
								.from(Instant.ofEpochSecond(ctimeSeconds).plusNanos(bar.readUINT32().longValue())));
					} else {
						lastAttributesModifiedTime = Optional.of(FileTime.from(ctimeSeconds, TimeUnit.SECONDS));
					}
				}
			}

			aclFlags = Optional.empty();
			acls.clear();
			if (version > 3 && isFlagSet(SSH_FILEXFER_ATTR_ACL, version) && bar.available() >= 4) {

				if (version >= 6 && bar.available() >= 4) {
					aclFlags = Optional.of(bar.readUINT32());
				}

				int length = (int) bar.readInt();

				if (length > 0 && bar.available() >= length) {
					int count = (int) bar.readInt();
					for (int i = 0; i < count; i++) {
						acls.add(new ACL((int) bar.readInt(), (int) bar.readInt(), (int) bar.readInt(),
								bar.readString()));
					}
				}
			}

			if (version >= 5 && isFlagSet(SSH_FILEXFER_ATTR_BITS, version) && bar.available() >= 4) {
				attributeBits = Optional.of(bar.readUINT32());
			} else {
				attributeBits = Optional.empty();
			}

			attributeBitsValid = Optional.empty();
			textHint = Optional.empty();
			mimeType = Optional.empty();
			linkCount = Optional.empty();
			untranslatedName = Optional.empty();
			if (version >= 6) {

				if (isFlagSet(SSH_FILEXFER_ATTR_BITS, version) && bar.available() >= 4) {
					attributeBitsValid = Optional.of(bar.readUINT32());
				}

				if (isFlagSet(SSH_FILEXFER_ATTR_TEXT_HINT, version) && bar.available() >= 1) {
					textHint = Optional.of((byte) bar.read());
				}
				if (isFlagSet(SSH_FILEXFER_ATTR_MIME_TYPE, version) && bar.available() >= 4) {
					mimeType = Optional.of(bar.readString());
				}

				if (isFlagSet(SSH_FILEXFER_ATTR_LINK_COUNT, version) && bar.available() >= 4) {
					linkCount = Optional.of(bar.readUINT32().intValue());
				}

				if (isFlagSet(SSH_FILEXFER_ATTR_UNTRANSLATED, version) && bar.available() >= 4) {
					untranslatedName = Optional.of(bar.readString());
				}
			}

			extendedAttributes.clear();
			if (version >= 3 && isFlagSet(SSH_FILEXFER_ATTR_EXTENDED, version) && bar.available() >= 4) {
				var count = (int) bar.readInt();
				for (int i = 0; i < count; i++) {
					extendedAttributes.put(bar.readString(), bar.readBinaryString());
				}
			}

			return this;
		}

		boolean isFlagSet(long flag, int version) {
			return SftpFileAttributes.isFlagSet(flag, flags, version, supportedAttributeMask);
		}

		void setAttributeBit(long attributeBit, boolean value) {
			flags = flags | SSH_FILEXFER_ATTR_BITS;
			if (value) {
				attributeBits = Optional.of(new UnsignedInteger32(
						attributeBits.map(UnsignedInteger32::longValue).orElse(0l) | attributeBit));
			} else {
				attributeBits = Optional.of(new UnsignedInteger32(
						attributeBits.map(UnsignedInteger32::longValue).orElse(0l) & ~attributeBit));
			}
		}
	}

	public static final long SSH_FILEXFER_ATTR_SIZE = 0x00000001;
	public static final long SSH_FILEXFER_ATTR_UIDGID = 0x00000002;
	public static final long SSH_FILEXFER_ATTR_PERMISSIONS = 0x00000004;

	public static final long SSH_FILEXFER_ATTR_ACCESSTIME = 0x00000008;

	public static final long SSH_FILEXFER_ATTR_EXTENDED = 0x80000000;

	public static final long VERSION_3_FLAGS = SSH_FILEXFER_ATTR_SIZE | SSH_FILEXFER_ATTR_UIDGID
			| SSH_FILEXFER_ATTR_PERMISSIONS | SSH_FILEXFER_ATTR_ACCESSTIME | SSH_FILEXFER_ATTR_EXTENDED;
	// Version 4 flags
	public static final long SSH_FILEXFER_ATTR_CREATETIME = 0x00000010;
	public static final long SSH_FILEXFER_ATTR_MODIFYTIME = 0x00000020;
	public static final long SSH_FILEXFER_ATTR_ACL = 0x00000040;
	public static final long SSH_FILEXFER_ATTR_OWNERGROUP = 0x00000080;

	public static final long SSH_FILEXFER_ATTR_SUBSECOND_TIMES = 0x00000100;

	public static final long VERSION_4_FLAGS = (VERSION_3_FLAGS ^ SSH_FILEXFER_ATTR_UIDGID)
			| SSH_FILEXFER_ATTR_CREATETIME | SSH_FILEXFER_ATTR_MODIFYTIME | SSH_FILEXFER_ATTR_ACL
			| SSH_FILEXFER_ATTR_OWNERGROUP | SSH_FILEXFER_ATTR_SUBSECOND_TIMES;

	// This is only used for version >= 5
	public static final long SSH_FILEXFER_ATTR_BITS = 0x00000200;

	public static final long VERSION_5_FLAGS = VERSION_4_FLAGS | SSH_FILEXFER_ATTR_BITS;
	// These are version >= 6
	public static final long SSH_FILEXFER_ATTR_ALLOCATION_SIZE = 0x00000400;
	public static final long SSH_FILEXFER_ATTR_TEXT_HINT = 0x00000800;
	public static final long SSH_FILEXFER_ATTR_MIME_TYPE = 0x00001000;
	public static final long SSH_FILEXFER_ATTR_LINK_COUNT = 0x00002000;
	public static final long SSH_FILEXFER_ATTR_UNTRANSLATED = 0x00004000;

	public static final long SSH_FILEXFER_ATTR_CTIME = 0x00008000;

	public static final long VERSION_6_FLAGS = VERSION_5_FLAGS | SSH_FILEXFER_ATTR_ALLOCATION_SIZE
			| SSH_FILEXFER_ATTR_TEXT_HINT | SSH_FILEXFER_ATTR_MIME_TYPE | SSH_FILEXFER_ATTR_LINK_COUNT
			| SSH_FILEXFER_ATTR_UNTRANSLATED | SSH_FILEXFER_ATTR_CTIME;
	// Types
	public static final int SSH_FILEXFER_TYPE_REGULAR = 1;
	public static final int SSH_FILEXFER_TYPE_DIRECTORY = 2;
	public static final int SSH_FILEXFER_TYPE_SYMLINK = 3;
	public static final int SSH_FILEXFER_TYPE_SPECIAL = 4;

	public static final int SSH_FILEXFER_TYPE_UNKNOWN = 5;
	// This is only used for version >= 5
	public static final int SSH_FILEXFER_TYPE_SOCKET = 6;
	public static final int SSH_FILEXFER_TYPE_CHAR_DEVICE = 7;
	public static final int SSH_FILEXFER_TYPE_BLOCK_DEVICE = 8;

	public static final int SSH_FILEXFER_TYPE_FIFO = 9;
	// Attribute bits
	public static final int SSH_FILEXFER_ATTR_FLAGS_READONLY = 0x00000001;
	public static final int SSH_FILEXFER_ATTR_FLAGS_SYSTEM = 0x00000002;
	public static final int SSH_FILEXFER_ATTR_FLAGS_HIDDEN = 0x00000004;
	public static final int SSH_FILEXFER_ATTR_FLAGS_CASE_INSENSITIVE = 0x00000008;
	public static final int SSH_FILEXFER_ATTR_FLAGS_ARCHIVE = 0x00000010;
	public static final int SSH_FILEXFER_ATTR_FLAGS_ENCRYPTED = 0x00000020;
	public static final int SSH_FILEXFER_ATTR_FLAGS_COMPRESSED = 0x00000040;
	public static final int SSH_FILEXFER_ATTR_FLAGS_SPARSE = 0x00000080;
	public static final int SSH_FILEXFER_ATTR_FLAGS_APPEND_ONLY = 0x00000100;
	public static final int SSH_FILEXFER_ATTR_FLAGS_IMMUTABLE = 0x00000200;
	public static final int SSH_FILEXFER_ATTR_FLAGS_SYNC = 0x00000400;

	public static final int SSH_FILEXFER_ATTR_FLAGS_TRANSLATION_ERR = 0x00000800;
	// ACL Flags
	public static final int SFX_ACL_CONTROL_INCLUDED = 0x00000001;
	public static final int SFX_ACL_CONTROL_PRESENT = 0x00000002;
	public static final int SFX_ACL_CONTROL_INHERITED = 0x00000004;
	public static final int SFX_ACL_AUDIT_ALARM_INCLUDED = 0x00000010;

	public static final int SFX_ACL_AUDIT_ALARM_INHERITED = 0x00000020;
	// Text Hint
	public static final int SSH_FILEXFER_ATTR_KNOWN_TEXT = 0x00;
	public static final int SSH_FILEXFER_ATTR_GUESSED_TEXT = 0x01;
	public static final int SSH_FILEXFER_ATTR_KNOWN_BINARY = 0x02;

	public static final int SSH_FILEXFER_ATTR_GUESSED_BINARY = 0x00;

	/**
	 * Format mask constant can be used to mask off a file type from the mode.
	 */
	public static final int S_IFMT = 0xF000;

	/**
	 * Format mask constant to mask off file mode from the type.
	 */
	public static final int S_MODE_MASK = 0x0FFF;

	/** Permissions flag: Identifies the file as a socket */
	public static final int S_IFSOCK = 0xC000;

	/** Permissions flag: Identifies the file as a symbolic link */
	public static final int S_IFLNK = 0xA000;

	/** Permissions flag: Identifies the file as a regular file */
	public static final int S_IFREG = 0x8000;

	/** Permissions flag: Identifies the file as a block special file */
	public static final int S_IFBLK = 0x6000;

	/** Permissions flag: Identifies the file as a directory */
	public static final int S_IFDIR = 0x4000;

	/** Permissions flag: Identifies the file as a character device */
	public static final int S_IFCHR = 0x2000;

	/** Permissions flag: Identifies the file as a pipe */
	public static final int S_IFIFO = 0x1000;

	/**
	 * Permissions flag: Bit to determine whether a file is executed as the owner
	 */
	public final static int S_ISUID = 0x800;

	/**
	 * Permissions flag: Bit to determine whether a file is executed as the group
	 * owner
	 */
	public final static int S_ISGID = 0x400;

	/** Permissions flag: Permits the owner of a file to read the file. */
	public final static int S_IRUSR = 0x100;

	/** Permissions flag: Permits the owner of a file to write to the file. */
	public final static int S_IWUSR = 0x80;

	/**
	 * Permissions flag: Permits the owner of a file to execute the file or to
	 * search the file's directory.
	 */
	public final static int S_IXUSR = 0x40;

	/** Permissions flag: Permits a file's group to read the file. */
	public final static int S_IRGRP = 0x20;

	/** Permissions flag: Permits a file's group to write to the file. */
	public final static int S_IWGRP = 0x10;

	/**
	 * Permissions flag: Permits a file's group to execute the file or to search the
	 * file's directory.
	 */
	public final static int S_IXGRP = 0x08;

	/** Permissions flag: Permits others to read the file. */
	public final static int S_IROTH = 0x04;

	/** Permissions flag: Permits others to write to the file. */
	public final static int S_IWOTH = 0x02;

	/**
	 * Permissions flag: Permits others to execute the file or to search the file's
	 * directory.
	 */
	public final static int S_IXOTH = 0x01;

	private static boolean isFlagSet(long flag, long flags, int version, Optional<Long> supportedAttributeMask) {
		if (version >= 5 && supportedAttributeMask.isPresent()) {
			boolean set = ((flags & (flag & 0xFFFFFFFFL)) == (flag & 0xFFFFFFFFL));
			if (set) {
				set = ((supportedAttributeMask.get() & (flag & 0xFFFFFFFFL)) == (flag & 0xFFFFFFFFL));
			}
			return set;
		}
		return ((flags & (flag & 0xFFFFFFFFL)) == (flag & 0xFFFFFFFFL));
	}

	private final String charsetEncoding;
	private final Optional<Long> supportedAttributeMask;
	private final Optional<Long> supportedAttributeBits;
	private final Optional<UnsignedInteger64> allocationSize;
	private final Optional<FileTime> lastAttributesModifiedTime;
	private final Optional<UnsignedInteger32> aclFlags;
	private final List<ACL> acls;
	private final Optional<UnsignedInteger32> attributeBits;
	private final Optional<String> mimeType;
	private final Optional<Byte> textHint;
	private final Optional<UnsignedInteger32> attributeBitsValid;
	private final Optional<Integer> linkCount;
	private final Optional<String> untranslatedName;
	private final int type;
	private final Optional<Integer> uid;
	private final Optional<Integer> gid;
	private final long flags;
	private final Optional<String> username;
	private final Optional<String> group;
	private final Optional<UnsignedInteger64> size;
	private final Optional<FileTime> lastAccessTime;
	private final Optional<FileTime> createTime;
	private final Optional<FileTime> lastModifiedTime;
	private final Map<String, byte[]> extendedAttributes;
	private final Optional<PosixPermissions> permissions;

	private SftpFileAttributes(SftpFileAttributesBuilder builder) {
		this.size = builder.size;
		this.type = builder.type;
		this.charsetEncoding = builder.calcCharset();
		this.supportedAttributeBits = builder.supportedAttributeBits;
		this.supportedAttributeMask = builder.supportedAttributeMask;
		this.flags = builder.flags;
		this.allocationSize = builder.allocationSize;
		this.uid = builder.uid;
		this.username = builder.username;
		this.gid = builder.gid;
		this.group = builder.group;
		this.permissions = builder.permissions;
		this.lastAccessTime = builder.lastAccessTime;
		this.createTime = builder.createTime;
		this.lastModifiedTime = builder.lastModifiedTime;
		this.lastAttributesModifiedTime = builder.lastAttributesModifiedTime;
		this.acls = Collections.unmodifiableList(new ArrayList<>(builder.acls));
		this.aclFlags = builder.aclFlags;
		this.attributeBits = builder.attributeBits;
		this.mimeType = builder.mimeType;
		this.textHint = builder.textHint;
		this.attributeBitsValid = builder.attributeBitsValid;
		this.linkCount = builder.linkCount;
		this.untranslatedName = builder.untranslatedName;
		this.extendedAttributes = Collections.unmodifiableMap(new HashMap<String, byte[]>(builder.extendedAttributes));
	}

	public List<ACL> acls() {
		return acls;
	}

	public UnsignedInteger32 aclsFlag() {
		return aclFlags.orElse(UnsignedInteger32.ZERO);
	}

	public Optional<UnsignedInteger32> aclsFlagOr() {
		return aclFlags;
	}

	public UnsignedInteger64 allocationSize() {
		return allocationSize.orElse(UnsignedInteger64.ZERO);
	}

	public Optional<UnsignedInteger64> allocationSizeOr() {
		return allocationSize;
	}

	public UnsignedInteger32 attributeBits() {
		return attributeBits.orElse(UnsignedInteger32.ZERO);
	}

	public Optional<UnsignedInteger32> attributeBitsOr() {
		return attributeBits;
	}

	public UnsignedInteger32 attributeBitsValid() {
		return attributeBitsValid.orElseThrow(() -> new IllegalStateException("No valid attribute bits set."));
	}

	public Optional<UnsignedInteger32> attributeBitsValidOr() {
		return attributeBitsValid;
	}

	public String charsetEncoding() {
		return charsetEncoding;
	}

	public FileTime createTime() {
		return createTime.orElseGet(() -> FileTime.fromMillis(0));
	}

	public Optional<FileTime> createTimeOr() {
		return createTime;
	}

	public byte[] extendedAttribute(String key) {
		return extendedAttributeOr(key)
				.orElseThrow(() -> new IllegalArgumentException(MessageFormat.format("No such key {0}", key)));
	}

	public Optional<byte[]> extendedAttributeOr(String key) {
		return Optional.ofNullable(extendedAttributes.get(key));
	}

	public Map<String, byte[]> extendedAttributes() {
		/* TODO: this will be a unmodifiable member map at 3.2.0 */
		return Collections.unmodifiableMap(extendedAttributes);
	}

	public long flags() {
		return flags;
	}

	public int gid() {
		return gid.orElse(0);
	}

	public Optional<Integer> gidOr() {
		return gid;
	}

	public String group() {
		return group.orElse("");
	}

	public Optional<String> groupOr() {
		return group;
	}

	public boolean hasAclFlags() {
		return aclFlags.isPresent();
	}

	public boolean hasAllocationSize() {
		return allocationSize.isPresent();
	}

	public boolean hasAttributeBits() {
		return attributeBits.isPresent();
	}

	public boolean hasCreateTime() {
		return createTime.isPresent();
	}

	public boolean hasExtendedAttribute(String key) {
		return extendedAttributes.containsKey(key);
	}

	public boolean hasGid() {
		return gid.isPresent();
	}

	public boolean hasGroup() {
		return group.isPresent();
	}

	public boolean hasLastAccessTime() {
		return lastAccessTime.isPresent();
	}

	public boolean hasLastAttributesModifiedTime() {
		return lastAttributesModifiedTime.isPresent();
	}

	public boolean hasLastModifiedTime() {
		return lastModifiedTime.isPresent();
	}

	public boolean hasPermissions() {
		return permissions.isPresent();
	}

	public boolean hasSize() {
		return size.isPresent();
	}

	public boolean hasSubSecondTimes() {
		return (flags & SSH_FILEXFER_ATTR_SUBSECOND_TIMES) != 0;
	}

	public boolean hasSupportedAttributeBits() {
		return supportedAttributeBits.isPresent();
	}

	public boolean hasSupportedAttributeMask() {
		return supportedAttributeMask.isPresent();
	}

	public boolean hasUid() {
		return uid.isPresent();
	}

	public boolean hasUsername() {
		return username.isPresent();
	}

	public boolean isAppendOnly() {
		return isAttributeBitSet(SSH_FILEXFER_ATTR_FLAGS_APPEND_ONLY);
	}

	public boolean isArchive() {
		return isAttributeBitSet(SSH_FILEXFER_ATTR_FLAGS_ARCHIVE);
	}

	public boolean isAttributeBitSet(long attributeBit) {
		return attributeBits.isPresent()
				&& ((attributeBits.get().longValue() & (attributeBit & 0xFFFFFFFFL)) == (attributeBit & 0xFFFFFFFFL));
	}

	public boolean isBlock() {
		return type == SSH_FILEXFER_TYPE_BLOCK_DEVICE;
	}

	public boolean isCaseInsensitive() {
		return isAttributeBitSet(SSH_FILEXFER_ATTR_FLAGS_CASE_INSENSITIVE);
	}

	public boolean isCharacter() {
		return type == SSH_FILEXFER_TYPE_CHAR_DEVICE;
	}

	public boolean isSpecial() {
		return type == SSH_FILEXFER_TYPE_SPECIAL;
	}

	public boolean isCompressed() {
		return isAttributeBitSet(SSH_FILEXFER_ATTR_FLAGS_COMPRESSED);
	}

	public boolean isDirectory() {
		return type == SSH_FILEXFER_TYPE_DIRECTORY;
	}

	public boolean isEncrypted() {
		return isAttributeBitSet(SSH_FILEXFER_ATTR_FLAGS_ENCRYPTED);
	}

	public boolean isFifo() {
		return type == SSH_FILEXFER_TYPE_FIFO;
	}

	public boolean isFile() {
		return type == SSH_FILEXFER_TYPE_REGULAR;
	}

	public boolean isHidden() {
		return isAttributeBitSet(SSH_FILEXFER_ATTR_FLAGS_HIDDEN);
	}

	public boolean isImmutable() {
		return isAttributeBitSet(SSH_FILEXFER_ATTR_FLAGS_IMMUTABLE);
	}

	public boolean isLink() {
		return type == SSH_FILEXFER_TYPE_SYMLINK;
	}

	public boolean isReadOnly() {
		return isAttributeBitSet(SSH_FILEXFER_ATTR_FLAGS_READONLY);
	}

	public boolean isSocket() {
		return type == SSH_FILEXFER_TYPE_SOCKET;
	}

	public boolean isSparse() {
		return isAttributeBitSet(SSH_FILEXFER_ATTR_FLAGS_SPARSE);
	}

	public boolean isSync() {
		return isAttributeBitSet(SSH_FILEXFER_ATTR_FLAGS_SYNC);
	}

	public boolean isSubSecondTimes() {
		return ( flags & SSH_FILEXFER_ATTR_SUBSECOND_TIMES ) != 0;
	}

	public boolean isSystem() {
		return isAttributeBitSet(SSH_FILEXFER_ATTR_FLAGS_SYSTEM);
	}

	public FileTime lastAccessTime() {
		return lastAccessTime.orElseGet(() -> FileTime.fromMillis(0));
	}

	public Optional<FileTime> lastAccessTimeOr() {
		return lastAccessTime;
	}

	public FileTime lastAttributesModifiedTime() {
		return lastAttributesModifiedTime.orElseGet(() -> FileTime.fromMillis(0));
	}

	public Optional<FileTime> lastAttributesModifiedTimeOr() {
		return lastAttributesModifiedTime;
	}

	public FileTime lastModifiedTime() {
		return lastModifiedTime.orElseGet(() -> FileTime.fromMillis(0));
	}

	public Optional<FileTime> lastModifiedTimeOr() {
		return lastModifiedTime;
	}

	public int linkCount() {
		return linkCount.orElse(0);
	}

	public Optional<Integer> linkCountOr() {
		return linkCount;
	}

	public String mimeType() {
		return mimeType.orElse("application/octet-stream");
	}

	public Optional<String> mimeTypeOr() {
		return mimeType;
	}

	public PosixPermissions permissions() {
		return permissions.orElse(PosixPermissions.EMPTY);
	}

	public Optional<PosixPermissions> permissionsOr() {
		return permissions;
	}

	public UnsignedInteger64 size() {
		return size.orElse(UnsignedInteger64.ZERO);
	}

	public Optional<UnsignedInteger64> sizeOr() {
		return size;
	}

	public long supportedAttributeBits() {
		return supportedAttributeBits.orElse(0l);
	}

	public Optional<Long> supportedAttributeBitsOr() {
		return supportedAttributeBits;
	}

	public long supportedAttributeMask() {
		return supportedAttributeMask.orElse(0l);
	}

	public Optional<Long> supportedAttributeMaskOr() {
		return supportedAttributeMask;
	}

	public byte textHint() {
		return textHint.orElseThrow(() -> new IllegalStateException("No text hint set."));
	}

	public Optional<Byte> textHintOr() {
		return textHint;
	}

	/**
	 * Returns a formatted byte array suitable for encoding into SFTP subsystem
	 * messages.
	 * 
	 * @return byte[]
	 * 
	 * @throws IOException
	 */
	public byte[] toByteArray(int version) throws IOException {
		var baw = new ByteArrayWriter();

		try {
			switch (version) {
			case 6:
				baw.writeInt(flags & VERSION_6_FLAGS);
				break;
			case 5:
				baw.writeInt(flags & VERSION_5_FLAGS);
				break;
			case 4:
				baw.writeInt(flags & VERSION_4_FLAGS);
				break;
			default:
				baw.writeInt(flags & VERSION_3_FLAGS);
				break;
			}

			if (version > 3)
				baw.write(type);

			if (isFlagSet(SSH_FILEXFER_ATTR_SIZE, version)) {
				baw.write(size.orElse(UnsignedInteger64.ZERO).toByteArray());
			}

			if (version <= 3 && isFlagSet(SSH_FILEXFER_ATTR_UIDGID, version)) {
				baw.writeInt(uid.orElse(0));
				baw.writeInt(gid.orElse(0));
			} else if (version > 3 && isFlagSet(SSH_FILEXFER_ATTR_OWNERGROUP, version)) {
				baw.writeString(username.orElseGet(() -> uid.map(Long::toString).orElse("")), charsetEncoding);
				baw.writeString(group.orElseGet(() -> gid.map(Long::toString).orElse("")), charsetEncoding);
			}

			if (isFlagSet(SSH_FILEXFER_ATTR_PERMISSIONS, version)) {
				baw.writeInt((permissions.map(PosixPermissions::asLong).orElse(0l) & S_MODE_MASK) | toModeType());
			}

			if (version <= 3 && isFlagSet(SSH_FILEXFER_ATTR_ACCESSTIME, version)) {
				baw.writeInt(lastAccessTime.map(a -> a.to(TimeUnit.SECONDS)).orElse(0l));
				baw.writeInt(lastModifiedTime.map(a -> a.to(TimeUnit.SECONDS)).orElse(0l));
			} else if (version > 3) {

				if (isFlagSet(SSH_FILEXFER_ATTR_ACCESSTIME, version)) {
					baw.writeUINT64(lastAccessTime.map(a -> a.to(TimeUnit.SECONDS)).orElse(0l));
					if (isFlagSet(SSH_FILEXFER_ATTR_SUBSECOND_TIMES, version)) {
						baw.writeUINT32(new UnsignedInteger32(lastAccessTime.map(this::nanosFromFileTime).orElse(0l)));
					}
				}

				if (isFlagSet(SSH_FILEXFER_ATTR_CREATETIME, version)) {
					baw.writeUINT64(createTime.map(a -> a.to(TimeUnit.SECONDS)).orElse(0l));
					if (isFlagSet(SSH_FILEXFER_ATTR_SUBSECOND_TIMES, version)) {
						baw.writeUINT32(new UnsignedInteger32(createTime.map(this::nanosFromFileTime).orElse(0l)));
					}
				}

				if (isFlagSet(SSH_FILEXFER_ATTR_MODIFYTIME, version)) {
					baw.writeUINT64(lastModifiedTime.map(a -> a.to(TimeUnit.SECONDS)).orElse(0l));
					if (isFlagSet(SSH_FILEXFER_ATTR_SUBSECOND_TIMES, version)) {
						baw.writeUINT32(
								new UnsignedInteger32(lastModifiedTime.map(this::nanosFromFileTime).orElse(0l)));
					}
				}

			}

			if (isFlagSet(SSH_FILEXFER_ATTR_ACL, version)) {
				var tmp = new ByteArrayWriter();
				try {
					tmp.writeInt(acls.size());
					for (var acl : acls) {
						tmp.writeInt(acl.getType());
						tmp.writeInt(acl.getFlags());
						tmp.writeInt(acl.getMask());
						tmp.writeString(acl.getWho());
					}
					baw.writeBinaryString(tmp.toByteArray());
				} finally {
					tmp.close();
				}
			}

			if (version >= 5 && isFlagSet(SSH_FILEXFER_ATTR_BITS, version)) {
				baw.writeInt(attributeBits.map(a -> supportedAttributeBits.isEmpty() ? a.longValue()
						: a.longValue() & supportedAttributeBits.get().longValue()).orElse(0l));
			}
//
			if (isFlagSet(SSH_FILEXFER_ATTR_EXTENDED, version)) {
				baw.writeInt(extendedAttributes.size());
				for (String key : extendedAttributes.keySet()) {
					baw.writeString(key);
					baw.writeBinaryString((byte[]) extendedAttributes.get(key));
				}
			}

			return baw.toByteArray();

		} finally {
			baw.close();
		}
	}

	public String toMaskString() {
		return permissions.map(PosixPermissions::asMaskString).orElse("----");
	}

	public int toModeType() {
		switch (type) {
		case SSH_FILEXFER_TYPE_DIRECTORY:
			return S_IFDIR;
		case SSH_FILEXFER_TYPE_REGULAR:
			return S_IFREG;
		case SSH_FILEXFER_TYPE_SYMLINK:
			return S_IFLNK;
		case SSH_FILEXFER_TYPE_CHAR_DEVICE:
			return S_IFCHR;
		case SSH_FILEXFER_TYPE_BLOCK_DEVICE:
			return S_IFBLK;
		case SSH_FILEXFER_TYPE_FIFO:
			return S_IFIFO;
		case SSH_FILEXFER_TYPE_SOCKET:
			return S_IFSOCK;
		case SSH_FILEXFER_TYPE_SPECIAL:
		case SSH_FILEXFER_TYPE_UNKNOWN:
		default:
			return 0;
		}
	}

	public String toPermissionsString() {
		var str = new StringBuilder();
		switch (type) {
		case SSH_FILEXFER_TYPE_BLOCK_DEVICE:
			str.append('b');
			break;
		case SSH_FILEXFER_TYPE_CHAR_DEVICE:
			str.append('c');
			break;
		case SSH_FILEXFER_TYPE_DIRECTORY:
			str.append('d');
			break;
		case SSH_FILEXFER_TYPE_FIFO:
			str.append('p');
			break;
		case SSH_FILEXFER_TYPE_SOCKET:
			str.append('s');
			break;
		case SSH_FILEXFER_TYPE_SYMLINK:
			str.append('l');
			break;
		case SSH_FILEXFER_TYPE_UNKNOWN:
		case SSH_FILEXFER_TYPE_REGULAR:
		default:
			str.append('-');
			break;
		}
		str.append(permissions.map(PosixPermissions::asFileModesString).orElse("---------"));
		return str.toString();
	}

	public int type() {
		return type;
	}

	public int uid() {
		return uid.orElse(0);
	}

	public Optional<Integer> uidOr() {
		return uid;
	}

	public String untranslatedName() {
		return untranslatedName.orElseThrow(() -> new IllegalStateException("No untranslated name set"));
	}

	public Optional<String> untranslatedNameOr() {
		return untranslatedName;
	}

	public String username() {
		return username.orElse("");
	}

	public Optional<String> usernameOr() {
		return username;
	}

	private boolean isFlagSet(long flag, int version) {
		return isFlagSet(flag, flags, version, supportedAttributeMask);
	}

	private long nanosFromFileTime(FileTime filetime) {
		return Integer.toUnsignedLong(filetime.toInstant().getNano());
	}

	public Optional<String> bestUsernameOr() {
		return username.or(() -> uid.map(u -> String.valueOf(u)));
	}

	public String bestUsername() {
		return bestUsernameOr().orElse("nouser");
	}

	public Optional<String> bestGroupOr() {
		return group.or(() -> gid.map(g -> String.valueOf(g)));
	}

	public String bestGroup() {
		return bestGroupOr().orElse("nogroup");
	}
}
