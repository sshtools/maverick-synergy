
package com.sshtools.vsession.commands.ssh;

import java.io.IOException;

public interface SshOptionsResolver {

	boolean resolveOptions(String destination, SshClientArguments arguments) throws IOException;

}
