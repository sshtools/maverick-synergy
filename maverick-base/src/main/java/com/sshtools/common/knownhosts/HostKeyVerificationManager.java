
package com.sshtools.common.knownhosts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;

public class HostKeyVerificationManager implements HostKeyVerification {

	List<HostKeyVerification> verifiers = new ArrayList<HostKeyVerification>();
	
	public HostKeyVerificationManager(Collection<? extends HostKeyVerification> verifiers) {
		this.verifiers.addAll(verifiers);
	}
	
	public HostKeyVerificationManager(HostKeyVerification verif) {
		this.verifiers.add(verif);
	}
	
	public HostKeyVerificationManager(HostKeyVerification... verifs) {
		this.verifiers.addAll(Arrays.asList(verifs));
	}
	
	public void addVerifier(HostKeyVerification verif) {
		this.verifiers.add(verif);
	}
	
	public boolean verifyHost(String host, SshPublicKey pk) throws SshException {
		
		for(HostKeyVerification v : verifiers) {
			if(v.verifyHost(host, pk)) {
				return true;
			}
		}
		return true;
	}

}
