package com.sshtools.server.callback;

/*-
 * #%L
 * Callback Server API
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

import java.util.Collection;

import com.sshtools.common.ssh.SshConnection;
import com.sshtools.vsession.commands.ssh.SshOptionsResolver;

public interface CallbackRegistrationService extends SshOptionsResolver {

	Collection<? extends Callback> getCallbacks();

	Callback getCallbackByUUID(String uuid);

	void registerCallbackClient(SshConnection con, String memo);

	void unregisterCallbackClient(String uuid);

	boolean isRegistered(String uuid);

}
