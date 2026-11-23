package com.sshtools.synergy.ssh;

/*-
 * #%L
 * Common API
 * %%
 * Copyright (C) 2002 - 2026 JADAPTIVE Limited
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

final class KeyExchangeState {

	enum Phase {
		IDLE, EXPECT_KEX_INIT, EXPECT_ALGORITHM_MESSAGE, EXPECT_NEWKEYS
	}

	private Phase phase = Phase.IDLE;
	private int[][] expectedMessages = new int[0][];
	private int expectedIndex;
	private boolean discardFirstPacket;
	private boolean remoteNewKeys;
	private boolean localNewKeys;

	synchronized boolean start() {
		if (phase == Phase.IDLE) {
			phase = Phase.EXPECT_KEX_INIT;
			return true;
		}
		return phase == Phase.EXPECT_KEX_INIT;
	}

	synchronized boolean acceptKexInit() {
		return phase == Phase.IDLE || phase == Phase.EXPECT_KEX_INIT;
	}

	synchronized void beginAlgorithm(String algorithm, boolean server,
			boolean discardFirstPacket) {
		expectedMessages = expectedMessages(algorithm, server);
		expectedIndex = 0;
		this.discardFirstPacket = discardFirstPacket;
		remoteNewKeys = false;
		localNewKeys = false;
		phase = Phase.EXPECT_ALGORITHM_MESSAGE;
	}

	synchronized boolean acceptAlgorithmMessage(int messageId) {
		if (phase != Phase.EXPECT_ALGORITHM_MESSAGE
				|| expectedIndex >= expectedMessages.length
				|| !contains(expectedMessages[expectedIndex], messageId)) {
			return false;
		}
		expectedIndex++;
		if (expectedIndex == expectedMessages.length) {
			phase = Phase.EXPECT_NEWKEYS;
		}
		return true;
	}

	synchronized boolean discardAlgorithmMessage(int messageId) {
		if (!discardFirstPacket || phase != Phase.EXPECT_ALGORITHM_MESSAGE
				|| !isKexAlgorithmMessage(messageId)) {
			return false;
		}
		discardFirstPacket = false;
		return true;
	}

	synchronized boolean acceptNewKeys() {
		if (phase != Phase.EXPECT_NEWKEYS || remoteNewKeys) {
			return false;
		}
		remoteNewKeys = true;
		return true;
	}

	synchronized void sentNewKeys() {
		localNewKeys = true;
	}

	synchronized boolean complete() {
		if (remoteNewKeys && localNewKeys) {
			phase = Phase.IDLE;
			remoteNewKeys = false;
			localNewKeys = false;
			return true;
		}
		return false;
	}

	synchronized Phase phase() {
		return phase;
	}

	static boolean isKexAlgorithmMessage(int messageId) {
		return messageId >= 30 && messageId <= 49;
	}

	private static int[][] expectedMessages(String algorithm, boolean server) {
		if (algorithm.contains("group-exchange")) {
			return server
					? new int[][] { { 30, 34 }, { 32 } }
					: new int[][] { { 31 }, { 33 } };
		}
		if (algorithm.contains("rsa")) {
			return server
					? new int[][] { { 31 } }
					: new int[][] { { 30 }, { 32 } };
		}
		return new int[][] { { server ? 30 : 31 } };
	}

	private static boolean contains(int[] values, int value) {
		for (int candidate : values) {
			if (candidate == value) {
				return true;
			}
		}
		return false;
	}
}
