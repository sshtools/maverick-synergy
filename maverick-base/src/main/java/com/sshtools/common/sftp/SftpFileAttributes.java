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


package com.sshtools.common.sftp;

import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

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
 */
public class SftpFileAttributes {

	
	public static final long SSH_FILEXFER_ATTR_SIZE 			= 0x00000001;
	public static final long SSH_FILEXFER_ATTR_UIDGID 			= 0x00000002;
	public static final long SSH_FILEXFER_ATTR_PERMISSIONS 		= 0x00000004;
	public static final long SSH_FILEXFER_ATTR_ACCESSTIME 		= 0x00000008;

	public static final long SSH_FILEXFER_ATTR_EXTENDED 		= 0x80000000;
	
	public static final long VERSION_3_FLAGS = SSH_FILEXFER_ATTR_SIZE 
			| SSH_FILEXFER_ATTR_UIDGID
			| SSH_FILEXFER_ATTR_PERMISSIONS
			| SSH_FILEXFER_ATTR_ACCESSTIME
			| SSH_FILEXFER_ATTR_EXTENDED;
	
	// Version 4 flags
	public static final long SSH_FILEXFER_ATTR_CREATETIME 		= 0x00000010;
	public static final long SSH_FILEXFER_ATTR_MODIFYTIME 		= 0x00000020;
	public static final long SSH_FILEXFER_ATTR_ACL 			    = 0x00000040;
	public static final long SSH_FILEXFER_ATTR_OWNERGROUP 		= 0x00000080;
	public static final long SSH_FILEXFER_ATTR_SUBSECOND_TIMES  = 0x00000100;
	
	public static final long VERSION_4_FLAGS = (VERSION_3_FLAGS ^ SSH_FILEXFER_ATTR_UIDGID)
			| SSH_FILEXFER_ATTR_CREATETIME
			| SSH_FILEXFER_ATTR_MODIFYTIME
			| SSH_FILEXFER_ATTR_ACL
			| SSH_FILEXFER_ATTR_OWNERGROUP
			| SSH_FILEXFER_ATTR_SUBSECOND_TIMES;
	
	// This is only used for version >= 5
	public static final long SSH_FILEXFER_ATTR_BITS			    = 0x00000200;
	
	
	public static final long VERSION_5_FLAGS = VERSION_4_FLAGS 
			| SSH_FILEXFER_ATTR_BITS;
	
	// These are version >= 6
	public static final long SSH_FILEXFER_ATTR_ALLOCATION_SIZE = 0x00000400;
	public static final long SSH_FILEXFER_ATTR_TEXT_HINT		= 0x00000800;
	public static final long SSH_FILEXFER_ATTR_MIME_TYPE	 	= 0x00001000;
	public static final long SSH_FILEXFER_ATTR_LINK_COUNT		= 0x00002000;
	public static final long SSH_FILEXFER_ATTR_UNTRANSLATED	= 0x00004000;
	public static final long SSH_FILEXFER_ATTR_CTIME	 		= 0x00008000;

	public static final long VERSION_6_FLAGS = VERSION_5_FLAGS 
			| SSH_FILEXFER_ATTR_ALLOCATION_SIZE
			| SSH_FILEXFER_ATTR_TEXT_HINT
			| SSH_FILEXFER_ATTR_MIME_TYPE
			| SSH_FILEXFER_ATTR_LINK_COUNT
			| SSH_FILEXFER_ATTR_UNTRANSLATED
			| SSH_FILEXFER_ATTR_CTIME;
	
	// Types
	public static final int SSH_FILEXFER_TYPE_REGULAR 		= 1;
	public static final int SSH_FILEXFER_TYPE_DIRECTORY 	= 2;
	public static final int SSH_FILEXFER_TYPE_SYMLINK 		= 3;
	public static final int SSH_FILEXFER_TYPE_SPECIAL 		= 4;
	public static final int SSH_FILEXFER_TYPE_UNKNOWN 		= 5;
	
	// This is only used for version >= 5
	public static final int SSH_FILEXFER_TYPE_SOCKET 		= 6;
	public static final int SSH_FILEXFER_TYPE_CHAR_DEVICE 	= 7;
	public static final int SSH_FILEXFER_TYPE_BLOCK_DEVICE 	= 8;
	public static final int SSH_FILEXFER_TYPE_FIFO 			= 9;
	
	// Attribute bits
	public static final int  SSH_FILEXFER_ATTR_FLAGS_READONLY         =	0x00000001;
    public static final int  SSH_FILEXFER_ATTR_FLAGS_SYSTEM           =	0x00000002;
    public static final int  SSH_FILEXFER_ATTR_FLAGS_HIDDEN           =	0x00000004;
    public static final int  SSH_FILEXFER_ATTR_FLAGS_CASE_INSENSITIVE =	0x00000008;
    public static final int  SSH_FILEXFER_ATTR_FLAGS_ARCHIVE          =	0x00000010;
    public static final int  SSH_FILEXFER_ATTR_FLAGS_ENCRYPTED        =	0x00000020;
    public static final int  SSH_FILEXFER_ATTR_FLAGS_COMPRESSED       =	0x00000040;
    public static final int  SSH_FILEXFER_ATTR_FLAGS_SPARSE           =	0x00000080;
    public static final int  SSH_FILEXFER_ATTR_FLAGS_APPEND_ONLY      = 0x00000100;
    public static final int  SSH_FILEXFER_ATTR_FLAGS_IMMUTABLE        =	0x00000200;
    public static final int  SSH_FILEXFER_ATTR_FLAGS_SYNC             =	0x00000400;
    public static final int  SSH_FILEXFER_ATTR_FLAGS_TRANSLATION_ERR  =	0x00000800;
    
	// ACL Flags 
    public static final int SFX_ACL_CONTROL_INCLUDED 				  = 0x00000001;
    public static final int SFX_ACL_CONTROL_PRESENT 				  = 0x00000002;
    public static final int SFX_ACL_CONTROL_INHERITED 				  = 0x00000004;
    public static final int SFX_ACL_AUDIT_ALARM_INCLUDED			  = 0x00000010;
    public static final int SFX_ACL_AUDIT_ALARM_INHERITED			  = 0x00000020;

