/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */

package com.sshtools.common.publickey;

import java.util.ArrayList;
import java.util.List;

public abstract class CertificateExtension extends EncodedExtension {

	final static CertificateExtension NO_PRESENCE_REQUIRED =  new NamedCertificateExtension("no-presence-required", true);
	final static CertificateExtension PERMIT_X11_FORWARDING = new NamedCertificateExtension("permit-X11-forwarding", true);
	final static CertificateExtension PERMIT_AGENT_FORWARDING = new NamedCertificateExtension("permit-agent-forwarding", true);
	final static CertificateExtension PERMIT_PORT_FORWARDING = new NamedCertificateExtension("permit-port-forwarding", true);
	final static CertificateExtension PERMIT_PTY = new NamedCertificateExtension("permit-pty", true);
	final static CertificateExtension PERMIT_USER_RC = new NamedCertificateExtension("permit-user-rc", true);
	

	
	public static CertificateExtension createKnownExtension(String name, byte[] value) {
		
		switch(name) {
		case "no-presence-required":
		case "permit-X11-forwarding":
		case "permit-agent-forwarding":
		case "permit-port-forwarding":
		case "permit-pty":
		case "permit-user-rc":
			return new NamedCertificateExtension(name, true);
		case OpenSshCertificate.OPTION_FORCE_COMMAND:
		case OpenSshCertificate.OPTION_SOURCE_ADDRESS:
			return new StringCertificateExtension(name, value, true);
		default:
			return new DefaultCertificateExtension(name, value);
		}
	}
	
	public static class Builder {

		List<CertificateExtension> tmp = new ArrayList<>();
		
		public Builder() {
			
		}
		
		public Builder defaultExtensions() {
			tmp.add(PERMIT_X11_FORWARDING);
			tmp.add(PERMIT_AGENT_FORWARDING);
			tmp.add(PERMIT_PORT_FORWARDING);
			tmp.add(PERMIT_PTY);
			tmp.add(PERMIT_USER_RC);
			return this;
		}
		
		public Builder knownExtension(CertificateExtension ext) {
			if(!ext.isKnown()) {
				throw new IllegalArgumentException("Extension instance provided is not a known extension!");
			}
			tmp.add(ext);
			return this;
		}
		
		public Builder customNamedExtension(String name) {
			tmp.add(new NamedCertificateExtension(name, false));
			return this;
		}
		
		public Builder customStringExtension(String name, String value) {
			tmp.add(new StringCertificateExtension(name, value, false));
			return this;
		}
		
		public List<CertificateExtension> build() {
			return tmp;
		}
	}

}
