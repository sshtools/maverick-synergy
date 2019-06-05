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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
/* HEADER */
package com.sshtools.common.ssh;

/**
 * An adapter for the {@link ChannelEventListener} interface.
 *
 * @author Lee David Painter
 */
public class ChannelEventAdapter implements ChannelEventListener {

  public ChannelEventAdapter() {
  }

  public void onChannelOpen(Channel channel) {

  }
  public void onChannelClose(Channel channel) {

  }
  public void onChannelEOF(Channel channel) {

  }
  public void onChannelClosing(Channel channel) {

  }
  public void onWindowAdjust(Channel channel, long currentWindowSpace) {
	  
  }

}
