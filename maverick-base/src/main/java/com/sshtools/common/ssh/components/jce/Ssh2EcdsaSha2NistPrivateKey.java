/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.common.ssh.components.jce;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Signature;

import com.sshtools.common.ssh.components.SshPrivateKey;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.common.util.SimpleASNReader;

public class Ssh2EcdsaSha2NistPrivateKey extends Ssh2BaseJCEPrivateKey implements SshPrivateKey {

	String name;
	String spec;
	String curve;
	
	public Ssh2EcdsaSha2NistPrivateKey(PrivateKey prv, String curve) throws IOException {
		this(prv, curve, null);
	}
	
	public Ssh2EcdsaSha2NistPrivateKey(PrivateKey prv, String curve, Provider customProvider) throws IOException {
        super(prv, customProvider);
    	if(curve.equals("prime256v1") || curve.equals("secp256r1") || curve.equals("nistp256")) {
    		this.curve = "secp256r1";
    		this.name = "ecdsa-sha2-nistp256";
    		this.spec = "SHA256WithECDSA";
    	} else if(curve.equals("secp384r1") || curve.equals("nistp384")) {
    		this.curve = "secp384r1";
    		this.name = "ecdsa-sha2-nistp384";
    		this.spec = "SHA384WithECDSA";        		
    	} else if(curve.equals("secp521r1") || curve.equals("nistp521")) {
    		this.curve = "secp521r1";
    		this.name = "ecdsa-sha2-nistp521";
    		this.spec = "SHA512WithECDSA";
    	} else {
    		throw new IOException("Unsupported curve name " + curve);
    	}
	}
	
	public byte[] sign(byte[] data) throws IOException {
		return sign(data, getAlgorithm());
	}
	
	public byte[] sign(byte[] data, String signingAlgorithm) throws IOException {
		try {
			Signature sig = getJCESignature(spec);
            sig.initSign(prv);
            sig.update(data);
            byte[] sigRaw = sig.sign();
            ByteArrayWriter baw = new ByteArrayWriter();
            try {
                SimpleASNReader asn = new SimpleASNReader(sigRaw);
                
                asn.getByte();
                asn.getLength();
                asn.getByte();

                byte[] r = asn.getData();
                asn.getByte();

                byte[] s = asn.getData();

                baw.writeBinaryString(r);
                baw.writeBinaryString(s);
                return baw.toByteArray();
            } catch (IOException ioe) {
                throw new IOException("DER decode failed: " + ioe.getMessage());
            } finally {
            	baw.close();
            }
        } catch (Exception e) {
            throw new IOException("Error in " + name +
                                             " sign: " + e.getMessage());
        }

	}

	public String getAlgorithm() {
		return name;
	}

	public PrivateKey getJCEPrivateKey() {
		return prv;
	}

	@Override
	public int hashCode() {
		return prv.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj==this) {
			return true;
		}
		if(obj instanceof Ssh2EcdsaSha2NistPrivateKey) {
			Ssh2EcdsaSha2NistPrivateKey other = (Ssh2EcdsaSha2NistPrivateKey)obj;
			if(other.prv!=null) {
				return other.prv.equals(prv);
			}
		}
		return false;
	}
}
