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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class KeyExchangeStateTest {

	@Test
	public void rejectsDuplicateKexInit() {
		KeyExchangeState state = new KeyExchangeState();
		assertTrue(state.start());
		assertTrue(state.acceptKexInit());
		state.beginAlgorithm("curve25519-sha256", true, false);
		assertFalse(state.acceptKexInit());
	}

	@Test
	public void rejectsDuplicateAndOutOfOrderAlgorithmMessages() {
		KeyExchangeState state = new KeyExchangeState();
		state.start();
		state.beginAlgorithm("curve25519-sha256", true, false);
		assertFalse(state.acceptAlgorithmMessage(31));
		assertTrue(state.acceptAlgorithmMessage(30));
		assertFalse(state.acceptAlgorithmMessage(30));
	}

	@Test
	public void permitsRsaClientPhasesInOrder() {
		KeyExchangeState state = new KeyExchangeState();
		state.start();
		state.beginAlgorithm("rsa2048-sha256", false, false);
		assertTrue(state.acceptAlgorithmMessage(30));
		assertFalse(state.acceptAlgorithmMessage(30));
		assertTrue(state.acceptAlgorithmMessage(32));
		assertFalse(state.acceptAlgorithmMessage(32));
	}

	@Test
	public void permitsGroupExchangePhasesInOrder() {
		KeyExchangeState state = new KeyExchangeState();
		state.start();
		state.beginAlgorithm("diffie-hellman-group-exchange-sha256", true, false);
		assertTrue(state.acceptAlgorithmMessage(34));
		assertFalse(state.acceptAlgorithmMessage(30));
		assertTrue(state.acceptAlgorithmMessage(32));
		assertFalse(state.acceptAlgorithmMessage(32));
	}

	@Test
	public void permitsOneDiscardedIncorrectGuess() {
		KeyExchangeState state = new KeyExchangeState();
		state.start();
		state.beginAlgorithm("diffie-hellman-group14-sha256", true, true);
		assertTrue(state.discardAlgorithmMessage(31));
		assertFalse(state.discardAlgorithmMessage(30));
		assertTrue(state.acceptAlgorithmMessage(30));
		assertFalse(state.acceptAlgorithmMessage(30));
	}

	@Test
	public void acceptsNewKeysOnceAndCompletesOnce() {
		KeyExchangeState state = new KeyExchangeState();
		state.start();
		state.beginAlgorithm("curve25519-sha256", true, false);
		assertTrue(state.acceptAlgorithmMessage(30));
		assertTrue(state.acceptNewKeys());
		assertFalse(state.acceptNewKeys());
		state.sentNewKeys();
		assertTrue(state.complete());
		assertFalse(state.complete());
	}
}
