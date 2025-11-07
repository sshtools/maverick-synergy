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

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Optional;

public final class ForwardingRequest {

	public enum Protocol {
		TCP, DOMAIN_SOCKETS
	}
	
	public final static class ForwardingRequestBuilder {
		private Optional<Protocol> protocol = Optional.empty();
		private Optional<String> bindPath = Optional.empty();
		private Optional<String> bindAddress = Optional.empty();
		private Optional<Integer> bindPort = Optional.empty();
		private Optional<String> destinationPath = Optional.empty();
		private Optional<String> destinationAddress = Optional.empty();
		private Optional<Integer> destinationPort = Optional.empty();
		
		private ForwardingRequestBuilder() {
		}

		public ForwardingRequestBuilder withPort(int port) {
			return withBindPort(port).withDestinationPort(port);
		}

		public ForwardingRequestBuilder withBind(InetSocketAddress addr) {
			return withBind(addr.getHostString()).withBindPort(addr.getPort());
		}

		public ForwardingRequestBuilder withBind(String addressToBind) {
			this.bindAddress = Optional.of(addressToBind);
			return this;
		}

		public ForwardingRequestBuilder withBind(String addressToBind, int portToBind) {
			return withBind(addressToBind).withBindPort(portToBind);
		}

		public ForwardingRequestBuilder withBindAll() {
			return withBind("::");
		}

		public ForwardingRequestBuilder withBindPort(int portToBind) {
			this.bindPort = portToBind == 0 ? Optional.empty() : Optional.of(portToBind);
			return this;
		}

		public ForwardingRequestBuilder withDestination(String destinationAddress, int destinationPort) {
			return withDestinationAddress(destinationAddress).withDestinationPort(destinationPort);
		}

		public ForwardingRequestBuilder withDestinationPort(int destinationPort) {
			this.destinationPort = Optional.of(destinationPort);
			return this;
		}

		public ForwardingRequestBuilder withDestinationAddress(String destinationAddress) {
			this.destinationAddress = Optional.of(destinationAddress);
			return this;
		}

		public ForwardingRequestBuilder withDestination(InetSocketAddress addr) {
			return withDestinationAddress(addr.getHostString()).withDestinationPort(addr.getPort());
		}

		public ForwardingRequestBuilder withPath(String path) {
			return withBindPath(path).withDestinationPath(path);
		}

		public ForwardingRequestBuilder withBindPath(String bindPath) {
			this.bindPath = Optional.of(bindPath);
			return this;
		}
		
		public ForwardingRequestBuilder withBindPath(Path boundPath) {
			return withBindPath(boundPath.toString());
		}

		public ForwardingRequestBuilder withDestinationPath(String destinationPath) {
			this.destinationPath = Optional.of(destinationPath);
			return this;
		}
		
		public ForwardingRequestBuilder withProtocol(Protocol protocol) {
			this.protocol = Optional.of(protocol);
			return this;
		}
		
		public ForwardingRequest build() {
			return new ForwardingRequest(this);
		}
		
		public static ForwardingRequestBuilder create() {
			return new ForwardingRequestBuilder();
		}
	}

	
	/**
	 * The role of the forward.
	 */
	public static enum ForwardingRole {
		/**
		 * The forward will bind to a listening socket locally
		 */
		BIND,
		/**
		 * The forward will connect to a socket remotely
		 */
		CONNECT
	}
	
	/**
	 * The type of forward.
	 */
	public static enum ForwardingType {
		/**
		 * A local forward 
		 */
		LOCAL, 
		/**
		 * A remote forward
		 */
		REMOTE;
		
		/**
		 * Get the key this type of forward is stored in a {@link Connection}'s properties.
		 * 
		 * @return type key
		 */
		public String key() {
			return getClass().getName() + "." + name();
		}
	}

	private final Protocol protocol;
	private final Optional<String> bindPath;
	private final Optional<Integer> bindPort;
	private final Optional<Integer> destinationPort;
	private final Optional<String> destinationPath;
	private final Optional<String> bindAddress;
	private final Optional<String> destinationAddress;
	
	private ForwardingRequest(ForwardingRequestBuilder bldr) {
		protocol = bldr.protocol.orElseGet(() -> {
			if(bldr.bindPath.isPresent() || bldr.destinationPath.isPresent()) {
				return Protocol.DOMAIN_SOCKETS;
			}
			else if(bldr.bindAddress.isPresent() || bldr.bindPort.isPresent() ||
					bldr.destinationAddress.isPresent() || bldr.destinationPort.isPresent()) {
				return Protocol.TCP;
			}
			else {
				throw new IllegalStateException("No protocol set or derived.");
			}
		});
		if(protocol == Protocol.TCP) {
			bindPort = bldr.bindPort.or(() -> Optional.of(0));
			destinationPort = bldr.destinationPort.or(() -> Optional.of(0));
			bindAddress = bldr.bindAddress.or(() -> Optional.of("127.0.0.1"));
			destinationAddress = bldr.destinationAddress.or(() -> Optional.of("127.0.0.1"));
			bindPath = Optional.empty();
			destinationPath = Optional.empty();
		}
		else {
			bindPath = bldr.bindPath;
			destinationPath = bldr.destinationPath;
			bindPort = Optional.empty();
			bindAddress = Optional.empty();
			destinationPort = Optional.empty();
			destinationAddress = Optional.empty();
		}
		
	}
	