    // Text Hint
    public static final int SSH_FILEXFER_ATTR_KNOWN_TEXT			  = 0x00;
    public static final int SSH_FILEXFER_ATTR_GUESSED_TEXT			  = 0x01;
    public static final int SSH_FILEXFER_ATTR_KNOWN_BINARY			  = 0x02;
    public static final int SSH_FILEXFER_ATTR_GUESSED_BINARY		  = 0x00;
    
	/**
	 * Format mask constant can be used to mask off a file
	 * type from the mode.
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
	 * Permissions flag: Bit to determine whether a file is executed as the
	 * owner
	 */
	public final static int S_ISUID = 0x800;

	/**
	 * Permissions flag: Bit to determine whether a file is executed as the
	 * group owner
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
	 * Permissions flag: Permits a file's group to execute the file or to search
	 * the file's directory.
	 */
	public final static int S_IXGRP = 0x08;

	/** Permissions flag: Permits others to read the file. */
	public final static int S_IROTH = 0x04;

	/** Permissions flag: Permits others to write to the file. */
	public final static int S_IWOTH = 0x02;

	/**
	 * Permissions flag: Permits others to execute the file or to search the
	 * file's directory.
	 */
	public final static int S_IXOTH = 0x01;

	long flags = 0x0000000;
	int type; // Version 4+
	UnsignedInteger64 size = null;
	UnsignedInteger64 allocationSize = null;
	String uid = null;
	String gid = null;
	UnsignedInteger32 permissions = null;
	UnsignedInteger64 atime = null;
	UnsignedInteger32 atime_nano = null; // Version 4+
	UnsignedInteger64 createtime = null;
	UnsignedInteger32 createtime_nano = null; // Version 4+
	UnsignedInteger64 mtime = null;
	UnsignedInteger32 mtime_nano = null;  // Version 4+
	UnsignedInteger64 ctime = null;		  // Version 6+
	UnsignedInteger32 ctime_nano = null;  // Version 6+
	UnsignedInteger32 attributeBits; 	  // Version 5+
	UnsignedInteger32 attributeBitsValid; // Version 6+
	byte textHint;
	String mimeType;
	UnsignedInteger32 linkCount;
	String untralsatedName;
	
	UnsignedInteger32 aclFlags = null;
	private Vector<ACL> acls = new Vector<ACL>();
	private Map<String, byte[]> extendedAttributes = new HashMap<String, byte[]>();
	
	String username;
	String group;
	
	char[] types = { 'p', 'c', 'd', 'b', '-', 'l', 's', };
	
	String  charsetEncoding;
	Long supportedAttributeMask;
	Long supportedAttributeBits;
	
	/**
	 * Creates a new FileAttributes object.
	 */
	public SftpFileAttributes(int type, 
			String charsetEncoding, 
			long supportedAttributeBits,
			long supportedAttributeMask) {
		this.supportedAttributeBits = supportedAttributeBits;
		this.supportedAttributeMask = supportedAttributeMask;
		this.charsetEncoding = charsetEncoding;
		this.type = type;
	}

	public SftpFileAttributes(int type, 
			String charsetEncoding) {
		this(type, charsetEncoding, 0L, 0L);
	}
	
	public int getType() {
		return type;
	}

	/**
	 * @param sftp
	 * @param bar
	 * @throws IOException
	 */
	public SftpFileAttributes(ByteArrayReader bar, int version, String charsetEncoding)
			throws IOException {
		this(bar, version, charsetEncoding, 0L, 0L);
	}
	
