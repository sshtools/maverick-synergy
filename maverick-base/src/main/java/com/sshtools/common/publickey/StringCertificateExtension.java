package com.sshtools.common.publickey;

import java.io.IOException;

import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;

public class StringCertificateExtension extends CertificateExtension {

	public StringCertificateExtension(String name, String value, boolean known) {
		setName(name);
		setKnown(known);
		try(ByteArrayWriter writer = new ByteArrayWriter()) {
			writer.writeString(value);
			setStoredValue(writer.toByteArray());
		} catch(IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
	
	StringCertificateExtension(String name, byte[] value, boolean known) {
		setName(name);
		setStoredValue(value);
	}

	public String getValue() {
		try {
			try(ByteArrayReader reader = new ByteArrayReader(getStoredValue())) {
				return reader.readString();
			}
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
}
