package com.sshtools.common.sftp;

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

import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import com.sshtools.common.util.UnsignedInteger32;

/**
 * {@link PosixPermissions} and {@link PosixPermissionsBuilder} allow querying
 * and creation of an immutable set of posix file permissions. Natively, this is
 * a stored as a set of {@link PosixFilePermission}, but may be expressed and
 * parsed in a variety of different ways, such as octal strings, "file mode
 * strings", raw unsigned values. A mode may also be created by combining user,
 * group and other elements.
 * <p>
 * Note that these classes only deal with the basic user, group; and other file
 * access mode elements e.g. <code>rw-r--r--</code>. Other common Posix
 * indicators such as the 'd' for directory prefix, 's' for Set UID indicators,
 * 'l' for links etc, are not parsed or generated here. Neither are the extended
 * SFTP bitmask flags.
 * <p>
 * This makes use of the existing core Java classes {@link PosixFilePermission}
 * and {@link PosixFilePermissions}, adding additional functionality and a
 * builder pattern for creation.
 * <p>
 * For example, to set the full set of permissions in one go :-
 *
 * <blockquote><pre>
 * sftp.chmod(PosixPermissionsBuilder.create().
 * 		fromFileModeString("rw-rw-rw-").
 * 		build(), "/path/to/file");
 * </pre></blockquote>
 * <p>
 * Or to retrieve permission, remove write permissions and set them back :-
 * 
 * <blockquote><pre>
 * sftp.chmod(PosixPermissionsBuilder.create().
 * 		fromPosixPermissions(sftp.stat("/path/to/file").getPosixPermissions).
 * 		withoutWritePermissions().
 * 		build(), "/path/to/file");
 * </pre></blockquote>
 */
public final class PosixPermissions {

	/**
	 * A builder for {@link PosixPermissions}.
	 */
	public final static class PosixPermissionsBuilder {
		/**
		 * Create a new {@link PosixPermissionsBuilder}
		 * 
		 * @return builder
		 */
		public static PosixPermissionsBuilder create() {
			return new PosixPermissionsBuilder();
		}

		private Set<PosixFilePermission> perms = new LinkedHashSet<>();

		private PosixPermissionsBuilder() {
		}

		/**
		 * Build a new {@link PosixPermissions} set.
		 * 
		 * @return permissions
		 */
		public PosixPermissions build() {
			return new PosixPermissions(this);
		}

		/**
		 * Add all permissions for everyone.
		 * 
		 * @return this for chaining
		 */
		public PosixPermissionsBuilder fromAllPermissions() {
			return fromPermissions(PosixFilePermission.values());
		}

		/**
		 * Set the mode using a either a raw <code>int</code> or a raw <code>long</code>.
		 * Only the first 32 bits of the value will be used.
		 * <p>
		 * Any existing value already built will be entirely replaced with this new
		 * value.
		 * 
		 * @param raw mode value
		 * @return this for chaining
		 */
		public PosixPermissionsBuilder fromBitmask(long mode) {
			perms.clear();
			;
			for (var perm : PosixFilePermission.values()) {
				if ((mode & toMask(perm)) != 0) {
					perms.add(perm);
				}
			}
			return this;
		}

		/**
		 * Set the mode using a a raw unsigned 32 bit value.
		 * <p>
		 * Any existing value already built will be entirely replaced with this new
		 * value.
		 * 
		 * @param raw mode value
		 * @return this for chaining
		 */
		public PosixPermissionsBuilder fromBitmask(UnsignedInteger32 mode) {
			return fromBitmask(mode.longValue());
		}

		/**
		 * Set the mode using a full (9 character) file mode string. The first 3
		 * characters are for the user, the second 3 are for group and the final 3 are
		 * for other users. The correlating output is
		 * {@link PosixPermissions#asFileModesString()}.
		 * <p>
		 * Any existing value already built will be entirely replaced with this new
		 * value.
		 * 
		 * @param file mode string
		 * @return this for chaining
		 */
		public PosixPermissionsBuilder fromFileModeString(String fileModes) {
			perms.clear();
			perms.addAll(PosixFilePermissions.fromString(fileModes));
			return this;
		}

