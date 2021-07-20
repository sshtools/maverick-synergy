
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
