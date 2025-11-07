package com.sshtools.common.forwarding;

import java.io.File;

/*-
 * #%L
 * Base API
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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.sshtools.common.forwarding.ForwardingRequest.ForwardingRole;
import com.sshtools.common.forwarding.ForwardingRequest.ForwardingType;
import com.sshtools.common.forwarding.ForwardingRequest.Protocol;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.Permissions;
import com.sshtools.common.scp.ScpPolicy;
import com.sshtools.common.scp.ScpPolicy.ScpPolicyBuilder;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.util.UnsignedInteger32;

/**
 * Represents various Forwarding related policy.
 * 
 * Note, will be made <code>final</code> at version 3.3.0, and will only be able to
 * be constructed using the {@link ForwardingPolicyBuilder}.
 * 
 * TODO make final at 3.3.0+
 */
public class ForwardingPolicy extends Permissions {
	/**
	 * Forwarding permissions
	 */
	public enum ForwardingPermission implements Permission {
		/**
		 * Add TCP permission to allow TCP forwarding from this host.
		 */
		TCP,
		/**
		 * Gateway permission must be added to allow other hosts, i.e. other hosts on
		 * the local LAN or other hosts on the remote LAN depending on the context. This
		 * does not apply to unix domain socket forwarding
		 */
		GATEWAY,
		/**
		 * Add UNIX_DOMAIN_SOCKET permission to allow unix domain socket forwarding from
		 * this host.
		 */
		UNIX_DOMAIN_SOCKET;

		@Override
		public int nativeMask() {
			switch(this) {
			case TCP:
				return ForwardingPolicy.TCP_FORWARDING;
			case GATEWAY:
				return ForwardingPolicy.GATEWAY_FORWARDING;
			default:
				return ForwardingPolicy.UNIX_DOMAIN_SOCKET_FORWARDING;
			}
		}
	}
	
	/**
	 * Interface to be implemented by objects that can query if an incoming or 
	 * outgoing, local or remote forward is valid.
	 */
	public interface ForwardingValidator {
		/**
		 * Validate whether the request of the given type is allowed.
		 * 
		 * @param type type
		 * @param request request
		 * @param role role
		 * @return allowed
		 */
		boolean validate(ForwardingType type, ForwardingRequest request, ForwardingRole role);
	}
	
	/**
	 * Default implementation of a {@link ForwardingValidator} that uses simple
	 * <code>hostname[:port]</code> or <code>/path/to/socket</code> patterns to decide 
	 * if a forwarding valid. Upon construction, this validator will allow any
	 * target.
	 * <p>
	 * Note, this default implementations allows any {@link ForwardingRole#BIND}, the
	 * grant rules only apply to {@link ForwardingRole#CONNECT}. 
	 * <p>
	 * It is safe to alter grants at runtime from any thread.
	 */
	public final static class DefaultForwardingValidator implements ForwardingValidator {
		
		private Set<String> permit = Collections.synchronizedSet(new HashSet<>());
		
		/**
		 * Grant access to a specific host, host and port or socket filename. Filenames 
		 * must be absolute in any format parseable as a {@link Path}.
		 * 
		 * @param forwardingSpec pattern (in format host[:port] or /path/to/file)
		 * @return this for chaining
		 */
		public DefaultForwardingValidator grant(String forwardingSpec) {
			Path path = Path.of(forwardingSpec);
			if(path.isAbsolute()) {
				permit.add(path.toString());
			}
			else {
				if(forwardingSpec.indexOf(':')==-1)
					forwardingSpec += ":*";
			
				permit.add(forwardingSpec);
			}
			return this;
		}
		
		/**
		 * Grant access to a specific path inet address.
		 * 
		 * @param path path to unix domain socket
		 * @return this for chaining
		 */
		public DefaultForwardingValidator grant(InetSocketAddress addr) {
			return grant(addr.getAddress().getHostName() + ":" + addr.getPort());
		}
		
		/**
		 * Grant access to a specific path to a unix domain socket.
		 * 
		 * @param path path to unix domain socket
		 * @return this for chaining
		 */
		public DefaultForwardingValidator grant(File path) {
			return grant(path.toPath());
		}
		