		/**
		 * Set the mode using a file mode string. The string can be any length up to 9
		 * characters, with any length less than this padding the string with '-' until
		 * it is 9 characters in length. The first 3 characters are for the user, the
		 * second 3 are for group and the final 3 are for other users. The correlating
		 * output is {@link PosixPermissions#asFileModesString()}.
		 * <p>
		 * Any existing value already built will be entirely replaced with this new
		 * value.
		 * 
		 * @param file mode string
		 * @return this for chaining
		 */
		public PosixPermissionsBuilder fromLaxFileModeString(String fileModes) {
			while (fileModes.length() < 9)
				fileModes += "-";
			return fromFileModeString(fileModes);
		}

		/**
		 * Set the mode using a UNIX style mask octal string, for example '0644'. This
		 * should be in the same format as that output by
		 * {@link PosixPermissions#asMaskString()}.
		 * <p>
		 * Any existing value already built will be entirely replaced with this new
		 * value.
		 * 
		 * @param maskString octal mask string
		 * @return this for chaining
		 */
		public PosixPermissionsBuilder fromMaskString(String maskString) {
			if (maskString.length() != 4) {
				throw new IllegalArgumentException("Mask length must be 4");
			}
			try {
				return fromBitmask(new UnsignedInteger32(String.valueOf(Integer.parseInt(maskString, 8))));
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException("Mask must be 4 digit octal number.");
			}
		}

		/**
		 * Removes all permission from the set to be built.
		 * 
		 * @return this for chaining
		 */
		public PosixPermissionsBuilder fromNoPermissions() {
			perms.clear();
			return this;
		}

		/**
		 * Set the mode using an array of {@link PosixFilePermission}.
		 * <p>
		 * Any existing value already built will be entirely replaced with this new
		 * value.
		 * 
		 * @param permissions permissions
		 * @return this for chaining
		 */
		public PosixPermissionsBuilder fromPermissions(PosixFilePermission... permissions) {
			return fromPermissions(Arrays.asList(permissions));
		}

		/**
		 * Set the mode using a collection of {@link PosixFilePermission}.
		 * <p>
		 * Any existing value already built will be entirely replaced with this new
		 * value.
		 * 
		 * @param permissions permissions
		 * @return this for chaining
		 */
		public PosixPermissionsBuilder fromPermissions(Collection<PosixFilePermission> permissions) {
			perms.clear();
			perms.addAll(permissions);
			return this;
		}

		/**
		 * Set the mode using an existing {@link PosixPermissions}.
		 * <p>
		 * Any existing value already built will be entirely replaced with this new
		 * value.
		 * 
		 * @param permissions permissions
		 * @return this for chaining
		 */
		public PosixPermissionsBuilder fromPosixPermissions(PosixPermissions permissions) {
			perms.clear();
			perms.addAll(permissions.perms);
			return this;
		}

		/**
		 * Set the mode using a UNIX style umask, for example '0022' will result in 0022
		 * ^ 0777. This should be in the same format as that output by
		 * {@link PosixPermissions#asUmaskString()}.
		 * <p>
		 * Any existing value already built will be entirely replaced with this new
		 * value.
		 * 
		 * @param umask octal umask string
		 * @return this for chaining
		 */
		public PosixPermissionsBuilder fromUmaskString(String umask) {
			if (umask.length() != 4) {
				throw new IllegalArgumentException("Mask length must be 4");
			}
			try {
				return fromBitmask(new UnsignedInteger32(String.valueOf(Integer.parseInt(umask, 8) ^ 0777)));
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException("Mask must be 4 digit octal number.");
			}
		}

		/**
		 * Adds execute permission to user, group and other. 
		 * 
		 * @return this for chaining
		 */
		public PosixPermissionsBuilder withAllExecute() {
			return withPermissions(PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OTHERS_EXECUTE);
		}

		/**
		 * Adds read permission to user, group and other. 
		 * 
		 * @return this for chaining
		 */
		public PosixPermissionsBuilder withAllRead() {
			return withPermissions(PosixFilePermission.OWNER_READ, PosixFilePermission.GROUP_READ, PosixFilePermission.OTHERS_READ);
		}

		/**
		 * Adds write permission to user, group and other. 
		 * 
		 * @return this for chaining
		 */
		public PosixPermissionsBuilder withAllWrite() {
			return withPermissions(PosixFilePermission.OWNER_WRITE, PosixFilePermission.GROUP_WRITE, PosixFilePermission.OTHERS_WRITE);
		}

