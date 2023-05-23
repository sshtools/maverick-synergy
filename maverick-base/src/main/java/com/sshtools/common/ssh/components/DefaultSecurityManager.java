package com.sshtools.common.ssh.components;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.sshtools.common.config.AdaptiveConfiguration;
import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.components.jce.AES128Cbc;
import com.sshtools.common.ssh.components.jce.AES128Ctr;
import com.sshtools.common.ssh.components.jce.AES128Gcm;
import com.sshtools.common.ssh.components.jce.AES192Cbc;
import com.sshtools.common.ssh.components.jce.AES192Ctr;
import com.sshtools.common.ssh.components.jce.AES256Cbc;
import com.sshtools.common.ssh.components.jce.AES256Ctr;
import com.sshtools.common.ssh.components.jce.AES256Gcm;
import com.sshtools.common.ssh.components.jce.ArcFour;
import com.sshtools.common.ssh.components.jce.ArcFour128;
import com.sshtools.common.ssh.components.jce.ArcFour256;
import com.sshtools.common.ssh.components.jce.BlowfishCbc;
import com.sshtools.common.ssh.components.jce.ChaCha20Poly1305;
import com.sshtools.common.ssh.components.jce.HmacMD5;
import com.sshtools.common.ssh.components.jce.HmacMD596;
import com.sshtools.common.ssh.components.jce.HmacMD5ETM;
import com.sshtools.common.ssh.components.jce.HmacRipeMd160;
import com.sshtools.common.ssh.components.jce.HmacRipeMd160ETM;
import com.sshtools.common.ssh.components.jce.HmacSha1;
import com.sshtools.common.ssh.components.jce.HmacSha196;
import com.sshtools.common.ssh.components.jce.HmacSha1ETM;
import com.sshtools.common.ssh.components.jce.HmacSha256;
import com.sshtools.common.ssh.components.jce.HmacSha256_96;
import com.sshtools.common.ssh.components.jce.HmacSha256_at_ssh_dot_com;
import com.sshtools.common.ssh.components.jce.HmacSha512;
import com.sshtools.common.ssh.components.jce.HmacSha512ETM;
import com.sshtools.common.ssh.components.jce.HmacSha512_96;
import com.sshtools.common.ssh.components.jce.TripleDesCbc;

public class DefaultSecurityManager implements SecurityManager {

	AdaptiveConfiguration config;
	
	static Map<String,SecurityLevel> DEFAULTS = new HashMap<>();
	
	static {
		
		DEFAULTS.put(ArcFour.CIPHER, SecurityLevel.WEAK);
		DEFAULTS.put(ArcFour128.CIPHER, SecurityLevel.WEAK);
		DEFAULTS.put(ArcFour256.CIPHER, SecurityLevel.WEAK);
		
		DEFAULTS.put(AES128Cbc.CIPHER, SecurityLevel.WEAK);
		DEFAULTS.put(AES192Cbc.CIPHER, SecurityLevel.WEAK);
		DEFAULTS.put(AES256Cbc.CIPHER, SecurityLevel.WEAK);
		DEFAULTS.put(BlowfishCbc.CIPHER, SecurityLevel.WEAK);
		DEFAULTS.put(TripleDesCbc.CIPHER, SecurityLevel.WEAK);
		
		DEFAULTS.put(AES128Ctr.CIPHER, SecurityLevel.STRONG);
		DEFAULTS.put(AES192Ctr.CIPHER, SecurityLevel.STRONG);
		DEFAULTS.put(AES256Ctr.CIPHER, SecurityLevel.STRONG);
		
		DEFAULTS.put(AES128Gcm.CIPHER, SecurityLevel.PARANOID);
		DEFAULTS.put(AES256Gcm.CIPHER, SecurityLevel.PARANOID);
		DEFAULTS.put(ChaCha20Poly1305.CIPHER, SecurityLevel.PARANOID);
		
		
		DEFAULTS.put(HmacMD5.ALGORITHM, SecurityLevel.WEAK);
		DEFAULTS.put(HmacMD596.ALGORITHM, SecurityLevel.WEAK);
		DEFAULTS.put(HmacMD5ETM.ALGORITHM, SecurityLevel.WEAK);
		DEFAULTS.put(HmacRipeMd160.ALGORITHM, SecurityLevel.WEAK);
		DEFAULTS.put(HmacRipeMd160ETM.ALGORITHM, SecurityLevel.WEAK);
		DEFAULTS.put(HmacSha1.ALGORITHM, SecurityLevel.WEAK);
		DEFAULTS.put(HmacSha196.ALGORITHM, SecurityLevel.WEAK);
		DEFAULTS.put(HmacSha1ETM.ALGORITHM, SecurityLevel.WEAK);
		
		DEFAULTS.put(HmacSha256.ALGORITHM, SecurityLevel.STRONG);
		DEFAULTS.put(HmacSha256_96.ALGORITHM, SecurityLevel.WEAK);
		DEFAULTS.put(HmacSha256_at_ssh_dot_com.ALGORITHM, SecurityLevel.WEAK);
		
		DEFAULTS.put(HmacSha512.ALGORITHM, SecurityLevel.PARANOID);
		DEFAULTS.put(HmacSha512_96.ALGORITHM, SecurityLevel.PARANOID);
		DEFAULTS.put(HmacSha512ETM.ALGORITHM, SecurityLevel.PARANOID);
		
	}
	public DefaultSecurityManager() {
		this(Paths.get("security.cfg"));
	}
	
	public DefaultSecurityManager(Path path) {
		config = new AdaptiveConfiguration(path.toFile());
	}
	@Override
	public SecurityLevel getSecurityLevel(String algorithm) {
		
		return toSecurityLevel(config.getProperty(algorithm, "WEAK"));
	}
	private SecurityLevel toSecurityLevel(String val) {
		return SecurityLevel.valueOf(val);
	}

}
