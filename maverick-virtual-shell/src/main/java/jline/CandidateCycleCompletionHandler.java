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
package jline;

import java.io.IOException;
import java.util.List;

/**
 *  <p>
 *  A {@link CompletionHandler} that deals with multiple distinct completions
 *  by cycling through each one every time tab is pressed. This
 *  mimics the behavior of the
 *  <a href="http://packages.qa.debian.org/e/editline.html">editline</a>
 *  library.
 *  </p>
 *  <p><strong>This class is currently a stub; it does nothing</strong></p>
 *  @author  <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class CandidateCycleCompletionHandler implements CompletionHandler {
    public boolean complete(final ConsoleReader reader, final List<String> candidates,
                            final int position) throws IOException {
        throw new IllegalStateException("CandidateCycleCompletionHandler unimplemented");
    }
}
