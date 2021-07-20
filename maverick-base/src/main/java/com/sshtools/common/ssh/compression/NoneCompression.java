
package com.sshtools.common.ssh.compression;

import java.io.IOException;


public class NoneCompression implements SshCompression {

	public void init(int type, int level) {
	}

	public byte[] compress(byte[] data, int start, int len) throws IOException {
		return uncompress(data, start, len);
	}

	public byte[] uncompress(byte[] data, int start, int len)
			throws IOException {
		if(len != data.length || start != 0) {
			byte[] arr = new byte[len];
			System.arraycopy(data, start, arr, 0, len);
			return arr;
		}
		else
			return data;
	}

	public String getAlgorithm() {
		return "none";
	}

}
