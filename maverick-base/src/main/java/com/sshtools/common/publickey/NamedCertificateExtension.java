package com.sshtools.common.publickey;

public class NamedCertificateExtension extends CertificateExtension {
	public NamedCertificateExtension(String name, boolean known) {
		setName(name);
		setKnown(known);
	}
}
