package com.sshtools.vsession.commands.ssh;

public interface SshOptionsResolver {

	boolean resolveDestination(String destination, SshClientArguments arguments);

}
