package com.sshtools.synergy.ssh;

import java.io.IOException;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.util.ByteArrayWriter;

public class TransportProtocolHelper {

	public static byte[] generateKexInit(SshContext sshContext, boolean supportsExtInfo, String extInfo) throws SshException, IOException {
		
		try(ByteArrayWriter baw = new ByteArrayWriter()) {
			
			byte[] cookie = new byte[16];
			ComponentManager.getDefaultInstance().getRND().nextBytes(cookie);

			baw.write((byte) TransportProtocol.SSH_MSG_KEX_INIT);

			baw.write(cookie);

			String list = sshContext.supportedKeyExchanges().list(
					sshContext.getPreferredKeyExchange());

			if(supportsExtInfo) {
				list += "," + extInfo;
			}
			baw.writeString(list);

			list = sshContext.getSupportedPublicKeys();

			baw.writeString(list);

			list = sshContext.supportedCiphersCS().list(
					sshContext.getPreferredCipherCS());

			baw.writeString(list);

			list = sshContext.supportedCiphersSC().list(
					sshContext.getPreferredCipherSC());

			baw.writeString(list);

			list = sshContext.supportedMacsCS().list(
					sshContext.getPreferredMacCS());

			baw.writeString(list);

			list = sshContext.supportedMacsSC().list(
					sshContext.getPreferredMacSC());

			baw.writeString(list);

			list = sshContext.supportedCompressionsCS().list(
					sshContext.getPreferredCompressionCS());

			baw.writeString(list);

			list = sshContext.supportedCompressionsSC().list(
					sshContext.getPreferredCompressionSC());

			baw.writeString(list);

			baw.writeInt(0);
			baw.writeInt(0);
			baw.write((byte) 0);
			baw.writeInt(0);
			
			return baw.toByteArray();
		}
	}

}
