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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.sshtools.client.SshClient.SshClientBuilder;
import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.events.EventListener;
import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.server.vsession.VirtualConsole;
import com.sshtools.vsession.commands.ssh.AbstractSshOptionsEvaluator;
import com.sshtools.vsession.commands.ssh.SshClientArguments;

public class InMemoryCallbackRegistrationService implements CallbackRegistrationService {

	public static final String PROXIED_CLIENT_CONNECTION = "proxyConnection";
	
	Map<String, Callback> callbacks = new HashMap<>();
	
	public InMemoryCallbackRegistrationService() {
		AbstractSshOptionsEvaluator.addResolver(this);
	}
	
	@Override
	public boolean resolveOptions(String destination, SshClientArguments arguments, VirtualConsole console)
			throws IOException {
		
		if(Log.isInfoEnabled()) {
			Log.info("Looking for callback with destination {}", destination);
		}
		
		Callback c = getCallbackByUUID(destination);
		if(Objects.nonNull(c)) {
			if(Log.isInfoEnabled()) {
				Log.info("Found callback with uuid {}", destination);
			}
			arguments.setConnection(c.getConnection());
			
			if(Log.isInfoEnabled()) {
				Log.info("Attaching callback connection {} as current proxied connection",
						c.getConnection().getUUID());
			}
			
			console.getConnection().setProperty( PROXIED_CLIENT_CONNECTION, 
					SshClientBuilder.create(c.getConnection()).
						withoutCloseOnDisconnect().
						build());
			
			c.getConnection().addEventListener(new EventListener() {

				@Override
				public void processEvent(Event evt) {
					if(evt.getId()==EventCodes.EVENT_DISCONNECTED) {
						SshConnection con = (SshConnection) evt.getAttribute(EventCodes.ATTRIBUTE_CONNECTION);
						if(Log.isInfoEnabled()) {
							Log.info("Detaching callback connection {} from current proxied connection",
									con.getUUID());
						}
						console.getConnection().setProperty(
								PROXIED_CLIENT_CONNECTION, 
								null);
					}
				}
				
			});
			return true;
		}
		
		return false;
	}

	@Override
	public Collection<String> matchDestinations(String destination) {
		
		List<String> results = new ArrayList<>();
		for(String uuid : callbacks.keySet()) {
			if(uuid.startsWith(destination)) {
				results.add(uuid);
			}
		}
		return results;
	}

	@Override
	public Collection<? extends Callback> getCallbacks() {
		return new ArrayList<>(callbacks.values());
	}

	@Override
	public Callback getCallbackByUUID(String uuid) {
		return callbacks.get(uuid);
	}

	@Override
	public void registerCallbackClient(SshConnection con) {
		callbacks.put(con.getUUID(), new DefaultCallback(con));
	}

	@Override
	public void unregisterCallbackClient(String uuid) {
		callbacks.remove(uuid);
	}

	@Override
	public boolean isRegistered(String uuid) {
		return callbacks.containsKey(uuid);
	}

}
