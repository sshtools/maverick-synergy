
package com.sshtools.common.nio;

/**
 * A class implementing this interface is notified when the selector it is
 * registered with becomes idle.
 *
 * @author Lee David Painter
 */
public interface IdleStateListener {

        /**
         * Process an idle event.
         * @return <tt>true</tt> if this listener should be cancelled.
         */
        public boolean idle();
}
