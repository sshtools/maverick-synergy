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

package com.sshtools.synergy.ssh;

public class ChannelDataWindow {

	int maximumWindowSpace;
	int minimumWindowSpace;
	int maximumPacketSize;
	int windowSpace;
	
	public ChannelDataWindow(int initialWindowSpace, int maximumWindowSpace, int minimumWindowSpace, int maximumPacketSize) {
		this.maximumWindowSpace = maximumWindowSpace;
		this.minimumWindowSpace = minimumWindowSpace;
		this.maximumPacketSize = maximumPacketSize;
		this.windowSpace = initialWindowSpace;
	}
	
	public synchronized void consume(int count) {
		windowSpace -= count;
	}
	
	public synchronized void adjust(int count) {
		windowSpace += count;
		notifyAll();
	}
	
	public synchronized int getWindowSpace() {
		return windowSpace;
	}

	public synchronized boolean isAdjustRequired() {
		return windowSpace < minimumWindowSpace;
	}
	
	public synchronized int getAdjustCount() {
		return maximumWindowSpace - windowSpace;
	}

	public synchronized int getMaximumWindowSpace() {
		return maximumWindowSpace;
	}

	public synchronized void setMaximumWindowSpace(int maximumWindowSpace) {
		this.maximumWindowSpace = maximumWindowSpace;
	}

	public synchronized int getMinimumWindowSpace() {
		return minimumWindowSpace;
	}

	public synchronized void setMinimumWindowSpace(int minimumWindowSpace) {
		this.minimumWindowSpace = minimumWindowSpace;
	}

	public int getMaximumPacketSize() {
		return maximumPacketSize;
	}

	public void setMaxiumPacketSize(int maximumPacketSize) {
		this.maximumPacketSize = maximumPacketSize;
	}

	public synchronized void close() {
		notifyAll();
	}
}
