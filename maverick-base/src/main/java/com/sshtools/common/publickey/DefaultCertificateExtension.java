package com.sshtools.common.publickey;

public class DefaultCertificateExtension extends CertificateExtension {

	public DefaultCertificateExtension(String name, byte[] value) {
		setName(name);
		setStoredValue(value);
	}
}
