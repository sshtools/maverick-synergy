/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
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