		/**
		 * Adds a set of modes using bitmask flags via a raw
		 * <code>int</code> or a raw <code>long</code>. Only the first 32 bits of the
		 * value will be used.
		 * 
		 * @param bitmask mask of flags to add
		 * @return this for chaining
		 */
		public PosixPermissionsBuilder withBitmaskFlags(long flags) {
			for (var perm : PosixFilePermission.values()) {
				if ((flags & toMask(perm)) != 0) {
					perms.add(perm);
				}
			}
			return this;
		}

		/**
		 * Set the mode using a format compatible with the UNIX chmod command. It supports either
		 * settings, adding or removing permissions from either the user, group or other parts.
		 * <p>
		 * For example, <code>u=rw,g=r,o=r</code> would set read-write on the user, and only read on
		 * group and other. Or, <code>u+x,g=,o=</code> would add execute permission for user, and clear
		 * group and other of all permissions.
		 * <p>
		 * Note this does not replace all permissions in this builder unless the argment string
		 * provided specifies  to do so, e.g. <code>u=,g=,o=</code> or 
		 * <code>u-rwx,g-rwx,o-rwx</code> would remove all permissions.
		 * <p>
		 * You can use an octal mask string with <code>chmod</code> and so that is also possible here. 
		 * 
		 * @param chmodArgument permissions expressed in format used by chmod UNIX command
		 * @return this for chaining
		 */
		public PosixPermissionsBuilder withChmodArgumentString(String chmodArgument) {

			if(chmodArgument.matches("\\d+")) {
				return fromMaskString(chmodArgument);
			}
			else {
				for(var el : chmodArgument.split(",")) {
					if(el.startsWith("u=") || el.startsWith("u+") || el.startsWith("u-")) {
						if(el.startsWith("u=")) {
							perms.removeAll(Arrays.asList(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
						}
						for(var ch : el.substring(2).toCharArray()) {
							switch(ch) {
							case 'r':
								if(el.startsWith("u-"))
									perms.remove(PosixFilePermission.OWNER_READ);
								else
									perms.add(PosixFilePermission.OWNER_READ);
								break;
							case 'w':
								if(el.startsWith("u-"))
									perms.remove(PosixFilePermission.OWNER_WRITE);
								else
									perms.add(PosixFilePermission.OWNER_WRITE);
								break;
							case 'x':
								if(el.startsWith("u-"))
									perms.remove(PosixFilePermission.OWNER_EXECUTE);
								else
									perms.add(PosixFilePermission.OWNER_EXECUTE);
								break;
							default:
								throw new IllegalArgumentException("Unknown user mode '" + ch + "'");
							}
						}
					}
					else if(el.startsWith("g=") || el.startsWith("g+") || el.startsWith("g-")) {
						if(el.startsWith("g=")) {
							perms.removeAll(Arrays.asList(PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE, PosixFilePermission.GROUP_EXECUTE));
						}
						for(var ch : el.substring(2).toCharArray()) {
							switch(ch) {
							case 'r':
								if(el.startsWith("g-"))
									perms.remove(PosixFilePermission.GROUP_READ);
								else
									perms.add(PosixFilePermission.GROUP_READ);
								break;
							case 'w':
								if(el.startsWith("g-"))
									perms.remove(PosixFilePermission.GROUP_WRITE);
								else
									perms.add(PosixFilePermission.GROUP_WRITE);
								break;
							case 'x':
								if(el.startsWith("g-"))
									perms.remove(PosixFilePermission.GROUP_EXECUTE);
								else
									perms.add(PosixFilePermission.GROUP_EXECUTE);
								break;
							default:
								throw new IllegalArgumentException("Unknown group mode '" + ch + "'");
							}
						}
					}
					else if(el.startsWith("o=") || el.startsWith("o+") || el.startsWith("o-")) {
						if(el.startsWith("o=")) {
							perms.removeAll(Arrays.asList(PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_WRITE, PosixFilePermission.OTHERS_EXECUTE));
						}
						for(var ch : el.substring(2).toCharArray()) {
							switch(ch) {
							case 'r':
								if(el.startsWith("o-"))
									perms.remove(PosixFilePermission.OTHERS_READ);
								else
									perms.add(PosixFilePermission.OTHERS_READ);
								break;
							case 'w':
								if(el.startsWith("o-"))
									perms.remove(PosixFilePermission.OTHERS_WRITE);
								else
									perms.add(PosixFilePermission.OTHERS_WRITE);
								break;
							case 'x':
								if(el.startsWith("o-"))
									perms.remove(PosixFilePermission.OTHERS_EXECUTE);
								else
									perms.add(PosixFilePermission.OTHERS_EXECUTE);
								break;
							default:
								throw new IllegalArgumentException("Unknown others mode '" + ch + "'");
							}
						}
					} 
					else {
						throw new IllegalArgumentException("Unknown scope '" + el + "'");
					}
				}
			}
			return this;
		}

		/**
		 * Remove a set of modes using bitmask flags via a raw
		 * <code>int</code> or a raw <code>long</code>. Only the first 32 bits of the
		 * value will be used.
		 * 
		 * @param bitmask mask of flags to remove
		 * @return this for chaining
		 */
		public PosixPermissionsBuilder withoutBitmaskFlags(long flags) {
			for (var perm : PosixFilePermission.values()) {
				if ((flags & toMask(perm)) != 0) {
					perms.remove(perm);
				}
			}
			return this;
		}

		/**
		 * Removes all execute permissions.
		 * 
		 * @return this for chaining
		 */
		public PosixPermissionsBuilder withoutExecutePermissions() {
			perms.removeAll(Arrays.asList(PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_EXECUTE,
					PosixFilePermission.OTHERS_EXECUTE));
			return this;
		}

		/**
		 * Removes all group <strong>and</strong> other permissions.
		 * 
		 * @return this for chaining
		 */
		public PosixPermissionsBuilder withoutGroupOtherPermissions() {
			perms.removeAll(Arrays.asList(PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE,
					PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OTHERS_READ,
					PosixFilePermission.OTHERS_WRITE, PosixFilePermission.OTHERS_EXECUTE));
			return this;
		}

		/**
		 * Removes all other permissions.
		 * 
		 * @return this for chaining
		 */
		public PosixPermissionsBuilder withoutOtherPermissions() {
			perms.removeAll(Arrays.asList(PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_WRITE,
					PosixFilePermission.OTHERS_EXECUTE));
			return this;
		}

		/**
		 * Remove a collection of {@link PosixFilePermission} from the built set of
		 * permissions.
		 * 
		 * @param permissions permissions to remove
		 * @return this for chaining
		 */
		public PosixPermissionsBuilder withoutPermissions(Collection<PosixFilePermission> permissions) {
			return withoutPermissions(permissions.toArray(new PosixFilePermission[0]));
		}

		/**
		 * Remove one or more {@link PosixFilePermission} from the built set of
		 * permissions.
		 * 
		 * @param permissions permissions to remove
		 * @return this for chaining
		 */
		public PosixPermissionsBuilder withoutPermissions(PosixFilePermission... permissions) {
			perms.removeAll(Arrays.asList(permissions));
			return this;
		}

		/**
		 * Removes all write permissions.
		 * 
		 * @return this for chaining
		 */
		public PosixPermissionsBuilder withoutWritePermissions() {
			perms.removeAll(Arrays.asList(PosixFilePermission.OWNER_WRITE, PosixFilePermission.GROUP_WRITE,
					PosixFilePermission.OTHERS_WRITE));
			return this;
		}

		/**
		 * Add a collection of {@link PosixFilePermission} to the built set of permissions.
		 * 
		 * @param permissions permissions
		 * @return this for chaining
		 */
		public PosixPermissionsBuilder withPermissions(Collection<PosixFilePermission> permissions) {
			return withPermissions(permissions.toArray(new PosixFilePermission[0]));
		}

		/**
		 * Add one or more {@link PosixFilePermission} to the built set of permissions.
		 * 
		 * @param permissions permissions
		 * @return this for chaining
		 */
		public PosixPermissionsBuilder withPermissions(PosixFilePermission... permissions) {
			perms.addAll(Arrays.asList(permissions));
			return this;
		}

	}

	/**
	 * Empty set of permissions (has a bitmask value of <code>zero</code>)
	 */
	public final static PosixPermissions EMPTY = PosixPermissionsBuilder.create().build();

	/**
	 * Get the bitmask flag value for a given permission.
	 * 
	 * @param permission permission
	 * @return bitmask flag value
	 */
	public static int toMask(PosixFilePermission permission) {
		switch (permission) {
		case OWNER_WRITE:
			return SftpFileAttributes.S_IWUSR;
		case OWNER_EXECUTE:
			return SftpFileAttributes.S_IXUSR;
		case GROUP_READ:
			return SftpFileAttributes.S_IRGRP;
		case GROUP_WRITE:
			return SftpFileAttributes.S_IWGRP;
		case GROUP_EXECUTE:
			return SftpFileAttributes.S_IXGRP;
		case OTHERS_READ:
			return SftpFileAttributes.S_IROTH;
		case OTHERS_WRITE:
			return SftpFileAttributes.S_IWOTH;
		case OTHERS_EXECUTE:
			return SftpFileAttributes.S_IXOTH;
		default:
			return SftpFileAttributes.S_IRUSR;
		}
	}

	private static int octal(int v, int r) {
		v >>>= r;
		return (((v & 0x04) != 0) ? 4 : 0) + (((v & 0x02) != 0) ? 2 : 0) + +(((v & 0x01) != 0) ? 1 : 0);
	}

	private final Set<PosixFilePermission> perms;

	private final int mode;

	private PosixPermissions(PosixPermissionsBuilder builder) {
		perms = Collections.unmodifiableSet(builder.perms);
		var m = 0;
		for (var perm : perms)
			m |= toMask(perm);
		mode = m;
	}

	/**
	 * Get the {@code String} representation of these permissions. It is guaranteed
	 * that the returned {@code String} can be parsed by the
	 * {@link PosixPermissionsBuilder#fromFileModeString(String)} method.
	 *
	 * @return the string representation of the permission set
	 */
	public String asFileModesString() {
		return PosixFilePermissions.toString(perms);
	}

	/**
	 * Get the mode as a signed integer. Note, you would never usually want to use
	 * this value without further manipulation to account for the sign bit. It is
	 * recommended you use {@link #asLong()} or {@link #asUInt32()} which guarantee
	 * the value will never be negative.
	 * 
	 * @return permissions as unsigned long
	 */
	public int asInt() {
		return mode;
	}

	/**
	 * Get the mode as an unsigned long. Note, the value will never be greater than
	 * the 32 bits needs to store a mode natively.
	 * 
	 * @return permissions as unsigned long
	 */
	public long asLong() {
		return Integer.toUnsignedLong(mode);
	}

	/**
	 * Return the UNIX style mode mask. This will be in the same format as that
	 * which may be parsed by
	 * {@link PosixPermissionsBuilder#fromMaskString(String)}.
	 * 
	 * @return mask string
	 */
	public String asMaskString() {
		var mode = asInt();
		var buf = new StringBuilder();
		buf.append('0');
		buf.append(octal(mode, 6));
		buf.append(octal(mode, 3));
		buf.append(octal(mode, 0));
		return buf.toString();
	}

	/**
	 * Get the set of individual permissions.
	 * 
	 * @return permissions
	 */
	public Set<PosixFilePermission> asPermissions() {
		return perms;
	}

	/**
	 * Get the mode as an unsigned 32 bit integer.
	 * 
	 * @return permissions as unsigned 32 bit integer
	 */
	public UnsignedInteger32 asUInt32() {
		return new UnsignedInteger32(asLong());
	}

	/**
	 * Get if these permissions contain all of the provided permissions.
	 * 
	 * @param perms permissions to test for
	 * @return true if all permissions are contained
	 */
	public boolean has(PosixFilePermission... permissions) {
		if (permissions.length == 0)
			throw new IllegalArgumentException("Must provide at least one permission.");
		for (var perm : permissions) {
			if (!perms.contains(perm))
				return false;
		}
		return true;
	}

	/**
	 * Get if this is an empty set of permissions.
	 * 
	 * @return empty
	 */
	public boolean isEmpty() {
		return perms.isEmpty();
	}

	@Override
	public int hashCode() {
		return Objects.hash(mode);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PosixPermissions other = (PosixPermissions) obj;
		return mode == other.mode;
	}

	@Override
	public String toString() {
		return "PosixPermissions [asFileModesString()=" + asFileModesString() + ", asMaskString()=" + asMaskString()
				+ "]";
	}
}