		/**
		 * Grant access to a specific path to a unix domain socket.
		 * 
		 * @param path path to unix domain socket
		 * @return this for chaining
		 */
		public DefaultForwardingValidator grant(Path path) {
			permit.add(path.toAbsolutePath().toString());
			return this;
		}
		
		/**
		 * Revoke previously granted access to a specific host, host and port or socket filename. Filenames 
		 * must be absolute in any format parseable as a {@link Path}.
		 * 
		 * @param forwardingSpec pattern (in format host[:port] or /path/to/file)
		 * @return this for chaining
		 */
		public DefaultForwardingValidator revoke(String forwardingSpec) {
			Path path = Path.of(forwardingSpec);
			if(path.isAbsolute()) {
				permit.remove(path.toString());
			}
			else {
				if(forwardingSpec.indexOf(':')==-1)
					forwardingSpec += ":*";
			
				permit.remove(forwardingSpec);
			}
			return this;
		}
		
		/**
		 * Revoke previously granted access to a specific path inet address.
		 * 
		 * @param path path to unix domain socket
		 * @return this for chaining
		 */
		public DefaultForwardingValidator revoke(InetSocketAddress addr) {
			return revoke(addr.getAddress().getHostName() + ":" + addr.getPort());
		}
		
		/**
		 * Revoke previously granted access to a specific path to a unix domain socket.
		 * 
		 * @param path path to unix domain socket
		 * @return this for chaining
		 */
		public DefaultForwardingValidator revoke(File path) {
			return revoke(path.toPath());
		}
		
		/**
		 * Remove previously granted access to a specific path to a unix domain socket.
		 * 
		 * @param path path to unix domain socket
		 * @return this for chaining
		 */
		public DefaultForwardingValidator revoke(Path path) {
			permit.remove(path.toAbsolutePath().toString());
			return this;
		}

		@Override
		public boolean validate(ForwardingType type, ForwardingRequest request, ForwardingRole role) {
			if(role == ForwardingRole.CONNECT) {
				boolean allow = permit.size() == 0;
				if(!allow) {
					if(request.protocol() == Protocol.TCP) {
						String p = request.destinationAddress() + ":" + request.destinationPort();
						String p2 = request.destinationAddress() + ":*";
						for(String s : permit) {
							allow = s.equals(p) || s.equals(p2);
							if(allow)
								break;
						}
					}
					else if(request.protocol() == Protocol.DOMAIN_SOCKETS) {
						String p = request.destinationPath();
						for(String s : permit) {
							allow = s.equals(p);
							if(allow)
								break;
						}
					}
					else
						throw new UnsupportedOperationException(request.protocol().name());
				}
				return allow;
			}
			else {
				return true;
			}
		}
	}
	
	/**
	 * Build a new {@link ForwardingPolicy}.
	 */
	public final static class ForwardingPolicyBuilder extends AbstractPermissionBuilder<Permission, ForwardingPolicyBuilder> {

		private int forwardingMaxPacketSize = 65536;
		private long forwardingMaxWindowSize = 65536 * 5;
		private long forwardingMinWindowSize = 32768;
		private Optional<ForwardingValidator> validator = Optional.empty();
		
		private ForwardingPolicyBuilder() { }
		
		/**
		 * Create a new {@link ForwardingPolicyBuilder} that will be used to configure
		 * and create a {@link ScpPolicy}.
		 * 
		 * @return builder
		 */
		public static ForwardingPolicyBuilder create() {
			return new ForwardingPolicyBuilder(); 
		}
		
		/**
		 * Set the callback that will validate whether tunnels are allowed at the 
		 * time they are opened. For many cases, a {@link DefaultForwardingValidator} will
		 * suffice. This is configured with a simple list of allowed addresses, ports and file
		 * paths. For more complex logic, consider implementing you own {@link ForwardingValidator}.
		 * 
		 * @param validator validator
		 * @return this for chaining
		 * 
		 */
		public ForwardingPolicyBuilder withValidator(ForwardingValidator validator) {
			this.validator = Optional.of(validator);
			return this;
		}
		
