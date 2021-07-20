

package com.sshtools.client.shell;

import java.io.IOException;

public interface ShellStartupTrigger {

	public boolean canStartShell(String currentLine, ShellWriter writer) throws IOException;
}
