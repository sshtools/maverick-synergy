/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */

package com.sshtools.common.forwarding;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.Permissions;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.util.UnsignedInteger32;

public class ForwardingPolicy extends Permissions {
	
	public static final int ALLOW_FORWARDING         = 0x00000001;
	public static final int GATEWAY_FORWARDING       = 0x00000002;
	public static final int UNIX_DOMAIN_SOCKET_FORWARDING       = 0x00000004;
	
	List<String> permit = new ArrayList<String>();
	
	private int forwardingMaxPacketSize = 65536;
	private UnsignedInteger32 forwardingMaxWindowSize = new UnsignedInteger32(65536 * 5);
	private UnsignedInteger32 forwardingMinWindowSize = new UnsignedInteger32(32768);
	
	public ForwardingPolicy() {
	}
	
	public ForwardingPolicy allowGatewayForwarding() {
		add(GATEWAY_FORWARDING);
		return this;
	}
	
	public ForwardingPolicy denyGatewayForwarding() {
		remove(GATEWAY_FORWARDING);
		return this;
	}
	
	/**
	 * Grant access to a specific host.
	 * @param host
	 * @param port
	 */
	public ForwardingPolicy grantForwarding(String host) {
		if(host.indexOf(':')==-1)
			host += ":*";
		
		permit.add(host);
		return this;
	}
	
	/**
	 * Revoke access from a specific host.
	 * @param host
	 */
	public ForwardingPolicy revokeForwarding(String host) {
		if(host.indexOf(':')==-1)
			host += ":*";
		permit.remove(host);
		return this;
	}
	
	/**
	 * Check that the source of the forwarding is permitted under this policy. For 
	 * remote forwarding the source is the network interface on the server that is listening 
	 * for connections. For local forwarding it is the original source of the forward on the 
	 * client's network.
	 * @param con the connection the request originated from
	 * @param originHost
	 * @param originPort
	 * @return
	 */
	public boolean checkInterfacePermitted(SshConnection con, String originHost, int originPort) {
		
		boolean allow = check(ALLOW_FORWARDING);
		
		if(allow) {
			var path = Paths.get(originHost);
			if(path.isAbsolute() && originPort == 0) {					
				allow = check(UNIX_DOMAIN_SOCKET_FORWARDING);
			}
			else {			
				try {
					InetAddress addr = InetAddress.getByName(originHost);
					
					allow = addr.isLoopbackAddress() | check(GATEWAY_FORWARDING);
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
	 * Check the host of the forwarding is permitted under this policy. For remote forwarding
	 * the host is the original source of the forwarding request on the local network. For local
	 * forwarding the host is the destination of the forwarding on the local network.
	 * 
	 * @param con the connection the request originated from
	 * @param host
	 * @param port
	 * @return
	 */
	public boolean checkHostPermitted(SshConnection con, String host, int port) {
		
		boolean allow = check(ALLOW_FORWARDING);
		
		if(allow) {
			
			allow = permit.size() == 0;
			
			if(!allow) {
				String p = host + ":" + port;
				String p2 = host + ":*";
				for(String s : permit) {
					allow = s.equals(p) || s.equals(p2);
					if(allow)
						break;
				}
			}
			return allow;

		}
		
		return allow;
		
	}

	public ForwardingPolicy allowForwarding() {
		add(ALLOW_FORWARDING);
		return this;
	}

	public ForwardingPolicy denyForwarding() {
		remove(ALLOW_FORWARDING);
		return this;
	}

	public int getForwardingMaxPacketSize() {
		return forwardingMaxPacketSize;
	}

	public void setForwardingMaxPacketSize(int forwardingMaxPacketSize) {
		this.forwardingMaxPacketSize = forwardingMaxPacketSize;
	}

	public UnsignedInteger32 getForwardingMaxWindowSize() {
		return forwardingMaxWindowSize;
	}

	public void setForwardingMaxWindowSize(UnsignedInteger32 forwardingMaxWindowSize) {
		this.forwardingMaxWindowSize = forwardingMaxWindowSize;
	}

	public UnsignedInteger32 getForwardingMinWindowSize() {
		return forwardingMinWindowSize;
	}

	public void setForwardingMinWindowSize(UnsignedInteger32 forwardingMinWindowSize) {
		this.forwardingMinWindowSize = forwardingMinWindowSize;
	}
	
	
}
