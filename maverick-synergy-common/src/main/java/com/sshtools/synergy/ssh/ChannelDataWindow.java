/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
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
	
	public synchronized void consume(long count) {
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