	public SftpFileAttributes(ByteArrayReader bar, int version, String charsetEncoding, 
			long supportedAttributeBits,
			long supportedAttributeMask)
			throws IOException {
		this.supportedAttributeBits = supportedAttributeBits;
		this.supportedAttributeMask = supportedAttributeMask;
		this.charsetEncoding = charsetEncoding;
		
		if (bar.available() >= 4)
			flags = bar.readInt();

		// Work out the type from the permissions field later if we're not using
		// version
		// 4 of the protocol
		if (version > 3) {
			// Get the type if were using version 4+ of the protocol
			if (bar.available() > 0)
				type = bar.read();
		}

		// if ATTR_SIZE flag is set then read size
		if (isFlagSet(SSH_FILEXFER_ATTR_SIZE, version) && bar.available() >= 8) {
			byte[] raw = new byte[8];
			bar.read(raw);
			size = new UnsignedInteger64(raw);
		}

		if (isFlagSet(SSH_FILEXFER_ATTR_ALLOCATION_SIZE, version) && bar.available() >= 8) {
			byte[] raw = new byte[8];
			bar.read(raw);
			allocationSize = new UnsignedInteger64(raw);
		}

		if (version <= 3 && isFlagSet(SSH_FILEXFER_ATTR_UIDGID, version)
				&& bar.available() >= 8) {

			uid = String.valueOf(bar.readInt());
			gid = String.valueOf(bar.readInt());
		} else if (version > 3 && isFlagSet(SSH_FILEXFER_ATTR_OWNERGROUP, version)
				&& bar.available() > 0) {
			uid = bar.readString(charsetEncoding);
			gid = bar.readString(charsetEncoding);
		}

		if (isFlagSet(SSH_FILEXFER_ATTR_PERMISSIONS, version) && bar.available() >= 4) {
			permissions = new UnsignedInteger32(bar.readInt());
			if(version <=3) {
				if((permissions.longValue() & S_IFREG) == S_IFREG) {
					type = SSH_FILEXFER_TYPE_REGULAR;
				} else if((permissions.longValue() & S_IFLNK) == S_IFLNK) {
					type = SSH_FILEXFER_TYPE_SYMLINK;
				} else if((permissions.longValue() & S_IFCHR) == S_IFCHR) {
					type = SSH_FILEXFER_TYPE_CHAR_DEVICE;
				} else if((permissions.longValue() & S_IFBLK) == S_IFBLK) {
					type = SSH_FILEXFER_TYPE_BLOCK_DEVICE;
				} else if((permissions.longValue() & S_IFDIR) == S_IFDIR) {
					type = SSH_FILEXFER_TYPE_DIRECTORY;
				} else if((permissions.longValue() & S_IFIFO) == S_IFIFO) {
					type = SSH_FILEXFER_TYPE_FIFO;
				} else if((permissions.longValue() & S_IFSOCK) == S_IFSOCK) {
					type = SSH_FILEXFER_TYPE_SOCKET;
				} else if((permissions.longValue() & S_IFMT) == S_IFMT) {
					type = SSH_FILEXFER_TYPE_SPECIAL;
				} else  {
					type = SSH_FILEXFER_TYPE_UNKNOWN;
				}
			}
		}
		
		if(type==0) {
			type = SSH_FILEXFER_TYPE_UNKNOWN;
		}

		if (version <= 3 && isFlagSet(SSH_FILEXFER_ATTR_ACCESSTIME, version)
				&& bar.available() >= 8) {
			atime = new UnsignedInteger64(bar.readInt());
			mtime = new UnsignedInteger64(bar.readInt());
		} else if (version > 3 && bar.available() > 0) {
			if (isFlagSet(SSH_FILEXFER_ATTR_ACCESSTIME, version) && bar.available() >= 8) {
				atime = bar.readUINT64();
				if (isFlagSet(SSH_FILEXFER_ATTR_SUBSECOND_TIMES, version)
						&& bar.available() >= 4) {
					atime_nano = bar.readUINT32();
				}
			}
		}

		if (version > 3 && bar.available() > 0) {
			if (isFlagSet(SSH_FILEXFER_ATTR_CREATETIME, version) && bar.available() >= 8) {
				createtime = bar.readUINT64();
				if (isFlagSet(SSH_FILEXFER_ATTR_SUBSECOND_TIMES, version)
						&& bar.available() >= 4)
					createtime_nano = bar.readUINT32();
			}
		}

		if (version > 3 && bar.available() > 0) {
			if (isFlagSet(SSH_FILEXFER_ATTR_MODIFYTIME, version) && bar.available() >= 8) {
				mtime = bar.readUINT64();
				if (isFlagSet(SSH_FILEXFER_ATTR_SUBSECOND_TIMES, version)
						&& bar.available() >= 4)
					mtime_nano = bar.readUINT32();
			}
		}

		if (version >= 6 && bar.available() > 0) {
			if (isFlagSet(SSH_FILEXFER_ATTR_CTIME, version) && bar.available() >= 8) {
				ctime = bar.readUINT64();
				if (isFlagSet(SSH_FILEXFER_ATTR_SUBSECOND_TIMES, version)
						&& bar.available() >= 4)
					ctime_nano = bar.readUINT32();
			}
		}
		// We are currently ignoring ACL and extended attributes
		if (version > 3 && isFlagSet(SSH_FILEXFER_ATTR_ACL, version)
				&& bar.available() >= 4) {

			if(version >= 6 && bar.available() >= 4) {
				aclFlags = bar.readUINT32();
			}
			
			int length = (int) bar.readInt();

			if (length > 0 && bar.available() >= length) {
				int count = (int) bar.readInt();
				for (int i = 0; i < count; i++) {
					acls.addElement(new ACL((int) bar.readInt(), (int) bar
							.readInt(), (int) bar.readInt(), bar.readString()));
				}
			}
		}

		if (version >= 5 && isFlagSet(SSH_FILEXFER_ATTR_BITS, version) && bar.available() >= 4) {
			attributeBits = bar.readUINT32();
		}

		if (version >= 6) {
				
			if(isFlagSet(SSH_FILEXFER_ATTR_BITS, version) && bar.available() >= 4) {
				attributeBitsValid = bar.readUINT32();
			}
		
			if(isFlagSet(SSH_FILEXFER_ATTR_TEXT_HINT, version) && bar.available() >= 1) {
				textHint = (byte) bar.read();
			}
			if(isFlagSet(SSH_FILEXFER_ATTR_MIME_TYPE, version) && bar.available() >= 4) {
				mimeType = bar.readString();
			}
			
			if(isFlagSet(SSH_FILEXFER_ATTR_LINK_COUNT, version) && bar.available() >= 4) {
				linkCount = bar.readUINT32();
			}
			
			if(isFlagSet(SSH_FILEXFER_ATTR_UNTRANSLATED, version) && bar.available() >= 4) {
				untralsatedName = bar.readString();
			}
		}
		
		if (version >= 3 && isFlagSet(SSH_FILEXFER_ATTR_EXTENDED, version)
				&& bar.available() >= 4) {
			int count = (int) bar.readInt();
			// read each extended attribute
			for (int i = 0; i < count; i++) {
				extendedAttributes
						.put(bar.readString(), bar.readBinaryString());
			}
		}
	}

	/**
	 * Get the UID of the owner.
	 * 
	 * @return String
	 */
	public String getUID() {
		if (username != null) {
			return username;
		}
		if (uid != null) {
			return uid;
		}
		return "";
	}

	/**
	 * Set the UID of the owner.
	 * 
	 * @param uid
	 */
	public void setUID(String uid) {
		if(uid==null) {
			throw new IllegalArgumentException("uid cannot be null!");
		}
		if(!uid.matches("\\d+")) {
			throw new IllegalArgumentException("uid must be a user id containing only digits");
		}
		flags |= SSH_FILEXFER_ATTR_OWNERGROUP;
		flags |= SSH_FILEXFER_ATTR_UIDGID;
		this.uid = uid;
	}

