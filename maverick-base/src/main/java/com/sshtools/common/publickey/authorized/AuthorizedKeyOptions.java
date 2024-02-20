package com.sshtools.common.publickey.authorized;

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

public class AuthorizedKeyOptions {

	public static final Option<?> RESRICT = new NoArgOption("restrict");
	
	public static final Option<?> AGENT_FORWARDING = new NoArgOption("agent-forwarding");
	public static final Option<?> PORT_FORWARDING = new NoArgOption("port-forwarding");
	public static final Option<?> PTY = new NoArgOption("pty");
	public static final Option<?> USER_RC = new NoArgOption("user-rc");
	public static final Option<?> X11_FORWARDING = new NoArgOption("X11-forwarding");
	
	public static final Option<?> NO_AGENT_FORWARDING = new NoArgOption("no-agent-forwarding");
	public static final Option<?> NO_PORT_FORWARDING = new NoArgOption("no-port-forwarding");
	public static final Option<?> NO_PTY = new NoArgOption("no-pty");
	public static final Option<?> NO_USER_RC = new NoArgOption("no-user-rc");
	public static final Option<?> NO_X11_FORWARDING = new NoArgOption("no-X11-forwarding");
	
	public static final Option<?> CERT_AUTHORITY = new NoArgOption("cert-authority");
	
	public static Option<?> getNoOption(Option<?> option) {
		return new NoArgOption("no-" + option.getName());
	}
}
