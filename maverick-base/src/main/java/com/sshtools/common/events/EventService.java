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
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
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