		/**
		 * Set the maximum forwarding packet size in bytes.
		 * 
		 * @param forwardingMaxPacketSize forwarding max packet size
		 * @return this for chaining
		 */
		public ForwardingPolicyBuilder withForwardingMaxPacketSize(int forwardingMaxPacketSize) {
			this.forwardingMaxPacketSize = forwardingMaxPacketSize;
			return this;
		}
		/**
		 * Set the maximum forwarding window size in bytes.
		 * 
		 * @param forwardingMaxWindowSize maximum forwarding window size
		 * @return this for chaining
		 */
		public ForwardingPolicyBuilder withForwardingMaxWindowSize(long forwardingMaxWindowSize) {
			this.forwardingMaxWindowSize = forwardingMaxWindowSize;
			return this;
		}
		
		/**
		 * Set the maximum forwarding window size in bytes.
		 * 
		 * @param forwardingMaxWindowSize maximum forwarding window size
		 * @return this for chaining
		 */
		public ForwardingPolicyBuilder withForwardingMaxWindowSize(UnsignedInteger32 forwardingMaxWindowSize) {
			return withForwardingMaxWindowSize(forwardingMaxWindowSize.longValue());
		}
		
		/**
		 * Set the minimum forwarding window size in bytes.
		 * 
		 * @param forwardingMinWindowSize forwarding session window size
		 * @return this for chaining
		 */
		public ForwardingPolicyBuilder withForwardingMinWindowSize(long forwardingMinWindowSize) {
			this.forwardingMinWindowSize = forwardingMinWindowSize;
			return this;
		}
		
		/**
		 * Set the minimum forwarding window size in bytes.
		 * 
		 * @param forwardingMinWindowSize maximum forwarding window size
		 * @return this for chaining
		 */
		public ForwardingPolicyBuilder withForwardingMinWindowSize(UnsignedInteger32 forwardingMinWindowSize) {
			return withForwardingMinWindowSize(forwardingMinWindowSize.longValue());
		}
		
		/**
		 * Allow all types of forwarding.
		 * 
		 * @return this for chaining
		 */
		public ForwardingPolicyBuilder allowAll() {
			return addPermissions(ForwardingPermission.values());
		}

		/**
		 * Allow gateway forwarding.
		 * 
		 * @return this for chaining
		 */
		public ForwardingPolicyBuilder allowGatewayForwarding() {
			return addPermissions(ForwardingPermission.GATEWAY);
		}

		/**
		 * Deny gateway forwarding. Note, this will override the permission if {@link #allowAll()}, 
		 * {@link #allowGatewayForwarding()} or and individual permission was used.
		 * 
		 * @return this for chaining
		 */
		public ForwardingPolicyBuilder denyGatewayForwarding() {
			removePermissions(ForwardingPermission.GATEWAY);
			return this;
		}

		/**
		 * Allow TCP forwarding.
		 * 
		 * @return this for chaining
		 */
		public ForwardingPolicyBuilder allowTCPForwarding() {
			addPermissions(ForwardingPermission.TCP);
			return this;
		}

		/**
		 * Allow UNIX domain socket forwarding.
		 * 
		 * @return this for chaining
		 */
		public ForwardingPolicyBuilder allowUnixDomainSocketForwarding() {
			addPermissions(ForwardingPermission.UNIX_DOMAIN_SOCKET);
			return this;
		}

		/**
		 * Deny TCP forwarding. Note, this will override the permission if {@link #allowAll()}, 
		 * {@link #allowTCPForwarding()} or and individual permission was used.
		 * 
		 * @return this for chaining
		 */
		public ForwardingPolicyBuilder denyTCPForwarding() {
			removePermissions(ForwardingPermission.TCP);
			return this;
		}

		/**
		 * Deny UNIX domain socket forwarding. Note, this will override the permission if {@link #allowAll()}, 
		 * {@link #allowUnixDomainSocketForwarding()} or and individual permission was used.
		 * 
		 * @return this for chaining
		 */
		public ForwardingPolicyBuilder denyUnixDomainSocketForwarding() {
			removePermissions(ForwardingPermission.UNIX_DOMAIN_SOCKET);
			return this;
		}
		
