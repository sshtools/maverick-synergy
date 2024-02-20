package com.sshtools.common.events;

/*-
 * #%L
 * Base API
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
