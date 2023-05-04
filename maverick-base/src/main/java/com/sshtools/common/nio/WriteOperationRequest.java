
package com.sshtools.common.nio;

/**
 * A Throwable Object used to notify the read process that the no further
 * messages should be processed and to allow the write process to write
 * outstanding messages.
 * 
 * @author Lee David Painter
 */
public class WriteOperationRequest extends Throwable {

	private static final long serialVersionUID = -4142682310499201091L;

	public WriteOperationRequest() {
	}
}