		/**
		 * Build a new {@link ScpPolicy} given the configuration of this builder.
		 * 
		 * @return policy
		 */
		public ForwardingPolicy build() {
			return new ForwardingPolicy(this);
		}

		private Set<Permission> permissions() {
			return permissions;
		}
	}

	/**
	 * @deprecated
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public static final int ALLOW_FORWARDING         = 0x00000001;

	/**
	 * @deprecated See {@link ForwardingPermission#TCP}
	 */
	@Deprecated
	public static final int TCP_FORWARDING       	 = 0x00000001;

	/**
	 * @deprecated See {@link ForwardingPermission#GATEWAY}
	 */
	@Deprecated
	public static final int GATEWAY_FORWARDING       = 0x00000002;

	/**
	 * @deprecated See {@link ForwardingPermission#UNIX_DOMAIN_SOCKET}
	 */
	@Deprecated
	public static final int UNIX_DOMAIN_SOCKET_FORWARDING = 0x00000008;

	/* TODO make all of these private + final, remove all deprecated setters at 3.3.x+ */
	private int forwardingMaxPacketSize;
	private UnsignedInteger32 forwardingMaxWindowSize = new UnsignedInteger32(65536 * 5);
	private UnsignedInteger32 forwardingMinWindowSize = new UnsignedInteger32(32768);
	private final ForwardingValidator validator;
	private final DefaultForwardingValidator defaultValidator;

	/**
	 * Construct a new policy
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public ForwardingPolicy() {
		validator = defaultValidator = new DefaultForwardingValidator();
		forwardingMaxPacketSize = 65536;
		forwardingMaxWindowSize = new UnsignedInteger32(65536 * 5);
		forwardingMinWindowSize = new UnsignedInteger32(32768);
	}
	
	private ForwardingPolicy(ForwardingPolicyBuilder bldr) {
		super(bldr);
		if(!bldr.permissions().isEmpty())
			permissions |= ALLOW_FORWARDING;
		if(bldr.validator.isPresent()) {
			defaultValidator = null;
			validator = bldr.validator.get();
		}
		else
			validator = defaultValidator = new DefaultForwardingValidator();
		forwardingMaxPacketSize = bldr.forwardingMaxPacketSize;
		forwardingMaxWindowSize = new UnsignedInteger32(bldr.forwardingMaxWindowSize);
		forwardingMinWindowSize = new UnsignedInteger32(bldr.forwardingMinWindowSize);
	}

	/**
	 * Allow gateway forwarding.
	 * 
	 * @return this for chaining
	 * @deprecated will become immutable, use {@link ForwardingPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public ForwardingPolicy allowGatewayForwarding() {
		add(GATEWAY_FORWARDING);
		return this;
	}

	/**
	 * Deny gateway forwarding.
	 * 
	 * @return this for chaining
	 * @deprecated will become immutable, use {@link ForwardingPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public ForwardingPolicy denyGatewayForwarding() {
		remove(GATEWAY_FORWARDING);
		return this;
	}
	
	/**
	 * Grant access to a specific host.
	 * 
	 * @param host host pattern (in format host[:port])
	 * @return this for chaining
	 * @deprecated will become immutable, use {@link ForwardingPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public ForwardingPolicy grantForwarding(String host) {
		checkDefaultValidator();
		defaultValidator.grant(host);
		return this;
	}
	
	/**
	 * Revoke access from a specific host.
	 * @param host host pattern (in format host[:port])
	 * @return this for chaining
	 * @deprecated will become immutable, use {@link ForwardingPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public ForwardingPolicy revokeForwarding(String host) {
		checkDefaultValidator();
		defaultValidator.revoke(host);
		return this;
	}
	
	/**
	 * Check that the source of the forwarding is permitted under this policy. For 
	 * remote forwarding the source is the network interface on the server that is listening 
	 * for connections. For local forwarding it is the original source of the forward on the 
	 * client's network.
	 * @param con the connection the request originated from
	 * @param originHost origin host
	 * @param originPort origin port
	 * @return valid
	 */
	@Deprecated(forRemoval = true, since = "3.2.0")
	public boolean checkInterfacePermitted(SshConnection con, String originHost, int originPort) {
		boolean allow = check(ALLOW_FORWARDING);
		if(allow) {
			var path = Paths.get(originHost);
			if(path.isAbsolute() && originPort == 0) {					
				return check(UNIX_DOMAIN_SOCKET_FORWARDING);
			}
			else {		
				try {
					allow = ( check(TCP_FORWARDING) && InetAddress.getByName(originHost).isLoopbackAddress() ) || check(GATEWAY_FORWARDING);
				} catch (UnknownHostException e) {
					if(Log.isErrorEnabled())
						Log.error("Failed to determine local forwarding originators interface {}", e, originHost);
					return false;
				}
			}
		}
		return allow;
	}

