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
package com.sshtools.synergy.ssh;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Date;
import java.util.Vector;

import com.sshtools.common.logger.Log;
import com.sshtools.common.publickey.SshPublicKeyFile;
import com.sshtools.common.publickey.SshPublicKeyFileFactory;
import com.sshtools.common.ssh.components.DigestUtils;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.common.util.Utils;

/**
 * Type can be any combination of the following values
 * 
 * 256
 * 512
 * 1024
 * 2048
 * 4096
 * 8192
 * 16384
 * 32768
 * 65536
 * 131072
 * 262144
 * 524288
 * 1048576
 * 2097152
 * 4194304
 * 8388608
 * 16777216
 * 33554432
 * 67108864
 * 134217728
 * 268435456
 * 536870912
 * 1073741824
 * @author lee
 *
 */
final class LicenseVerification {
	
	String licensee;
	String license;
	String comments;
	String description;
	String hash;
	long expires;
	long created;
	String productName;
	long updatesUntil;
	public static final int EXPIRED = 1;
	public static final int INVALID = 2;
	public static final int OK = 4;
	public static final int NOT_LICENSED = 8;
	public static final int EXPIRED_MAINTENANCE = 16;

	static final int LICENSE_VERIFICATION_MASK = 31;
	static final int LICENSE_TYPE_MASK = 0xFFFFFFE0;
	int status = NOT_LICENSED;

	private static final String BEGIN_MARKER = "----BEGIN";
	private static final String END_MARKER = "----END";

	Date verifyDate;
	
	LicenseVerification(Date verifyDate) {
		this.verifyDate = verifyDate;
	}
	
	LicenseVerification() {
		this(new Date(System.currentTimeMillis()));
	}

	final void setLicense(String license) {
		this.license = license;
	}