	/**
	 * Set the GID of this file.
	 * 
	 * @param gid
	 */
	public void setGID(String gid) {
		if(gid==null) {
			throw new IllegalArgumentException("gid cannot be null!");
		}
		if(!gid.matches("\\d+")) {
			throw new IllegalArgumentException("gid must be a group id containing only digits");
		}
		flags |= SSH_FILEXFER_ATTR_OWNERGROUP;
		flags |= SSH_FILEXFER_ATTR_UIDGID;
		this.gid = gid;
	}

	/**
	 * Get the GID of this file.
	 * 
	 * @return String
	 */
	public String getGID() {
		if (group != null) {
			return group;
		}
		if (gid != null) {
			return gid;
		}
		return "";
	}

	public boolean hasUID() {
		return username!=null || uid != null;
	}

	public boolean hasGID() {
		return group != null || gid != null;
	}

	/**
	 * Set the size of the file.
	 * 
	 * @param size
	 */
	public void setSize(UnsignedInteger64 size) {
		this.size = size;

		// Set the flag
		if (size != null) {
			flags |= SSH_FILEXFER_ATTR_SIZE;
		} else {
			flags ^= SSH_FILEXFER_ATTR_SIZE;
		}
	}

	/**
	 * 
	 * Get the size of the file.
	 * 
	 * @return UnsignedInteger64
	 */
	public UnsignedInteger64 getSize() {
		if (size != null) {
			return size;
		}
		return new UnsignedInteger64("0");
	}

	public boolean hasSize() {
		return size != null;
	}

	/**
	 * Set the permissions of the file. This value should be a valid mask of the
	 * permissions flags defined within this class.
	 */
	public void setPermissions(UnsignedInteger32 permissions) {
		this.permissions = permissions;
		
		if(permissions!=null) {
			if(type == 0) {
				if((permissions.longValue() & SftpFileAttributes.S_IFDIR) == SftpFileAttributes.S_IFDIR) {
					this.type = SSH_FILEXFER_TYPE_DIRECTORY;
				} else if((permissions.longValue() & SftpFileAttributes.S_IFREG) == SftpFileAttributes.S_IFREG) {
					this.type = SSH_FILEXFER_TYPE_REGULAR;
				} else if((permissions.longValue() & SftpFileAttributes.S_IFCHR) == SftpFileAttributes.S_IFCHR) {
					this.type = SSH_FILEXFER_TYPE_SPECIAL;
				} else if((permissions.longValue() & SftpFileAttributes.S_IFBLK) == SftpFileAttributes.S_IFBLK) {
					this.type = SSH_FILEXFER_TYPE_SPECIAL;
				} else if((permissions.longValue() & SftpFileAttributes.S_IFIFO) == SftpFileAttributes.S_IFIFO) {
					this.type = SSH_FILEXFER_TYPE_SPECIAL;
				} else if((permissions.longValue() & SftpFileAttributes.S_IFMT) == SftpFileAttributes.S_IFMT) {
					this.type = SSH_FILEXFER_TYPE_SPECIAL;
				} else if((permissions.longValue() & SftpFileAttributes.S_IFSOCK) == SftpFileAttributes.S_IFSOCK) {
					this.type = SSH_FILEXFER_TYPE_SPECIAL;
				} else if((permissions.longValue() & SftpFileAttributes.S_IFLNK) == SftpFileAttributes.S_IFLNK) {
					this.type = SSH_FILEXFER_TYPE_SYMLINK;
				} else {
					this.type = SSH_FILEXFER_TYPE_UNKNOWN;
				}
			}

			flags |= SSH_FILEXFER_ATTR_PERMISSIONS;	
		
		} else {
			if((flags & SSH_FILEXFER_ATTR_PERMISSIONS) == SSH_FILEXFER_ATTR_PERMISSIONS) {
				flags ^= SSH_FILEXFER_ATTR_PERMISSIONS;
			}
		}
	}

	/**
	 * Set permissions given a UNIX style mask, for example '0644'
	 * 
	 * @param mask
	 *            mask
	 * 
	 * @throws IllegalArgumentException
	 *             if badly formatted string
	 */
	public void setPermissionsFromMaskString(String mask) {
		if (mask.length() != 4) {
			throw new IllegalArgumentException("Mask length must be 4");
		}

		try {
			setPermissions(new UnsignedInteger32(String.valueOf(Integer
					.parseInt(mask, 8))));
		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException(
					"Mask must be 4 digit octal number.");
		}
	}

	/**
	 * Set the permissions given a UNIX style umask, for example '0022' will
	 * result in 0022 ^ 0777.
	 * 
	 * @param umask
	 * @throws IllegalArgumentException
	 *             if badly formatted string
	 */
	public void setPermissionsFromUmaskString(String umask) {
		if (umask.length() != 4) {
			throw new IllegalArgumentException("umask length must be 4");
		}

		try {
			setPermissions(new UnsignedInteger32(String.valueOf(Integer
					.parseInt(umask, 8) ^ 0777)));
		} catch (NumberFormatException ex) {
			throw new IllegalArgumentException(
					"umask must be 4 digit octal number");
		}
	}

