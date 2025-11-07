package com.sshtools.common.tests;

/*-
 * #%L
 * Base API Tests
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class RandomReadableChannel extends AbstractDigestReadableChannel {

	private final int maximumBlockSize;
	private final SecureRandom r = new SecureRandom();
	private final boolean randomBlock;

	private long totalDataAmount;

	RandomReadableChannel(int maximumBlockSize, long totalDataAmount, boolean randomBlock)
			throws NoSuchAlgorithmException {
		this.maximumBlockSize = maximumBlockSize;
		this.totalDataAmount = totalDataAmount;
		this.randomBlock = randomBlock;
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		checkClosed();
		if (totalDataAmount == 0) {
			return -1;
		}
		var max = Math.min(dst.remaining(), maximumBlockSize);
		if (totalDataAmount < max) {
			max = (int) totalDataAmount;
		}
		var s = max;

		if (randomBlock) {
			s = r.nextInt(max);
			if (s == 0) {
				s = max;
			}
		}
		var b = new byte[s];
		r.nextBytes(b);

		digest.update(b);
		
		dst.put(b);
		totalDataAmount -= b.length;
		return b.length;
	}

}