	/**
	 * Validates whether or not a local or remote forwarding may be bound or
	 * connected.
	 * <p>
	 * When {@link ForwardingRole} is {@link ForwardingRole#BIND}, checks will be
	 * made to see if the either the source local TCP server socket, or the source
	 * local unix domain socket may be bound. In this case, the
	 * {@link ForwardingRequest} will contain the bind address or path, the
	 * destination host or path will be empty and the protocol will specify whether
	 * TCP or unix domain sockets are to be used.
	 * <p>
	 * When {@link ForwardingRole} is {@link ForwardingRole#CONNECT}, checks will be
	 * made to see if the destination address or path is allowed. In this case, the
	 * {@link ForwardingRequest} will contain the destination address or path, the
	 * bind host or path will be empty and the protocol will specify whether TCP or
	 * unix domain sockets are to be used.
	 * <p>
	 * In both cases, {@link ForwardingType} will specify the type of forward in
	 * use.
	 * The {@link ForwardingPermission} will be checked only for when {@link ForwardingRole} is
	 * {@link ForwardingRole#BIND}.
	 * <p>
	 * 
	 * @param con     the connection the request originated from
	 * @param role    the role of the connection
	 * @param type	  type type
	 * @param request the forwarding request
	 * @return permitted
	 */
	public boolean validate(SshConnection con, ForwardingRole role, ForwardingType type, ForwardingRequest request) {
		if(checkAny(con, 
					ForwardingPermission.values(),
					type,
					request)) {
			if(role == ForwardingRole.BIND) {
				if(request.protocol() == Protocol.DOMAIN_SOCKETS) {					
					return check(con, ForwardingPermission.UNIX_DOMAIN_SOCKET, type, request) &&
							validator.validate(type, request, role);
				}
				else {
					boolean allow = checkAny(con, new ForwardingPermission[] { 
							ForwardingPermission.TCP, 
							ForwardingPermission.GATEWAY  
						}, request);
					if(allow) {			
						try {
							allow = ( check(con, ForwardingPermission.TCP, type, request) && InetAddress.getByName(request.bindAddress()).isLoopbackAddress() ) || check(con, ForwardingPermission.GATEWAY, type, request);
						} catch (UnknownHostException e) {
							if(Log.isErrorEnabled())
								Log.error("Failed to determine local forwarding originators interface {}", e, request.bindAddress());
							return false;
						}
					}
				
					return allow;
				}
			}
			else if(role == ForwardingRole.CONNECT) {
				return validator.validate(type, request, role);
			}
			else {
				throw new UnsupportedOperationException(role.name());
			}
		}
		else {
			return false;
		}
	}
	
