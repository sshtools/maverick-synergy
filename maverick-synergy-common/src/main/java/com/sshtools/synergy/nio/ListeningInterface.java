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

package com.sshtools.synergy.nio;

import java.net.Inet6Address;
import java.net.InetSocketAddress;

/**
 * Represents a listening interface for incoming socket connections.
 */
public class ListeningInterface {
	private InetSocketAddress addressToBind;
	private ProtocolContextFactory<?> contextFactory;
	private int actualPort;
	private int backlog;
	private boolean reuseAddress = true;
	
	public ListeningInterface(InetSocketAddress addressToBind, ProtocolContextFactory<?> context) {
		this(addressToBind, context, 50);
	}

	public ListeningInterface(InetSocketAddress addressToBind, ProtocolContextFactory<?> context, int backlog) {
		this.addressToBind = addressToBind;
		this.contextFactory = context;
		this.backlog = backlog;
	}

	public int getActualPort() {
		return actualPort;
	}

	public void setActualPort(int actualPort) {
		this.actualPort = actualPort;
	}

	public InetSocketAddress getAddressToBind() {
		return addressToBind;
	}

	public ProtocolContextFactory<?> getContextFactory() {
		return contextFactory;
	}

	public boolean isIPV6Interface() {
		return addressToBind.getAddress() instanceof Inet6Address;
	}

	public int getBacklog() {
		return backlog;
	}

	public void setBacklog(int backlog) {
		this.backlog = backlog;
	}
	
	public boolean getSocketOptionReuseAddress() {
		return reuseAddress;
	}
	
	public void setSocketOptionReuseAddress(boolean reuseAddress) {
		this.reuseAddress = reuseAddress;
	}

}