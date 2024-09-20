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

/**
 * Version 4 of the SFTP protocol introduces an ACL field in the
 * {@link SftpFileAttributes} structure.
 *
 * @author Lee David Painter
 */
public class ACL {

    public static final int ACL_ALLOWED_TYPE 	= 0x00000000;
    public static final int ACL_DENIED_TYPE 	= 0x00000001;
    public static final int ACL_AUDIT_TYPE 		= 0x00000002;
    public static final int ACL_ALARM_TYPE		= 0x00000003;

    public static final int ACE4_FILE_INHERIT_ACE           = 0x00000001;
    public static final int ACE4_DIRECTORY_INHERIT_ACE      = 0x00000002;
    public static final int ACE4_NO_PROPAGATE_INHERIT_ACE   = 0x00000004;
    public static final int ACE4_INHERIT_ONLY_ACE           = 0x00000008;
    public static final int ACE4_SUCCESSFUL_ACCESS_ACE_FLAG = 0x00000010;
    public static final int ACE4_FAILED_ACCESS_ACE_FLAG     = 0x00000020;
    public static final int ACE4_IDENTIFIER_GROUP           = 0x00000040;
    
    public static final int ACE4_READ_DATA         = 0x00000001;
    public static final int ACE4_LIST_DIRECTORY    = 0x00000001;
    public static final int ACE4_WRITE_DATA        = 0x00000002;
    public static final int ACE4_ADD_FILE          = 0x00000002;
    public static final int ACE4_APPEND_DATA       = 0x00000004;
    public static final int ACE4_ADD_SUBDIRECTORY  = 0x00000004;
    public static final int ACE4_READ_NAMED_ATTRS  = 0x00000008;
    public static final int ACE4_WRITE_NAMED_ATTRS = 0x00000010;
    public static final int ACE4_EXECUTE           = 0x00000020;
    public static final int ACE4_DELETE_CHILD      = 0x00000040;
    public static final int ACE4_READ_ATTRIBUTES   = 0x00000080;
    public static final int ACE4_WRITE_ATTRIBUTES  = 0x00000100;
    public static final int ACE4_DELETE            = 0x00010000;
    public static final int ACE4_READ_ACL          = 0x00020000;
    public static final int ACE4_WRITE_ACL         = 0x00040000;
    public static final int ACE4_WRITE_OWNER       = 0x00080000;
    public static final int ACE4_SYNCHRONIZE       = 0x00100000;
    
    private final int type;
    private final int flags;
    private final int mask;
    private final String who;

    public ACL(int type, int flags, int mask, String who) {
    	this.type = type;
    	this.flags = flags;
    	this.mask = mask;
    	this.who = who;
    }

    public int getType() {
        return type;
    }

    public int getFlags() {
        return flags;
    }

    public int getMask() {
        return mask;
    }

    public String getWho() {
        return who;
    }

	@Override
	public String toString() {
		return "ACL [type=" + type + ", flags=" + flags + ", mask=" + mask + ", who=" + who + "]";
	}
}
