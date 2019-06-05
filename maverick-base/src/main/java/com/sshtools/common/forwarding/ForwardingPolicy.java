package com.sshtools.common.forwarding;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.Permissions;
import com.sshtools.common.ssh.SshConnection;

public class ForwardingPolicy extends Permissions {

	
	
	public static final int ALLOW_FORWARDING         = 0x00000001;
	public static final int GATEWAY_FORWARDING       = 0x00000002;
	
	List<String> permit = new ArrayList<String>();
	
	public ForwardingPolicy() {
	}
	
	public void allowGatewayForwarding() {
		add(GATEWAY_FORWARDING);
	}
	
	public void denyGatewayForwarding() {
		remove(GATEWAY_FORWARDING);
	}
	
	/**
	 * Grant access to a specific host.
	 * @param host
	 * @param port
	 */
	public void grantForwarding(String host) {
		if(host.indexOf(':')==-1)
			host += ":*";
		
		permit.add(host);
	}
	
	/**
	 * Revoke access from a specific host.
	 * @param host
	 */
	public void revokeForwarding(String host) {
		if(host.indexOf(':')==-1)
			host += ":*";
		permit.remove(host);
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
			try {
				InetAddress addr = InetAddress.getByName(originHost);
				
				allow = addr.isLoopbackAddress() | check(GATEWAY_FORWARDING);
			} catch (UnknownHostException e) {
				if(Log.isErrorEnabled())
					Log.error("Failed to determine local forwarding originators interface " + originHost, e);
				return false;
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

	public void allowForwarding() {
		add(ALLOW_FORWARDING);
	}

	public void denyForwarding() {
		remove(ALLOW_FORWARDING);
	}
}
