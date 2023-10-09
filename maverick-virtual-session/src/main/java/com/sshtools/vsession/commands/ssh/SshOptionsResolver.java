package com.sshtools.vsession.commands.ssh;

import java.io.IOException;
import java.util.Collection;

import com.sshtools.server.vsession.VirtualConsole;

public interface SshOptionsResolver {

	boolean resolveOptions(String destination, SshClientArguments arguments, VirtualConsole console) throws IOException;

	Collection<String> matchDestinations(String destination);

}
