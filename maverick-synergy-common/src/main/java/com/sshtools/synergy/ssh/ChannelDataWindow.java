/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
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
	
	public synchronized void consume(long count) {
		windowSpace -= count;
	}
	
	public synchronized void adjust(long count) {
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
