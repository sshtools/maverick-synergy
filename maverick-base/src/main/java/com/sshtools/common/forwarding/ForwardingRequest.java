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

/**
 * Represents a port or domain socket forwarding request, encapsulating the bind
 * and destination addresses (or paths for UNIX domain sockets) used to set up
 * SSH forwarding channels. Instances are immutable and should be created using
 * the {@link ForwardingRequestBuilder}.
 */
public final class ForwardingRequest {

	/**
	 * The transport protocol used for a forwarding request.
	 */
	public enum Protocol {
		/** TCP/IP based forwarding. */
		TCP,
		/** UNIX domain socket based forwarding. */
		DOMAIN_SOCKETS
	}
	
	/**
	 * A builder for creating {@link ForwardingRequest} instances. Use {@link #create()} to
	 * obtain a new builder, configure it using the {@code with*} methods, and call
	 * {@link #build()} to produce an immutable {@link ForwardingRequest}.
	 */
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

		/**
		 * Set both the bind port and destination port to the same value.
		 *
		 * @param port the port number to use for both bind and destination
		 * @return this builder
		 */
		public ForwardingRequestBuilder withPort(int port) {
			return withBindPort(port).withDestinationPort(port);
		}

		/**
		 * Set the bind address and port from an {@link InetSocketAddress}.
		 *
		 * @param addr the socket address to bind to
		 * @return this builder
		 */
		public ForwardingRequestBuilder withBind(InetSocketAddress addr) {
			return withBindAddress(addr.getHostString()).withBindPort(addr.getPort());
		}

		/**
		 * Set the bind address and optionally the bind port from a string specification.
		 * The format is either {@code "host:port"} or just {@code "host"}  or {@code "port"} .
		 *
		 * @param bindSpec the bind specification string
		 * @return this builder
		 */
		public ForwardingRequestBuilder withBind(String bindSpec) {
			var parts = bindSpec.split(":");
			if(parts.length > 1) {
				return withBindAddress(parts[0]).withBindPort(Integer.parseInt(parts[1]));
			}
			else {
				try {
					return withBindPort(Integer.parseInt(bindSpec));
				}
				catch(NumberFormatException e) {
					return withBindAddress(bindSpec);
				}
			}
		}

		/**
		 * Set the address to bind to.
		 *
		 * @param addressToBind the bind address
		 * @return this builder
		 */
		public ForwardingRequestBuilder withBindAddress(String addressToBind) {
			this.bindAddress = Optional.of(addressToBind);
			return this;
		}

		/**
		 * Set both the bind address and bind port.
		 *
		 * @param addressToBind the address to bind to
		 * @param portToBind the port to bind to
		 * @return this builder
		 */
		public ForwardingRequestBuilder withBind(String addressToBind, int portToBind) {
			return withBindAddress(addressToBind).withBindPort(portToBind);
		}

		/**
		 * Configure the request to bind on all interfaces ({@code "::"}).
		 *
		 * @return this builder
		 */
		public ForwardingRequestBuilder withBindAll() {
			return withBindAddress("::");
		}

		/**
		 * Set the port to bind to. A value of {@code 0} indicates a random port
		 * and will clear any previously set bind port.
		 *
		 * @param portToBind the port to bind to, or {@code 0} for a random port
		 * @return this builder
		 */
		public ForwardingRequestBuilder withBindPort(int portToBind) {
			this.bindPort = portToBind == 0 ? Optional.empty() : Optional.of(portToBind);
			return this;
		}

		/**
		 * Set the destination address and port.
		 *
		 * @param destinationAddress the destination host address
		 * @param destinationPort the destination port
		 * @return this builder
		 */
		public ForwardingRequestBuilder withDestination(String destinationAddress, int destinationPort) {
			return withDestinationAddress(destinationAddress).withDestinationPort(destinationPort);
		}

		/**
		 * Set the destination from a string specification. The format is either
		 * {@code "host:port"} or just {@code "host"} or {@code "port"}.
		 *
		 * @param destinationSpec the destination specification string
		 * @return this builder
		 */
		public ForwardingRequestBuilder withDestination(String destinationSpec) {
			var parts = destinationSpec.split(":");
			if(parts.length > 1) {
				return withDestinationAddress(parts[0]).withDestinationPort(Integer.parseInt(parts[1]));
			}
			else {
				try {
					return withDestinationPort(Integer.parseInt(destinationSpec));
				}
				catch(NumberFormatException e) {
					return withDestinationAddress(destinationSpec);
				}
			}
		}

		/**
		 * Set the destination port.
		 *
		 * @param destinationPort the destination port number
		 * @return this builder
		 */
		public ForwardingRequestBuilder withDestinationPort(int destinationPort) {
			this.destinationPort = Optional.of(destinationPort);
			return this;
		}

