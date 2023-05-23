package com.sshtools.common.logger;

import java.nio.file.Path;

@FunctionalInterface
public interface FileWatchingCallback {

	void changed(Path path);
}
