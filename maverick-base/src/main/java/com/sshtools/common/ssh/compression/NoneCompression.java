package com.sshtools.common.ssh.compression;

import java.io.IOException;


public class NoneCompression implements SshCompression {

	public void init(int type, int level) {
		// TODO Auto-generated method stub

	}

	public byte[] compress(byte[] data, int start, int len) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public byte[] uncompress(byte[] data, int start, int len)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public String getAlgorithm() {
		// TODO Auto-generated method stub
		return null;
	}

}