		/**
		 * Set the destination address.
		 *
		 * @param destinationAddress the destination host address
		 * @return this builder
		 */
		public ForwardingRequestBuilder withDestinationAddress(String destinationAddress) {
			this.destinationAddress = Optional.of(destinationAddress);
			return this;
		}

		/**
		 * Set the destination address and port from an {@link InetSocketAddress}.
		 *
		 * @param addr the destination socket address
		 * @return this builder
		 */
		public ForwardingRequestBuilder withDestination(InetSocketAddress addr) {
			return withDestinationAddress(addr.getHostString()).withDestinationPort(addr.getPort());
		}

		/**
		 * Set both the bind path and destination path to the same UNIX domain socket path.
		 *
		 * @param path the UNIX domain socket path
		 * @return this builder
		 */
		public ForwardingRequestBuilder withPath(String path) {
			return withBindPath(path).withDestinationPath(path);
		}

		/**
		 * Set the UNIX domain socket path to bind to.
		 *
		 * @param bindPath the bind path
		 * @return this builder
		 */
		public ForwardingRequestBuilder withBindPath(String bindPath) {
			this.bindPath = Optional.of(bindPath);
			return this;
		}
		
		/**
		 * Set the UNIX domain socket path to bind to from a {@link Path}.
		 *
		 * @param boundPath the bind path
		 * @return this builder
		 */
		public ForwardingRequestBuilder withBindPath(Path boundPath) {
			return withBindPath(boundPath.toString());
		}

		/**
		 * Set the UNIX domain socket destination path.
		 *
		 * @param destinationPath the destination path
		 * @return this builder
		 */
		public ForwardingRequestBuilder withDestinationPath(String destinationPath) {
			this.destinationPath = Optional.of(destinationPath);
			return this;
		}
		
		/**
		 * Explicitly set the transport protocol. If not set, the protocol will be
		 * derived from the configured addresses or paths.
		 *
		 * @param protocol the protocol to use
		 * @return this builder
		 */
		public ForwardingRequestBuilder withProtocol(Protocol protocol) {
			this.protocol = Optional.of(protocol);
			return this;
		}
		
		/**
		 * Build an immutable {@link ForwardingRequest} from the current builder state.
		 *
		 * @return a new {@link ForwardingRequest}
		 * @throws IllegalStateException if no protocol can be set or derived
		 */
		public ForwardingRequest build() {
			return new ForwardingRequest(this);
		}
		
		/**
		 * Create a new {@link ForwardingRequestBuilder} instance.
		 *
		 * @return a new builder
		 */
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
	
	/**
	 * Determine whether this request binds on all interfaces. For UNIX domain
	 * sockets this always returns {@code true}. For TCP, it returns {@code true}
	 * when the bind address is {@code "0.0.0.0"} or {@code "::"}.
	 *
	 * @return {@code true} if the request binds on all interfaces
	 */
	public boolean bindAll() {
		switch(protocol) {
		case DOMAIN_SOCKETS:
			return true;
		default:
			return bindAddress().equals("0.0.0.0") || bindAddress().equals("::");
		}
	}
	
	/**
	 * Get the port to bind to.
	 *
	 * @return the bind port
	 * @throws IllegalStateException if no bind port is set (e.g. for domain sockets)
	 */
	public int bindPort() {
		return bindPort.orElseThrow(() -> new IllegalStateException("This forward has no port to bind."));
	}
	
	/**
	 * Get the bind port as an {@link Optional}.
	 *
	 * @return an {@link Optional} containing the bind port, or empty if not set
	 */
	public Optional<Integer> bindPortOr() {
		return bindPort;
	}
	
	/**
	 * Get the destination port.
	 *
	 * @return the destination port
	 * @throws IllegalStateException if no destination port is set (e.g. for domain sockets)
	 */
	public int destinationPort() {
		return destinationPort.orElseThrow(() -> new IllegalStateException("This forward has no destination port."));
	}
	
	/**
	 * Get the destination port as an {@link Optional}.
	 *
	 * @return an {@link Optional} containing the destination port, or empty if not set
	 */
	public Optional<Integer> destinationPortOr() {
		return destinationPort;
	}
	
	/**
	 * Get the address to bind to.
	 *
	 * @return the bind address
	 * @throws IllegalStateException if no bind address is set (e.g. for domain sockets)
	 */
	public String bindAddress() {
		return bindAddress.orElseThrow(() -> new IllegalStateException("This forward has no address to bind."));
	}
	
	/**
	 * Get the bind address as an {@link Optional}.
	 *
	 * @return an {@link Optional} containing the bind address, or empty if not set
	 */
	public Optional<String> bindAddressOr() {
		return bindAddress;
	}

	/**
	 * Get the destination address.
	 *
	 * @return the destination address
	 * @throws IllegalStateException if no destination address is set (e.g. for domain sockets)
	 */
	public String destinationAddress() {
		return destinationAddress.orElseThrow(() -> new IllegalStateException("This forward has no local address."));
	}

