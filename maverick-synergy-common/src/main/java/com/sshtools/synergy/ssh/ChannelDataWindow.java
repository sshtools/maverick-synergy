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

import com.sshtools.common.util.UnsignedInteger32;

public class ChannelDataWindow {

	UnsignedInteger32 maximumWindowSpace;
	UnsignedInteger32 minimumWindowSpace;
	int maximumPacketSize;
	UnsignedInteger32 windowSpace;
	
	public ChannelDataWindow(UnsignedInteger32 initialWindowSpace, UnsignedInteger32 maximumWindowSpace, UnsignedInteger32 minimumWindowSpace, int maximumPacketSize) {
		this.maximumWindowSpace = maximumWindowSpace;
		this.minimumWindowSpace = minimumWindowSpace;
		this.maximumPacketSize =  maximumPacketSize;
		this.windowSpace =  initialWindowSpace;
	}
	
	public synchronized void consume(int count) {
		windowSpace = UnsignedInteger32.deduct(windowSpace, count);
	}
	
	public synchronized void adjust(UnsignedInteger32 count) {
		windowSpace = UnsignedInteger32.add(windowSpace, count);
		notifyAll();
	}
	
	public synchronized UnsignedInteger32 getWindowSpace() {
		return windowSpace;
	}

	public synchronized boolean isAdjustRequired() {
		return windowSpace.longValue() < minimumWindowSpace.longValue();
	}
	
	public synchronized UnsignedInteger32 getAdjustCount() {
		return UnsignedInteger32.deduct(maximumWindowSpace, windowSpace);
	}

	public synchronized UnsignedInteger32 getMaximumWindowSpace() {
		return maximumWindowSpace;
	}

	public synchronized void setMaximumWindowSpace(UnsignedInteger32 maximumWindowSpace) {
		this.maximumWindowSpace = maximumWindowSpace;
	}

	public synchronized UnsignedInteger32 getMinimumWindowSpace() {
		return minimumWindowSpace;
	}

	public synchronized void setMinimumWindowSpace(UnsignedInteger32 minimumWindowSpace) {
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
