package com.sshtools.common.events;

import java.util.HashMap;
import java.util.Map;

/**
 * Superclass of all events that may be fired during the life of J2SSH.
 * <p>
 * All events have the following attributes in common :-
 * <ul>
 * <li>Event code. This is an <code>int</code> and must be unique across the
 * whole of J2SSH.</li>
 * <li>State. A boolean specifying whether the event is the result of a
 * successful operation or a failed one.</li>
 * </ul>
 * <p>
 */
public class Event extends EventObject {

    private final int id;
    private final boolean state;
    private final Map<String,Object> eventAttributes = new HashMap<String,Object>();
	
    /**
     * @param source source of event
     * @param id event code
     * @param boolean state true=successful false=unsuccessful
     */
    public Event(Object source, int id, boolean state) {
        super(source);
        this.id = id;
        this.state = state;
    }
    
	public Event(Object source, int id, Throwable error) {
		super(source);
		this.id = id;
		this.state = (error == null);
		if(error!=null) {
			addAttribute(EventCodes.ATTRIBUTE_THROWABLE, error);
		}
	}


    /**
     * Get the unique event id
     * 
     * @return unique event id
     */
    public int getId() {
        return id;
    }

    /**
     * Get the event state. May be one of {@link #STATE_SUCCESSFUL} or
     * {@link #STATE_UNSUCCESSFUL}.
     * 
     * @return event state
     */
    public boolean getState() {
        return state;
    }

    /**
     * Get the value of an event attribute
     * 
     * @param key key of event
     * @return value
     */
    public Object getAttribute(String key) {
        return eventAttributes.get(key);
    }

    public String getAllAttributes() {
        StringBuffer buff = new StringBuffer();
        for (String key : eventAttributes.keySet()) {
            Object value = eventAttributes.get(key);
            buff.append("|\r\n");
            buff.append(key);
            buff.append(" = ");
            if(value!=null) {
            	buff.append(value.toString());
            }
        }

        return buff.toString();
    }

    /**
     * Add an attribute to the event
     * 
     * @param key key of attribute
     * @param String value of attribute
     * @return this object, to allow event attribute chains
     */
    public Event addAttribute(String key, Object value) {
        eventAttributes.put(key, value);
        return this;
    }

}