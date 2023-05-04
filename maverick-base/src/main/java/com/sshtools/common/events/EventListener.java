package com.sshtools.common.events;

/**
 * Interface to be implemented by classes interested in receiving
 * events.
 * <p>
 * Listeners should be registered by using
 * EventService#addListener(EventListener).
 */
public interface EventListener {

    /**
     * Invoked when an event occurs. 
     * 
     * @param evt event
     */
    public void processEvent(Event evt);

}