	/**
	 * Get the destination address as an {@link Optional}.
	 *
	 * @return an {@link Optional} containing the destination address, or empty if not set
	 */
	public Optional<String> destinationAddressOr() {
		return destinationAddress;
	}
	
	/**
	 * Get the transport protocol for this forwarding request.
	 *
	 * @return the protocol
	 */
	public Protocol protocol() {
		return protocol;
	}

	/**
	 * Get the UNIX domain socket bind path as an {@link Optional}.
	 *
	 * @return an {@link Optional} containing the bind path, or empty if not set
	 */
	public Optional<String> bindPathOr() {
		return bindPath;
	}
	
	/**
	 * Get the UNIX domain socket path to bind to.
	 *
	 * @return the bind path
	 * @throws IllegalStateException if no bind path is set (e.g. for TCP)
	 */
	public String bindPath() {
		return bindPath.orElseThrow(() -> new IllegalStateException("This forward has no local path."));
	}
	
	/**
	 * Get the UNIX domain socket destination path.
	 *
	 * @return the destination path
	 * @throws IllegalStateException if no destination path is set (e.g. for TCP)
	 */
	public String destinationPath() {
		return destinationPath.orElseThrow(() -> new IllegalStateException("This forward has no remote path."));
	}

	/**
	 * Get the UNIX domain socket destination path as an {@link Optional}.
	 *
	 * @return an {@link Optional} containing the destination path, or empty if not set
	 */
	public Optional<String> destinationPathOr() {
		return destinationPath;
	}
	
	/**
	 * Get a human-readable name for the bind side of this forwarding request.
	 * For domain sockets, this is the bind path. For TCP, this is the bind
	 * address and port formatted as {@code "address:port"}.
	 *
	 * @return the bind name
	 */
	public String bindName() {
		switch(protocol) {
		case DOMAIN_SOCKETS:
			return bindPathOr().orElse("?");
		default:
			return bindPort.isPresent() ? bindAddress() + ":" + bindPort() : bindAddress();
		}
	}
	
	/**
	 * Get a human-readable name for the destination side of this forwarding request.
	 * For domain sockets, this is the destination path. For TCP, this is the destination
	 * address and port formatted as {@code "address:port"}.
	 *
	 * @return the destination name
	 */
	public String destinationName() {
		switch(protocol) {
		case DOMAIN_SOCKETS:
			return destinationPath().toString();
		default:
			return destinationPort.isPresent() ? destinationAddress() + ":" + destinationPort() : destinationAddress();
		}
	}
	
	/**
	 * Determine whether this request has a bind address or bind path configured.
	 *
	 * @return {@code true} if a bind address or bind path is present
	 */
	public boolean hasBind() {
		return bindAddress.isPresent() || bindPath.isPresent();
	}
	
	/**
	 * Determine whether this request has a destination address or destination path configured.
	 *
	 * @return {@code true} if a destination address or destination path is present
	 */
	public boolean hasDestination() {
		return destinationAddress.isPresent() || destinationPath.isPresent();
	}

	/**
	 * {@inheritDoc}
	 */
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
	 * @param addressToBind the address to bind to
	 * @param portToBind the port to bind to
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
	 * @return request
	 */
	public static ForwardingRequest ofTcp(String addressToBind, String destinationHost, int destinationPort) {
		return new ForwardingRequestBuilder().
				withBindAddress(addressToBind).
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
	 * @return request
	 */
	public static ForwardingRequest ofTcp(String addressToBind, int portToBind, String destinationHost, int destinationPort) {
		return new ForwardingRequestBuilder().
				withBind(addressToBind, portToBind).
				withDestination(destinationHost, destinationPort).
				build();
	}

	/**
	 * Return a string representation of the bind specification for this request,
	 * which is either [<address>:]<port> for TCP forwarding or <socketPath> for
	 * domain socket forwarding. If the bind address is not specified, it defaults
	 * to an empty string.
	 * 
	 * @return bind specification string
	 */
	public String bindSpec() {
		switch(protocol) {
		case DOMAIN_SOCKETS:
			return bindPath.orElse("");
		case TCP:
			return bindAddress.orElse("") + (bindPort.isPresent() ? ":" + bindPort.get() : "");
		default:
			return "";
		}
	}
	
	/**
	 * Return a string representation of the destination specification for this
	 * request, which is either [<address>:]<port> for TCP forwarding or
	 * <socketPath> for domain socket forwarding. If the destination address is not
	 * specified, it defaults to an empty string.
	 * 
	 * @return destination specification string
	 */
	public String destinationSpec() {
		switch(protocol) {
		case DOMAIN_SOCKETS:
			return destinationPath.orElse("");
		case TCP:
			return destinationAddress.orElse("") + (destinationPort.isPresent() ? ":" + destinationPort.get() : "");
		default:
			return "";
		}
	}
}