	/**
	 * Set the permissions from a string in the format "rwxr-xr-x"
	 * 
	 * @param newPermissions
	 */
	public void setPermissions(String newPermissions) {
		int cp = getModeType();

		if (permissions != null) {
			cp = cp
					| (((permissions.longValue() & S_IFMT) == S_IFMT) ? S_IFMT
							: 0);
			cp = cp
					| (((permissions.longValue() & S_IFSOCK) == S_IFSOCK) ? S_IFSOCK
							: 0);
			cp = cp
					| (((permissions.longValue() & S_IFLNK) == S_IFLNK) ? S_IFLNK
							: 0);
			cp = cp
					| (((permissions.longValue() & S_IFREG) == S_IFREG) ? S_IFREG
							: 0);
			cp = cp
					| (((permissions.longValue() & S_IFBLK) == S_IFBLK) ? S_IFBLK
							: 0);
			cp = cp
					| (((permissions.longValue() & S_IFDIR) == S_IFDIR) ? S_IFDIR
							: 0);
			cp = cp
					| (((permissions.longValue() & S_IFCHR) == S_IFCHR) ? S_IFCHR
							: 0);
			cp = cp
					| (((permissions.longValue() & S_IFIFO) == S_IFIFO) ? S_IFIFO
							: 0);
			cp = cp
					| (((permissions.longValue() & S_ISUID) == S_ISUID) ? S_ISUID
							: 0);
			cp = cp
					| (((permissions.longValue() & S_ISGID) == S_ISGID) ? S_ISGID
							: 0);
		}

		int len = newPermissions.length();

		if (len >= 1) {
			cp = cp
					| ((newPermissions.charAt(0) == 'r') ? SftpFileAttributes.S_IRUSR
							: 0);
		}

		if (len >= 2) {
			cp = cp
					| ((newPermissions.charAt(1) == 'w') ? SftpFileAttributes.S_IWUSR
							: 0);
		}

		if (len >= 3) {
			cp = cp
					| ((newPermissions.charAt(2) == 'x') ? SftpFileAttributes.S_IXUSR
							: 0);
		}

		if (len >= 4) {
			cp = cp
					| ((newPermissions.charAt(3) == 'r') ? SftpFileAttributes.S_IRGRP
							: 0);
		}

		if (len >= 5) {
			cp = cp
					| ((newPermissions.charAt(4) == 'w') ? SftpFileAttributes.S_IWGRP
							: 0);
		}

		if (len >= 6) {
			cp = cp
					| ((newPermissions.charAt(5) == 'x') ? SftpFileAttributes.S_IXGRP
							: 0);
		}

		if (len >= 7) {
			cp = cp
					| ((newPermissions.charAt(6) == 'r') ? SftpFileAttributes.S_IROTH
							: 0);
		}

		if (len >= 8) {
			cp = cp
					| ((newPermissions.charAt(7) == 'w') ? SftpFileAttributes.S_IWOTH
							: 0);
		}

		if (len >= 9) {
			cp = cp
					| ((newPermissions.charAt(8) == 'x') ? SftpFileAttributes.S_IXOTH
							: 0);
		}

		setPermissions(new UnsignedInteger32(cp));
	}

	/**
	 * Get the current permissions value.
	 * 
	 * @return UnsignedInteger32
	 */
	public UnsignedInteger32 getPermissions() {
		if (permissions != null)
			return permissions;
		return new UnsignedInteger32(0);
	}

	/**
	 * Set the last access and last modified times. These times are represented
	 * by integers containing the number of seconds from Jan 1, 1970 UTC. NOTE:
	 * You should divide any value returned from Java's
	 * System.currentTimeMillis() method by 1000 to set the correct times as
	 * this returns the time in milliseconds from Jan 1, 1970 UTC.
	 * 
	 * @param atime
	 * @param mtime
	 */
	public void setTimes(UnsignedInteger64 atime, UnsignedInteger64 mtime) {
		this.atime = atime;
		this.mtime = mtime;

		// Set the flags
		if (atime != null) {
			flags |= SSH_FILEXFER_ATTR_ACCESSTIME;
		} else {
			if(isFlagSet(SSH_FILEXFER_ATTR_ACCESSTIME)) {
				flags ^= SSH_FILEXFER_ATTR_ACCESSTIME;
			}
		}

		if (mtime != null) {
			flags |= SSH_FILEXFER_ATTR_MODIFYTIME;
		} else {
			if(isFlagSet(SSH_FILEXFER_ATTR_MODIFYTIME)) {
				flags ^= SSH_FILEXFER_ATTR_MODIFYTIME;
			}
		}
	}
	
	/**
	 * Sets SFTP v4 time attributes including sub-second times. If you pass a null value
	 * for any sub-second time it will be defaulted to zero. If you pass null value for 
	 * any time value if will be not be included in the attributes and its sub-second
	 * value will also not be included. 
	 * @param atime
	 * @param atime_nano
	 * @param mtime
	 * @param mtime_nano
	 * @param ctime
	 * @param ctime_nano
	 */
	public void setTimes(UnsignedInteger64 atime, UnsignedInteger32 atime_nano,
			UnsignedInteger64 mtime, UnsignedInteger32 mtime_nano,
			UnsignedInteger64 ctime, UnsignedInteger32 ctime_nano) {
		
		setTimes(atime, mtime);
		
		flags |= SSH_FILEXFER_ATTR_SUBSECOND_TIMES;
		
		this.atime_nano = atime_nano != null ? atime_nano : new UnsignedInteger32(0);
		this.mtime_nano = mtime_nano != null ? mtime_nano : new UnsignedInteger32(0);
		this.createtime_nano = ctime_nano != null ? ctime_nano : new UnsignedInteger32(0);
		
		this.createtime = ctime;
	
		if(ctime != null) {
			flags |= SSH_FILEXFER_ATTR_CREATETIME;
		} else {
			if(isFlagSet(SSH_FILEXFER_ATTR_CREATETIME)) {
				flags ^= SSH_FILEXFER_ATTR_CREATETIME;
			}
		}
	}
	
	/**
	 * Set SFTP v4 time attributes without any sub-second times. 
	 * @param atime last accessed time
	 * @param mtime last modified time
	 * @param ctime creation time
	 */
	public void setTimes(UnsignedInteger64 atime, 
			UnsignedInteger64 mtime, 
			UnsignedInteger64 ctime) {
		
		setTimes(atime, mtime);
		
		if(isFlagSet(SSH_FILEXFER_ATTR_SUBSECOND_TIMES)) {
			flags ^= SSH_FILEXFER_ATTR_SUBSECOND_TIMES;
		}
		
		this.atime_nano = null;
		this.mtime_nano = null;
		this.createtime_nano = null;
		
		this.createtime = ctime;
	
		if(ctime != null) {
			flags |= SSH_FILEXFER_ATTR_CREATETIME;
		} else {
			if(isFlagSet(SSH_FILEXFER_ATTR_CREATETIME)) {
				flags ^= SSH_FILEXFER_ATTR_CREATETIME;
			}
		}
	}

