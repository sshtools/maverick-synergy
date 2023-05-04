/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
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

package com.sshtools.client;


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