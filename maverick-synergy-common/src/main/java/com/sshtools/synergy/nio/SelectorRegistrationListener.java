package com.sshtools.synergy.nio;

/*-
 * #%L
 * Common API
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
         * @throws IOException 
         */
        public void registrationCompleted(SelectableChannel channel,
                                    SelectionKey key,
                                    SelectorThread selectorThread) throws IOException;
}
