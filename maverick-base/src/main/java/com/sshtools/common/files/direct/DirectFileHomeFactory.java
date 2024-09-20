package com.sshtools.common.files.direct;

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

import com.sshtools.common.ssh.SshConnection;

public class DirectFileHomeFactory {
	public String getHomeDirectory(SshConnection con) {
		String os = System.getProperty("os.name");
		if(os.startsWith("Mac OS X"))
			return "/Users/" + con.getUsername();
		else if(os.startsWith("Windows 1"))
			return "/Users/" + con.getUsername();
		else if(os.startsWith("Windows"))
			return "/Documents and Settings/" + con.getUsername();
		else
			return "/home/" + con.getUsername();
	}
}
