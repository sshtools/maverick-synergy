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

package com.sshtools.common.publickey.authorized;

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
