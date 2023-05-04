package com.sshtools.synergy.nio;

import java.nio.channels.SelectionKey;

/**
 * Callback to receive a SelectionKey
 */
public interface SelectionKeyAware {

	void setSelectionKey(SelectionKey key);
}
