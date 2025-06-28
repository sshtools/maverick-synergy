package com.sshtools.client.sftp;

/*-
 * #%L
 * Client API
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

/**
 * Simple adaptive block size manager. The block size will be reduced on
 * failures and increased slightly on successes.
 */
public class AdaptiveBlockSizeManager {

private int current;
private final int min;
private final int max;

public AdaptiveBlockSizeManager(int initial) {
this(initial, 4096, 65536);
}

public AdaptiveBlockSizeManager(int initial, int min, int max) {
this.current = initial;
this.min = min;
this.max = max;
}

public int getCurrentBlockSize() {
return current;
}

public void recordFailure() {
current = Math.max(min, current / 2);
}

public void recordSuccess() {
if(current < max)
current = Math.min(max, current + 4096);
}
}
