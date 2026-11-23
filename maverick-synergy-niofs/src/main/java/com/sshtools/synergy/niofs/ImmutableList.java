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
 * Copyright (C) 2002-2023 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */
package com.sshtools.synergy.niofs;

/*-
 * #%L
 * Java NIO File System ImplemnentationI
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

import java.util.AbstractList;

/**
 * Simple immutable array list
 *
 * @param <T> The element type
 */
public class ImmutableList<T> extends AbstractList<T> {

    private final T[] data;
    private final int from;
    private final int to;

    public ImmutableList(T[] data) {
        this(data, 0, data.length);
    }

    public ImmutableList(T[] data, int from, int to) {
        this.data = data;
        this.from = from;
        this.to = to;
    }

    @Override
    public T get(int index) {
        return data[from + index];
    }

    @Override
    public int size() {
        return to - from;
    }

    @Override
    public ImmutableList<T> subList(int fromIndex, int toIndex) {
        if (fromIndex == from && toIndex == to) {
            return this;
        }
        return new ImmutableList<>(data, from + fromIndex, from + toIndex);
    }

}
