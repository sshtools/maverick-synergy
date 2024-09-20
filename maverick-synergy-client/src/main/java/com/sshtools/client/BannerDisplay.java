package com.sshtools.client;

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
 *
 * <p>Callback interface to display authentication banner messages.</p>
 *
 * <p>In some jurisdictions sending a warning message before
 * authentication may be relevant for getting legal protection.  Many
 * UNIX machines, for example, normally display text from
 * `/etc/issue', or use "tcp wrappers" or similar software to display
 * a banner before issuing a login prompt.</p>
 *
 * <p>Implement this interface to show the authentication banner message.
 * The method should display the message and should not return until the user
 * accepts the message</p>
 * 
 */
public interface BannerDisplay {
  /**
   * Called when a banner message is received.
   * @param message the message to display.
   */
  public void displayBanner(String message);
}
