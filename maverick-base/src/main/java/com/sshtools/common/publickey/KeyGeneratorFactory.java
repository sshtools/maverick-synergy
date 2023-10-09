package com.sshtools.common.publickey;

import com.sshtools.common.ssh.components.ComponentInstanceFactory;

public interface KeyGeneratorFactory<T extends KeyGenerator> extends ComponentInstanceFactory<T> {
}