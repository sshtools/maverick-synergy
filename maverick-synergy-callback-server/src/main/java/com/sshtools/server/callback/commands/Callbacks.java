/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.server.callback.commands;

import java.io.IOException;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.callback.Callback;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.VirtualConsole;

public class Callbacks extends CallbackCommand {

	public Callbacks() {
		super("callbacks", "Callback", "callbacks", "List the connected callback clients");
	}

	@Override
	public void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException, UsageException {

		for(Callback con : service.getCallbacks()) {
			console.println(String.format("%-25s %-15s %s/%s", con.getUUID(), con.getRemoteAddress(), con.getUsername(), con.getMemo()));
		}
	}

}
