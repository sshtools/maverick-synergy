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