	public boolean hasAccessTime() {
		return atime!=null;
	}
	
	public boolean hasCreateTime() {
		return createtime!=null;
	}
	
	/**
	 * Get the last accessed time. This integer value represents the number of
	 * seconds from Jan 1, 1970 UTC. When using with Java Date/Time classes you
	 * should multiply this value by 1000 as Java uses the time in milliseconds
	 * rather than seconds.
	 * 
	 * @return UnsignedInteger64
	 */
	public UnsignedInteger64 getAccessedTime() {
		return atime;
	}

	
	public boolean hasModifiedTime() {
		return mtime!=null;
	}
	
	/**
	 * Get the last modified time. This integer value represents the number of
	 * seconds from Jan 1, 1970 UTC. When using with Java Date/Time classes you
	 * should multiply this value by 1000 as Java uses the time in milliseconds
	 * rather than seconds.
	 * 
	 * @return UnsignedInteger64
	 */
	public UnsignedInteger64 getModifiedTime() {
		if (mtime != null) {
			return mtime;
		}
		return new UnsignedInteger64(0);
	}

	/**
	 * Returns the modified date/time as a Java Date object.
	 * 
	 * @return
	 */
	public Date getModifiedDateTime() {

		long time = 0;

		if (mtime != null) {
			time = mtime.longValue() * 1000;
		}

		if (mtime_nano != null) {
			time += (mtime_nano.longValue() / 1000000);
		}
		return new Date(time);
	}
	
	/**
	 * Returns the creation date/time as a Java Date object.
	 * 
	 * @return
	 */
	public Date getCreationDateTime() {

		long time = 0;

		if (createtime != null) {
			time = createtime.longValue() * 1000;
		}

		if (createtime_nano != null) {
			time += (createtime_nano.longValue() / 1000000);
		}
		return new Date(time);
	}

	/**
	 * Returns the last accessed date/time as a Java Date object.
	 * 
	 * @return
	 */
	public Date getAccessedDateTime() {

		long time = 0;

		if (atime != null) {
			time = atime.longValue() * 1000;
		}

		if (atime_nano != null) {
			time += (atime_nano.longValue() / 1000000);
		}
		return new Date(time);
	}

	/**
	 * Get the creation time of this file. This is only supported for SFTP
	 * protocol version 4 and above; if called when protocol revision is lower
	 * this method will return a zero value.
	 * 
	 * @return UnsignedInteger64
	 */
	public UnsignedInteger64 getCreationTime() {
		if (createtime != null)
			return createtime;
		return new UnsignedInteger64(0);
	}

	/**
	 * Determine if a permissions flag is set.
	 * 
	 * @param flag
	 * 
	 * @return boolean
	 */
	private boolean isFlagSet(long flag, int version) {
		if(version >= 5 && supportedAttributeMask != null && supportedAttributeMask.longValue()!=0) {
			boolean set = ((flags & (flag & 0xFFFFFFFFL)) == (flag & 0xFFFFFFFFL));
			if(set) {
				set =  ((supportedAttributeMask & (flag & 0xFFFFFFFFL)) == (flag & 0xFFFFFFFFL));
			}
			return set;
		}
		return ((flags & (flag & 0xFFFFFFFFL)) == (flag & 0xFFFFFFFFL));
	}
	
