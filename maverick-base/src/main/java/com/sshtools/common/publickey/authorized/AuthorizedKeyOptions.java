
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
