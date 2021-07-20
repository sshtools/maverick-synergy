
package com.sshtools.common.publickey;

public class EncodedExtension {

	String name;
	byte[] value;
	boolean known;
	
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	protected void setKnown(boolean known) {
		this.known = known;
	}

	protected byte[] getStoredValue() {
		if(value!=null) {
			return value;
		}
		return new byte[0];
	}
	
	public boolean isKnown() {
		return known;
	}
	
	protected void setStoredValue(byte[] value) {
		this.value = value;
	}

	public String getValue() {
		return null;
	}
}