	private boolean isFlagSet(long flag) {
		return ((flags & (flag & 0xFFFFFFFFL)) == (flag & 0xFFFFFFFFL));
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
		ByteArrayWriter baw = new ByteArrayWriter();

		try {
			switch(version) {
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
				baw.write(size.toByteArray());
			}

			if (version <= 3 && isFlagSet(SSH_FILEXFER_ATTR_UIDGID, version)) {
				if (uid != null) {
					try {
						baw.writeInt(Long.parseLong(uid));
					} catch (NumberFormatException ex) {
						baw.writeInt(0);
					}
				} else {
					baw.writeInt(0);
				}

				if (gid != null) {
					try {
						baw.writeInt(Long.parseLong(gid));
					} catch (NumberFormatException ex) {
						baw.writeInt(0);
					}
				} else {
					baw.writeInt(0);
				}
			} else if (version > 3 && isFlagSet(SSH_FILEXFER_ATTR_OWNERGROUP, version)) {
				if (username != null)
					baw.writeString(username, charsetEncoding);
				else if(uid!=null) 
					baw.writeString(uid, charsetEncoding);
				else
					baw.writeString("");

				if (group != null)
					baw.writeString(username, charsetEncoding);
				else if(gid!=null)
					baw.writeString(gid, charsetEncoding);
				else
					baw.writeString("");
			}

			if (isFlagSet(SSH_FILEXFER_ATTR_PERMISSIONS, version)) {
				baw.writeInt((permissions.longValue() & S_MODE_MASK) | getModeType());
			}

			if (version <= 3 && isFlagSet(SSH_FILEXFER_ATTR_ACCESSTIME, version)) {
				baw.writeInt(atime.longValue());
				baw.writeInt(mtime.longValue());
			} else if (version > 3) {

				if (isFlagSet(SSH_FILEXFER_ATTR_ACCESSTIME, version)) {
					baw.writeUINT64(atime);
					if (isFlagSet(SSH_FILEXFER_ATTR_SUBSECOND_TIMES, version)) {
						baw.writeUINT32(atime_nano);
					}
				}

				if (isFlagSet(SSH_FILEXFER_ATTR_CREATETIME, version)) {
					baw.writeUINT64(createtime);
					if (isFlagSet(SSH_FILEXFER_ATTR_SUBSECOND_TIMES, version)) {
						baw.writeUINT32(createtime_nano);
					}
				}

				if (isFlagSet(SSH_FILEXFER_ATTR_MODIFYTIME, version)) {
					baw.writeUINT64(mtime);
					if (isFlagSet(SSH_FILEXFER_ATTR_SUBSECOND_TIMES, version)) {
						baw.writeUINT32(mtime_nano);
					}
				}

			}

			if (isFlagSet(SSH_FILEXFER_ATTR_ACL, version)) {
				ByteArrayWriter tmp = new ByteArrayWriter();

				try {
					Enumeration<ACL> e = acls.elements();
					tmp.writeInt(acls.size());
					while (e.hasMoreElements()) {
						ACL acl = e.nextElement();
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

			if(version >= 5 && isFlagSet(SSH_FILEXFER_ATTR_BITS, version)) {
				if(attributeBits==null) {
					baw.writeInt(0);
				} else {
					if(supportedAttributeBits==null) {
						baw.writeInt(attributeBits.longValue());
					}else {
						baw.writeInt(attributeBits.longValue() & supportedAttributeBits.longValue());	
					}
					
				}
			}
			
			if (isFlagSet(SSH_FILEXFER_ATTR_EXTENDED, version)) {
				baw.writeInt(extendedAttributes.size());
				for(String key : extendedAttributes.keySet()) {
					baw.writeString(key);
					baw.writeBinaryString((byte[]) extendedAttributes.get(key));
				}
			}

			return baw.toByteArray();

		} finally {
			baw.close();
		}
	}

	public int getModeType() {
		
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

	private int octal(int v, int r) {
		v >>>= r;

		return (((v & 0x04) != 0) ? 4 : 0) + (((v & 0x02) != 0) ? 2 : 0)
				+ +(((v & 0x01) != 0) ? 1 : 0);
	}

	private String rwxString(int v, int r) {
		v >>>= r;

		String rwx = ((((v & 0x04) != 0) ? "r" : "-") + (((v & 0x02) != 0) ? "w"
				: "-"));

		if (((r == 6) && ((permissions.longValue() & S_ISUID) == S_ISUID))
				|| ((r == 3) && ((permissions.longValue() & S_ISGID) == S_ISGID))) {
			rwx += (((v & 0x01) != 0) ? "s" : "S");
		} else {
			rwx += (((v & 0x01) != 0) ? "x" : "-");
		}

		return rwx;
	}

	/**
	 * 
	 * Returns a formatted permissions string.
	 * 
	 * @return String
	 */
	public String getPermissionsString() {
		if (permissions != null) {
			StringBuffer str = new StringBuffer();
			boolean has_ifmt = ((int) permissions.longValue() & S_IFMT) > 0;
			if (has_ifmt) {
				str.append(types[(int) (permissions.longValue() & S_IFMT) >>> 13]);
			} else {
				switch(type) {
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
				
			}

			str.append(rwxString((int) permissions.longValue(), 6));
			str.append(rwxString((int) permissions.longValue(), 3));
			str.append(rwxString((int) permissions.longValue(), 0));

			return str.toString();
		}
		return "";
	}

	/**
	 * Return the UNIX style mode mask
	 * 
	 * @return mask
	 */
	public String getMaskString() {
		StringBuffer buf = new StringBuffer();

		if (permissions != null) {
			int i = (int) permissions.longValue();
			buf.append('0');
			buf.append(octal(i, 6));
			buf.append(octal(i, 3));
			buf.append(octal(i, 0));
		} else {
			buf.append("----");
		}
		return buf.toString();
	}

	/**
	 * Determine whether these attributes refer to a directory
	 * 
	 * @return boolean
	 */
	public boolean isDirectory() {
		return type == SSH_FILEXFER_TYPE_DIRECTORY;
	}

	/**
	 * 
	 * Determine whether these attributes refer to a file.
	 * 
	 * @return boolean
	 */
	public boolean isFile() {
		return type == SSH_FILEXFER_TYPE_REGULAR;
	}

	/**
	 * Determine whether these attributes refer to a symbolic link.
	 * 
	 * @return boolean
	 */
	public boolean isLink() {
		return type == SSH_FILEXFER_TYPE_SYMLINK;
	}

	/**
	 * Determine whether these attributes refer to a pipe.
	 * 
	 * @return boolean
	 */
	public boolean isFifo() {
		return type == SSH_FILEXFER_TYPE_FIFO;
	}

	/**
	 * Determine whether these attributes refer to a block special file.
	 * 
	 * @return boolean
	 */
	public boolean isBlock() {
		return type == SSH_FILEXFER_TYPE_BLOCK_DEVICE;
	}

	/**
	 * Determine whether these attributes refer to a character device.
	 * 
	 * @return boolean
	 */
	public boolean isCharacter() {
		return type == SSH_FILEXFER_TYPE_CHAR_DEVICE;
	}

	/**
	 * Determine whether these attributes refer to a socket.
	 * 
	 * @return boolean
	 */
	public boolean isSocket() {
		return type == SSH_FILEXFER_TYPE_SOCKET;
	}

	public void setUsername(String username) {
		flags |= SSH_FILEXFER_ATTR_OWNERGROUP;
		this.username = username;
	}

	public void setGroup(String group) {
		flags |= SSH_FILEXFER_ATTR_OWNERGROUP;
		this.group = group;
	}
	
	public boolean hasAttributeBits() {
		return attributeBits!=null;
	}
	
	private void setAttributeBit(long attributeBit, boolean value) throws SftpStatusException {
		
		if(!hasAttributeBits()) {
			attributeBits = new UnsignedInteger32(0);
		}
		
		flags = flags | SSH_FILEXFER_ATTR_BITS;
		
		if(value) {
			attributeBits = new UnsignedInteger32(attributeBits.longValue() | attributeBit);
		} else {
			if((attributeBits.longValue() & attributeBit) == attributeBit) {
				attributeBits = new UnsignedInteger32(attributeBits.longValue() ^ attributeBit);
			}
		}
		
	}
	
	public boolean isAttributeBitSet(long attributeBit) throws SftpStatusException {
		if(!hasAttributeBits()) {
			return false;
		}
		return ((attributeBits.longValue() & (attributeBit & 0xFFFFFFFFL)) == (attributeBit & 0xFFFFFFFFL));
	}
	
	public boolean isReadOnly() throws SftpStatusException {
		return isAttributeBitSet(SSH_FILEXFER_ATTR_FLAGS_READONLY);
	}
	
	public void setReadOnly(boolean value) throws SftpStatusException {
		setAttributeBit(SSH_FILEXFER_ATTR_FLAGS_READONLY, value);
	}
	
	public boolean isSystem() throws SftpStatusException {
		return isAttributeBitSet(SSH_FILEXFER_ATTR_FLAGS_SYSTEM);
	}

	public void setSystem(boolean value) throws SftpStatusException {
		setAttributeBit(SSH_FILEXFER_ATTR_FLAGS_SYSTEM, value);
	}
	
	public boolean isHidden() throws SftpStatusException {
		return isAttributeBitSet(SSH_FILEXFER_ATTR_FLAGS_HIDDEN);
	}
	
	public void setHidden(boolean value) throws SftpStatusException {
		setAttributeBit(SSH_FILEXFER_ATTR_FLAGS_HIDDEN, value);
	}
	
	public boolean isCaseInsensitive() throws SftpStatusException {
		return isAttributeBitSet(SSH_FILEXFER_ATTR_FLAGS_CASE_INSENSITIVE);
	}
	
	public void setCaseSensitive(boolean value) throws SftpStatusException {
		setAttributeBit(SSH_FILEXFER_ATTR_FLAGS_CASE_INSENSITIVE, value);
	}
	
	public boolean isArchive() throws SftpStatusException {
		return isAttributeBitSet(SSH_FILEXFER_ATTR_FLAGS_ARCHIVE);
	}
	
	public void setArchive(boolean value) throws SftpStatusException {
		setAttributeBit(SSH_FILEXFER_ATTR_FLAGS_ARCHIVE, value);
	}
	
	public boolean isEncrypted() throws SftpStatusException {
		return isAttributeBitSet(SSH_FILEXFER_ATTR_FLAGS_ENCRYPTED);
	}
	
	public void setEncrypted(boolean value) throws SftpStatusException {
		setAttributeBit(SSH_FILEXFER_ATTR_FLAGS_ENCRYPTED, value);
	}
	
	public boolean isCompressed() throws SftpStatusException {
		return isAttributeBitSet(SSH_FILEXFER_ATTR_FLAGS_COMPRESSED);
	}
	
	public void setCompressed(boolean value) throws SftpStatusException {
		setAttributeBit(SSH_FILEXFER_ATTR_FLAGS_COMPRESSED, value);
	}
	
	public boolean isSparse() throws SftpStatusException {
		return isAttributeBitSet(SSH_FILEXFER_ATTR_FLAGS_SPARSE);
	}
	
	public void setSparse(boolean value) throws SftpStatusException {
		setAttributeBit(SSH_FILEXFER_ATTR_FLAGS_SPARSE, value);
	}
	
	public boolean isAppendOnly() throws SftpStatusException {
		return isAttributeBitSet(SSH_FILEXFER_ATTR_FLAGS_APPEND_ONLY);
	}

	public void setAppendOnly(boolean value) throws SftpStatusException {
		setAttributeBit(SSH_FILEXFER_ATTR_FLAGS_APPEND_ONLY, value);
	}
	
	public boolean isImmutable() throws SftpStatusException {
		return isAttributeBitSet(SSH_FILEXFER_ATTR_FLAGS_IMMUTABLE);
	}
	
	public void setImmutable(boolean value) throws SftpStatusException {
		setAttributeBit(SSH_FILEXFER_ATTR_FLAGS_IMMUTABLE, value);
	}
	
	public boolean isSync() throws SftpStatusException {
		return isAttributeBitSet(SSH_FILEXFER_ATTR_FLAGS_SYNC);
	}
	
	public void setSync(boolean value) throws SftpStatusException {
		setAttributeBit(SSH_FILEXFER_ATTR_FLAGS_SYNC, value);
	}
	
	/**
	 * Set all the extended attributes. The keys should be of type String, as
	 * should the values.
	 * 
	 * @param attributes
	 *            map of all extended attributes
	 */
	public void setExtendedAttributes(Map<String, byte[]> attributes) {
		flags |= SSH_FILEXFER_ATTR_EXTENDED;
		this.extendedAttributes = attributes;
	}

	/**
	 * Set a single extended attribute value.
	 * 
	 * @param attrName
	 *            attribute name
	 * @param attrValue
	 *            attribute value
	 */
	public void setExtendedAttribute(String attrName, byte[] attrValue) {
		flags |= SSH_FILEXFER_ATTR_EXTENDED;
		if (extendedAttributes == null) {
			extendedAttributes = new HashMap<String, byte[]>();
		}
		extendedAttributes.put(attrName, attrValue);
	}

	/**
	 * Set a single extended attribute value.
	 * 
	 * @param attrName
	 *            attribute name to remove
	 */
	public void removeExtendedAttribute(String attrName) {
		if (extendedAttributes != null) {
			if (extendedAttributes.containsKey(attrName)) {
				extendedAttributes.remove(attrName);
			}
		}
	}

	/**
	 * Get the extended attributes. The key is of type String, as is the value.
	 * 
	 * @return attribute values
	 */
	public Map<String, byte[]> getExtendedAttributes() {
		return this.extendedAttributes;
	}
	
	public boolean hasExtendedAttribute(String attrName) {
		return extendedAttributes.containsKey(attrName);
	}
	
	public byte[] getExtendedAttribute(String attrName) {
		if(extendedAttributes!=null) {
			return extendedAttributes.get(attrName);
		}
		return null;
	}	
	
	public static void main(String[] args) {
		
		
		System.out.println(Long.toBinaryString(VERSION_3_FLAGS));
		System.out.println(Long.toBinaryString(VERSION_4_FLAGS));
		System.out.println(Long.toBinaryString(VERSION_5_FLAGS));
		System.out.println(Long.toBinaryString(VERSION_6_FLAGS));
	}
}