	/**
	 * Check the host of the forwarding is permitted under this policy. For remote forwarding
	 * the host is the original source of the forwarding request on the local network. For local
	 * forwarding the host is the destination of the forwarding on the local network.
	 * 
	 * @param con the connection the request originated from
	 * @param host
	 * @param port
	 * @return permitted
	 * @deprecated
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public boolean checkHostPermitted(SshConnection con, String host, int port) {
		boolean allow = check(ALLOW_FORWARDING);
		if(allow) {
			return check(con, ForwardingPermission.TCP, host, port) && validator.validate(
				ForwardingType.LOCAL, ForwardingRequest.ofTcpDestination(host, port), ForwardingRole.CONNECT
			);
		}
		return allow;
	}

	/**
	 * Allow TCP forwarding.
	 * 
	 * @return this for chaining
	 * @deprecated will become immutable, use {@link ForwardingPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public ForwardingPolicy allowTCPForwarding() {
		add(TCP_FORWARDING);
		return this;
	}

	/**
	 * Allow UNIX domain socket forwarding.
	 * 
	 * @return this for chaining
	 * @deprecated will become immutable, use {@link ForwardingPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public ForwardingPolicy allowUnixDomainSocketForwarding() {
		add(UNIX_DOMAIN_SOCKET_FORWARDING);
		return this;
	}

	/**
	 * Deny TCP forwarding.
	 * 
	 * @return this for chaining
	 * @deprecated will become immutable, use {@link ForwardingPolicyBuilder}.
	 */
	public ForwardingPolicy denyTCPForwarding() {
		remove(TCP_FORWARDING);
		return this;
	}

	/**
	 * Deny UNIX domain socket forwarding.
	 * 
	 * @return this for chaining
	 * @deprecated will become immutable, use {@link ForwardingPolicyBuilder}.
	 */
	public ForwardingPolicy denyUnixDomainSocketForwarding() {
		remove(UNIX_DOMAIN_SOCKET_FORWARDING);
		return this;
	}

	/**
	 * Allow all forwarding.
	 * 
	 * @return this for chaining
	 * @deprecated will become immutable, use {@link ForwardingPolicyBuilder}.
	 */
	public ForwardingPolicy allowForwarding() {
		add(TCP_FORWARDING);
		add(UNIX_DOMAIN_SOCKET_FORWARDING);
		return this;
	}

	/**
	 * Deny all forwarding.
	 * 
	 * @return this for chaining
	 * @deprecated will become immutable, use {@link ForwardingPolicyBuilder}.
	 */
	public ForwardingPolicy denyForwarding() {
		remove(TCP_FORWARDING);
		remove(UNIX_DOMAIN_SOCKET_FORWARDING);
		return this;
	}

	/**
	 * Get the maximum forwarding packet size.
	 * 
	 * @return maximum forwarding packet size 
	 */
	public int getForwardingMaxPacketSize() {
		return forwardingMaxPacketSize;
	}

	/**
	 * Set the maximum forwarding packet size.
	 * 
	 * @param forwardingMaxWindowSize maximum forwarding packet size 
	 * @deprecated will become immutable, use {@link ForwardingPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setForwardingMaxPacketSize(int forwardingMaxPacketSize) {
		this.forwardingMaxPacketSize = forwardingMaxPacketSize;
	}

	/**
	 * Get the maximum forwarding window size.
	 * 
	 * @return maximum forwarding window size 
	 */
	public UnsignedInteger32 getForwardingMaxWindowSize() {
		return forwardingMaxWindowSize;
	}

	/**
	 * Set the maximum forwarding window size.
	 * 
	 * @param forwardingMaxWindowSize maximum forwarding window size 
	 * @deprecated will become immutable, use {@link ForwardingPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setForwardingMaxWindowSize(UnsignedInteger32 forwardingMaxWindowSize) {
		this.forwardingMaxWindowSize = forwardingMaxWindowSize;
	}

	/**
	 * Get the minimum forwarding window size.
	 * 
	 * @return minimum forwarding window size 
	 */
	public UnsignedInteger32 getForwardingMinWindowSize() {
		return forwardingMinWindowSize;
	}

	/**
	 * Set the minimum forwarding window size.
	 * 
	 * @param forwardingMinWindowSize minimum forwarding window size 
	 * @deprecated will become immutable, use {@link ScpPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setForwardingMinWindowSize(UnsignedInteger32 forwardingMinWindowSize) {
		this.forwardingMinWindowSize = forwardingMinWindowSize;
	}
	
	private void checkDefaultValidator() {
		if(defaultValidator == null)
			throw new UnsupportedOperationException("Use " + ForwardingValidator.class.getName());
	}
	
	
}
