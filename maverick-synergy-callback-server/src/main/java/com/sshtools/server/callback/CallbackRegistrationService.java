package com.sshtools.server.callback;

import java.util.Collection;

import com.sshtools.common.ssh.SshConnection;
import com.sshtools.vsession.commands.ssh.SshOptionsResolver;

public interface CallbackRegistrationService extends SshOptionsResolver {

	Collection<? extends Callback> getCallbacks();

	Callback getCallbackByUUID(String uuid);

	void registerCallbackClient(SshConnection con);

	void unregisterCallbackClient(String uuid);

	boolean isRegistered(String uuid);
	
	

}
