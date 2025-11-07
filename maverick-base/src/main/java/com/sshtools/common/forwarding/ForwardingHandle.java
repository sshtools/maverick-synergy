package com.sshtools.common.forwarding;

/*-
 * #%L
 * Base API
 * %%
 * Copyright (C) 2002 - 2025 JADAPTIVE Limited
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

import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;


/**
 * Represents an instance of a forwarding, either remote or local as created by
 * {@link ForwardingManager}.
 */
public interface ForwardingHandle extends  Closeable {
	public static boolean conflicts(ForwardingHandle handle, ForwardingRequest other) {
		var protocol = handle.request().protocol();
		if(protocol != other.protocol())
			return false;
		else {
			switch(protocol) {
			case DOMAIN_SOCKETS:
				return handle.request().bindPath().equals(other.bindPath());
			default:
				if(handle.boundPort().orElse(0) == other.bindPort()) {
					if(handle.request().bindAll() || other.bindAll())
						return true;			
					else
						return handle.request().bindAddress().equals(other.bindAddress());
				}
				else
					return false;
			}
		}
		
	}
	
	
	/**
	 * Wrap a handle to receive notification when it is closed.
	 * 
	 * @param delegate handle
	 * @param onClose on close
	 * @return wrapped handle
	 */
	public static ForwardingHandle onClose(ForwardingHandle delegate, Consumer<ForwardingHandle> onClose) {
		return new ForwardingHandle() {

			@Override					
			public String toString() {
				return "{" + type().name() + "} : " + delegate.request() + " = " + boundPort().orElse(0);
			}
			
			@Override
			public void close(boolean killActiveTunnels) throws IOException {
				try {
					delegate.close(killActiveTunnels);
				}
				finally {
					onClose.accept(this);
				}
			}
			
			@Override
			public ForwardingRequest request() {
				return delegate.request();
			}
			
			@Override
			public Optional<Integer> boundPort() {
				return delegate.boundPort();
			}

			@Override
			public ForwardingRequest.ForwardingType type() {
				return delegate.type();
			}

			@Override
			public Optional<String> boundPath() {
				return delegate.boundPath();
			}
		};
	}
	
	/**
	 * The type of forwarding.
	 * 
	 * @return type
	 */
	ForwardingRequest.ForwardingType type();
	
	/**
	 * The request that created this forward. You can access the original hostnames,
	 * ports, file path etc from this.
	 * 
	 * @return original forwarding request
	 */
	ForwardingRequest request();
	
	/**
	 * If the original TCP forwarding request used port zero, then the actual port number will be
	 * assigned when the forwarding starts. Use this method to obtain the assigned
	 * port number. This will either be a non-zero number, or {@link Optional#empty()} 
	 * if the forwarding type has no concept of a bound port. 
	 * 
	 * @return bound port
	 */
	Optional<Integer> boundPort();
	
	/**
	 * If the original Unix Domain Socket forwarding request used an empty path, then the 
	 * actual port number will be assigned when the forwarding starts. Use this method to obtain the assigned
	 * path. This will either be a temporary file  path, or {@link Optional#empty()} 
	 * if the forwarding type has no concept of a bound path. 
	 * 
	 * @return bound port
	 */
	Optional<String> boundPath();
	
	/**
	 * Close the tunnel, and optionally any active connections.
	 * 
	 * @param killActiveTunnels kill active tunnels.
	 * @throws IOException on error
	 */
	void close(boolean killActiveTunnels) throws IOException ;
	
	/**
	 * Close the tunnel and all active connections. 
	 */
	default void close() throws IOException {
		close(true);
	}
	

}
