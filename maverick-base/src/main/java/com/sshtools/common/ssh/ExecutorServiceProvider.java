package com.sshtools.common.ssh;

import java.util.List;
import java.util.concurrent.ExecutorService;

public interface ExecutorServiceProvider {

	ExecutorService getExecutorService();

	List<ExecutorOperationListener> getExecutorListeners();
}
