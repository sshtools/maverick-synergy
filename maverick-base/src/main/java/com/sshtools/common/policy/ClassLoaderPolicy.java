package com.sshtools.common.policy;

public class ClassLoaderPolicy {

	ClassLoader classLoader = null;
	
	public ClassLoader getClassLoader() {
		return getClass().getClassLoader();
	}
	
	public void setClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}
}
