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
package com.sshtools.common.events;

/**
 * Interface to be implemented by an event service implementation.
 */
public interface EventService {
    
    /**
     * Add an EventListener to the list of objects that will be sent Events.
     * 
     * @param listener listener to add
     */
    public void addListener(EventListener listener);
        
    /**
     * Fire an Event at all EventListeners that have registered an interest in events.
     * @param evt event to fire to all listener
     */
    public void fireEvent(Event evt);

    /**
     * Remove an EventListener
     * @param listener
     */
	public void removeListener(EventListener listener);
	
	/**
	 * Register an event code descriptor (debug use only)
	 */
	public void registerEventCodeDescriptor(@SuppressWarnings("rawtypes") Class cls);
    
	/**
	 * Get an event name from the registered event code descriptors (debug use only)
	 */
    public String getEventName(Integer id);
}
