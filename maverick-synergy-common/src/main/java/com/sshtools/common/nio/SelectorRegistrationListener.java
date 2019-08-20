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
package com.sshtools.common.nio;

import java.nio.channels.*;

/**
 * Allows an object to receive notification that a registration has completed
 * on a {@link SelectorThread}.
 */
public interface SelectorRegistrationListener {

        /**
         * The registration completed and its selector added to an available
         * {@link SelectorThread}
         *
         * @param channel SelectableChannel
         * @param key SelectionKey
         * @param selectorThread SelectorThread
         */
        public void registrationCompleted(SelectableChannel channel,
                                    SelectionKey key,
                                    SelectorThread selectorThread);
}