	public boolean bindAll() {
		switch(protocol) {
		case DOMAIN_SOCKETS:
			return true;
		default:
			return bindAddress().equals("0.0.0.0") || bindAddress().equals("::");
		}
	}
	
	public int bindPort() {
		return bindPort.orElseThrow(() -> new IllegalStateException("This forward has no port to bind."));
	}
	
	public Optional<Integer> bindPortOr() {
		return bindPort;
	}
	
	public int destinationPort() {
		return destinationPort.orElseThrow(() -> new IllegalStateException("This forward has no destination port."));
	}
	
	public Optional<Integer> destinationPortOr() {
		return destinationPort;
	}
	
	public String bindAddress() {
		return bindAddress.orElseThrow(() -> new IllegalStateException("This forward has no address to bind."));
	}
	
	public Optional<String> bindAddressOr() {
		return bindAddress;
	}

	public String destinationAddress() {
		return destinationAddress.orElseThrow(() -> new IllegalStateException("This forward has no local address."));
	}

	public Optional<String> destinationAddressOr() {
		return destinationAddress;
	}
	
	public Protocol protocol() {
		return protocol;
	}

	public Optional<String> bindPathOr() {
		return bindPath;
	}
	
	public String bindPath() {
		return bindPath.orElseThrow(() -> new IllegalStateException("This forward has no local path."));
	}
	
	public String destinationPath() {
		return destinationPath.orElseThrow(() -> new IllegalStateException("This forward has no remote path."));
	}

	public Optional<String> destinationPathOr() {
		return destinationPath;
	}
	
	public String bindName() {
		switch(protocol) {
		case DOMAIN_SOCKETS:
			return bindPathOr().orElse("?");
		default:
			return bindPort.isPresent() ? bindAddress() + ":" + bindPort() : bindAddress();
		}
	}
	
	public String destinationName() {
		switch(protocol) {
		case DOMAIN_SOCKETS:
			return destinationPath().toString();
		default:
			return destinationPort.isPresent() ? destinationAddress() + ":" + destinationPort() : destinationAddress();
		}
	}
	
	public boolean hasBind() {
		return bindAddress.isPresent() || bindPath.isPresent();
	}
	
	public boolean hasDestination() {
		return destinationAddress.isPresent() || destinationPath.isPresent();
	}

	@Override
	public String toString() {
		switch(protocol) {
		case DOMAIN_SOCKETS:
			return "[UDS] " + bindPath.orElse("*") + " -> " + destinationPathOr().orElse("*");
		case TCP:
			return "[TCP] " + bindAddress.orElse("*") + ":" + bindPort.orElse(0) + " -> " + destinationAddress.orElse("*") + ":" + destinationPort.orElse(0);
		default:
			return ForwardingRequest.super.toString();
		}
	}

	/**
	 * Convenience method to create a request for a TCP destination.
	 * 
	 * @param host destination host
	 * @param port destination port
	 * @return request
	 */
	public static ForwardingRequest ofTcpDestination(String host, int port) {
		return new ForwardingRequestBuilder().
				withDestination(host, port).
				build();
	}

	/**
	 * Convenience method to create a request for a UNIX domain socket destination.
	 * 
	 * @param path destination path
	 * @return request
	 */
	public static ForwardingRequest ofDomainSocketDestination(String path) {
		return new ForwardingRequestBuilder().
				withDestinationPath(path).
				build();
	}

	/**
	 * Convenience method to create a request for a UNIX domain socket bind.
	 * 
	 * @param path bind path
	 * @return request
	 */
	public static ForwardingRequest ofDomainSocketBind(String path) {
		return new ForwardingRequestBuilder().
				withBindPath(path).
				build();
	}

	/**
	 * Convenience method to create a request for a UNIX domain socket bind.
	 * 
	 * @param bindPath bind path
	 * @param destinationPath destination bind path
	 * @return request
	 */
	public static ForwardingRequest ofDomainSocket(String bindPath, String destinationPath) {
		return new ForwardingRequestBuilder().
				withBindPath(bindPath).
				withDestinationPath(destinationPath).
				build();
	}

	/**
	 * Convenience method to create a request for a TCP socket bind.
	 * 
	 * @param path bind path
	 * @return request
	 */
	public static ForwardingRequest ofTcpBind(String addressToBind, int portToBind) {
		return new ForwardingRequestBuilder().
				withBind(addressToBind, portToBind).
				build();
	}

	/**
	 * Convenience method create a request a for TCP socket bound to a particular address and a random
	 * with a destination of a particular host and port.
	 * 
	 * @param addressToBind address to bind
	 * @param destinationHost destination host
	 * @param destinationPort destination port
	 * @return
	 */
	public static ForwardingRequest ofTcp(String addressToBind, String destinationHost, int destinationPort) {
		return new ForwardingRequestBuilder().
				withBind(addressToBind).
				withDestination(destinationHost, destinationPort).
				build();
	}

	/**
	 * Convenience method create a request a for TCP socket bound to a particular address and a port
	 * with a destination of a particular host and port.
	 * 
	 * @param addressToBind address to bind
	 * @param portToBind port to bind or zero
	 * @param destinationHost destination host
	 * @param destinationPort destination port
	 * @return
	 */
	public static ForwardingRequest ofTcp(String addressToBind, int portToBind, String destinationHost, int destinationPort) {
		return new ForwardingRequestBuilder().
				withBind(addressToBind, portToBind).
				withDestination(destinationHost, destinationPort).
				build();
	}
}