	final void addLicense(InputStream in) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			license = readToString(in);
		} catch (IOException ex) {
			license = "";
		} finally {
			try {
				in.close();
			} catch (IOException ex) {
			}
			try {
				out.close();
			} catch (IOException ex) {
			}
		}
	}

	final String readToString(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			int read;
			while ((read = in.read()) > -1) {
				out.write(read);
			}
			return new String(out.toByteArray(), "UTF8");
		} finally {
			try {
				in.close();
			} catch (IOException ex) {
			}
			try {
				out.close();
			} catch (IOException ex) {
			}
		}
	}

	final int verifyLicense(String productKey, String vendorName, long releaseDate) {

		try {

			SshPublicKeyFile file = SshPublicKeyFileFactory.parse(productKey.getBytes("UTF8"));
			SshPublicKey key = file.toPublicKey();
			
			BufferedReader reader = new BufferedReader(new StringReader(license));
			String line = reader.readLine();
			if (!line.startsWith(BEGIN_MARKER)) {
				return status = INVALID;
			}

			int idx;
			String licensee = "";
			String comments = "";
			String keydata;
			String validfrom = null;

			StringBuffer keydataBuffer = new StringBuffer("");

			while ((line = reader.readLine()) != null) {
				if (line.startsWith(END_MARKER)) {
					break;
				}
				idx = line.indexOf(':');
				if (idx > -1) {
					String header = line.substring(0, idx).trim();
					String value = line.substring(idx + 1).trim();

					if (header.equals("Licensee")) {
						licensee = value;
						Log.debug(String.format("Licensee: %s", licensee));
						continue;
					} else if (header.equals("Comments")) {
						comments = value;
						Log.debug(String.format("Comments: %s", comments));
						continue;
					} else if (header.equals("Created")) {
						validfrom = value;
						Log.debug(String.format("Created: %s", validfrom));
						continue;
					} else if (header.equals("Type")) {
						description = value;
						Log.debug(String.format("Type: %s", description));
						continue;
					} else if (header.equals("Product")) {
						productName = value;
						Log.debug(String.format("Product: %s", productName));
						continue;
					} else if (header.equals("License Expires")) {
						continue;
					} else if (header.equals("Support Expires")) {
						continue;
					}
					Log.debug(String.format("Data: %s", header));
					keydataBuffer.append(header);
					for (int i = header.length(); i < 15; i++) {
						keydataBuffer.append(' ');
					}
					keydataBuffer.append(": " + value);
				} else {
					keydataBuffer.append(line);
				}
			}
			keydata = keydataBuffer.toString();
			// Convert license into a big integer
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			int pos = 0;
			while (pos < keydata.length()) {
				if (keydata.charAt(pos) == '\r' || keydata.charAt(pos) == '\n') {
					pos++;
					continue;
				}
				buf.write(Integer.parseInt(keydata.substring(pos, pos + 2), 16));
				pos += 2;
			}

			DataInputStream din = new DataInputStream(new ByteArrayInputStream(buf.toByteArray()));

			byte[] tmp = new byte[24];
			din.readFully(tmp);

			byte[] xor = new byte[] { 55, -121, 33, 9, 68, 73, 11, -37, 
					-39, -1, 12, 48, 99, 49, 11, 55,
					0, 45, 12, 87, 25, 23, 21, 11 };

			for (int i = 0; i < 24; i++) {
				tmp[i] ^= xor[i];
			}
			DataInputStream din2 = new DataInputStream(new ByteArrayInputStream(tmp));
			long startdate = din2.readLong();
			long enddate = din2.readLong();
			long supportdate = din2.readLong();
		
			byte[] signature = new byte[din.available()];
			din.readFully(signature);

			this.expires = enddate;
			this.created = startdate;
			

			Vector<Integer> types = new Vector<Integer>();
            types.add(0);
            types.add(32);
            types.add(64);
            types.add(128);
        
            for (int i = 0; i < 23; i++) {
                int type = 256 << i;
                for (Integer t : types) {
                	ByteArrayWriter dout = new ByteArrayWriter();
                	try {
	                    dout.writeString(productName, "UTF-8");
	                    dout.writeString(vendorName, "UTF-8");
	                    dout.writeString(comments, "UTF-8");
	                    dout.writeString(licensee, "UTF-8");
	                    dout.writeInt(type + t);
	                    dout.writeString(description, "UTF-8");
	                    dout.writeUINT64(startdate);
	                    dout.writeUINT64(enddate);
	                    dout.writeUINT64(supportdate);
	                    if (enddate > verifyDate.getTime()) {
	                        this.comments = comments;
	                        this.licensee = licensee;
	                        this.updatesUntil = supportdate;
	                        byte[] s = dout.toByteArray();
	
	                        Log.debug(String.format("Verifyign signature for type %d", t));
	                        if (key.verifySignature(signature, s)) {
	                        	this.hash = Utils.bytesToHex(DigestUtils.md5(s));
	                        	
	                        	/**
	                        	 * This fix is to ensure if we ever issue perpetual 
	                        	 * evaluation licenses again they will only be valid
	                        	 * for the support period (assuming that is also correct).
	                        	 */
	                        	if((type+t)==256) {
	                        		if(enddate > supportdate) {
	                        			if(supportdate > 0 && releaseDate > supportdate) {
	                        				Log.debug("License is out of support");
	                        			    return status = EXPIRED | (type + t);
	                        			} else {
	                        				Log.debug("License is OK");
	                        				return status = OK | (type + t);
	                        			}
	                        		}
	                        	}
	                        	
								if(supportdate > 0 && releaseDate > supportdate) {
									Log.debug("License is out of support");
								     return status = EXPIRED_MAINTENANCE | (type + t);
								} else {
									Log.debug("License is OK");
									return status = OK | (type + t);
								}
	                        }
	                    } else {
	                    	Log.debug("License is Expired");
	                        return status = EXPIRED | (type + t);
	                    }
	                    
                    } finally {
    					dout.close();
    				}
                }
                //types.add(type); //We only support 4 types
            }
            
            Log.debug("Sorry but license is invalid.");
			return status = INVALID;
		} catch (Throwable ex) {
			ex.printStackTrace();
			return status = INVALID;
		}
	}

	String getDescription() {
		return description;
	}

	String getComments() {
		return comments;
	}

	String getLicensee() {
		return licensee;
	}

	int getStatus() {
		return status;
	}

	String getProduct() {
		return productName;
	}

	long getExpiryDate() {
		return expires;
	}
	
	String getHash() {
		return hash;
	}
	
	long getUpdatesUntil() {
		return updatesUntil;
	}
	
	boolean isPerpetual() {
		return expires == Long.MAX_VALUE;
	}
	
	long getCreated() {
		return created;
	}
}
