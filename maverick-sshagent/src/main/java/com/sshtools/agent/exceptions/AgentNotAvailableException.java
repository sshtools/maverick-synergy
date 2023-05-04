package com.sshtools.agent.exceptions;

/**
 * @desc   Throws when the agent is not available
 * @author Aruna Abesekara
 * @version 1.0
 */
public class AgentNotAvailableException extends Exception {

	private static final long serialVersionUID = -3802779893612697396L;

	/**
     * Creates a new AgentNotAvailableException object.
     */
    public AgentNotAvailableException() {
        super("An agent could not be found");
    }
    
    public AgentNotAvailableException(String msg) {
        super(msg);
    }
}
