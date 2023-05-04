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
import java.nio.file.spi.FileSystemProvider;

import com.sshtools.common.files.nio.AbstractFileNIOProvider;
import com.sshtools.common.publickey.SshPrivateKeyProvider;
import com.sshtools.common.ssh.components.DigestFactory;
import com.sshtools.common.ssh.components.NoneCipher;
import com.sshtools.common.ssh.components.NoneHmac;
import com.sshtools.common.ssh.components.SshCipherFactory;
import com.sshtools.common.ssh.components.SshHmacFactory;
import com.sshtools.common.ssh.components.SshPublicKeyFactory;
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
import com.sshtools.common.ssh.components.jce.HmacSha256ETM;
import com.sshtools.common.ssh.components.jce.HmacSha256_96;
import com.sshtools.common.ssh.components.jce.HmacSha256_at_ssh_dot_com;
import com.sshtools.common.ssh.components.jce.HmacSha512;
import com.sshtools.common.ssh.components.jce.HmacSha512ETM;
import com.sshtools.common.ssh.components.jce.HmacSha512_96;
import com.sshtools.common.ssh.components.jce.MD5Digest;
import com.sshtools.common.ssh.components.jce.OpenSshEcdsaSha2Nist256Certificate;
import com.sshtools.common.ssh.components.jce.OpenSshEcdsaSha2Nist384Certificate;
import com.sshtools.common.ssh.components.jce.OpenSshEcdsaSha2Nist521Certificate;
import com.sshtools.common.ssh.components.jce.OpenSshEd25519Certificate;
import com.sshtools.common.ssh.components.jce.OpenSshRsaCertificate;
import com.sshtools.common.ssh.components.jce.SHA1Digest;
import com.sshtools.common.ssh.components.jce.SHA256Digest;
import com.sshtools.common.ssh.components.jce.SHA384Digest;
import com.sshtools.common.ssh.components.jce.SHA512Digest;
import com.sshtools.common.ssh.components.jce.Ssh2DsaPublicKey;
import com.sshtools.common.ssh.components.jce.Ssh2EcdsaSha2Nist256PublicKey;
import com.sshtools.common.ssh.components.jce.Ssh2EcdsaSha2Nist384PublicKey;
import com.sshtools.common.ssh.components.jce.Ssh2EcdsaSha2Nist521PublicKey;
import com.sshtools.common.ssh.components.jce.Ssh2RsaPublicKey;
import com.sshtools.common.ssh.components.jce.Ssh2RsaPublicKeySHA256;
import com.sshtools.common.ssh.components.jce.Ssh2RsaPublicKeySHA512;
import com.sshtools.common.ssh.components.jce.SshEd25519PublicKeyJCE;
import com.sshtools.common.ssh.components.jce.SshEd448PublicKeyJCE;
import com.sshtools.common.ssh.components.jce.TripleDesCbc;
import com.sshtools.common.ssh.components.jce.TripleDesCtr;
import com.sshtools.common.ssh.compression.NoneCompression;
import com.sshtools.common.ssh.compression.SshCompressionFactory;

@SuppressWarnings("rawtypes")
open module com.sshtools.maverick.base {
	/* Optional. Only needed for PuTTYPrivateKeyFile */
	requires static org.bouncycastle.pkix;
	requires static org.bouncycastle.provider;
	requires static org.bouncycastle.util;
	
	requires transitive com.sshtools.common.logger;
	requires com.sshtools.common.util;
	exports com.sshtools.common.auth;
	exports com.sshtools.common.command;
	exports com.sshtools.common.config;
	exports com.sshtools.common.events;
	exports com.sshtools.common.files;
	exports com.sshtools.common.files.direct;
	exports com.sshtools.common.files.nio;
	exports com.sshtools.common.forwarding;
	exports com.sshtools.common.knownhosts;
	exports com.sshtools.common.net;
	exports com.sshtools.common.nio;
	exports com.sshtools.common.permissions;
	exports com.sshtools.common.policy;
	exports com.sshtools.common.publickey;
	exports com.sshtools.common.publickey.authorized;
	exports com.sshtools.common.rsa;
	exports com.sshtools.common.scp;
	exports com.sshtools.common.sftp;
	exports com.sshtools.common.sftp.extensions;
	exports com.sshtools.common.sftp.extensions.filter;
	exports com.sshtools.common.shell;
	exports com.sshtools.common.ssh;
	exports com.sshtools.common.ssh.components;
	exports com.sshtools.common.ssh.components.jce;
	exports com.sshtools.common.ssh.compression;
	exports com.sshtools.common.ssh2;
	exports com.sshtools.common.sshd;
	exports com.sshtools.common.sshd.config;
	
	uses SshPrivateKeyProvider; 
	provides FileSystemProvider with AbstractFileNIOProvider;
	
	uses DigestFactory;
	provides DigestFactory with 
		MD5Digest.MD5DigestFactory, 
		SHA1Digest.SHA1DigestFactory, 
		SHA256Digest.SHA256DigestFactory, 
		SHA384Digest.SHA384DigestFactory, 
		SHA512Digest.SHA512DigestFactory;
	
	uses SshHmacFactory;
	provides SshHmacFactory with
		NoneHmac.NoneHmacFactory,
		HmacMD5.HmacMD5Factory,
		HmacMD596.HmacMD596Factory,
		HmacMD5ETM.HmacMD5ETMFactory,
		HmacRipeMd160.HmacRipeMd160Factory,
		HmacRipeMd160ETM.HmacRipeMd160ETMFactory,
		HmacSha1.HmacSha1Factory,
		HmacSha196.HmacSha196Factory,
		HmacSha1ETM.HmacSha1ETMFactory,
		HmacSha256.HmacSha256Factory,
		HmacSha256_96.HmacSha256_96Factory,
		HmacSha256_at_ssh_dot_com.HmacSha256_at_ssh_dot_comFactory,
		HmacSha256ETM.HmacSha256ETMFactory,
		HmacSha512.HmacSha512Factory,
		HmacSha512_96.HmacSha512_96Factory,
		HmacSha512ETM.HmacSha512ETMFactory;
	
	uses SshPublicKeyFactory;
	provides SshPublicKeyFactory with
		OpenSshEcdsaSha2Nist256Certificate.OpenSshEcdsaSha2Nist256CertificateFactory,
		OpenSshEcdsaSha2Nist384Certificate.OpenSshEcdsaSha2Nist384CertificateFactory,
		OpenSshEcdsaSha2Nist521Certificate.OpenSshEcdsaSha2Nist521CertificateFactory,
		OpenSshEd25519Certificate.OpenSshEd25519CertificateFactory,
		OpenSshRsaCertificate.OpenSshRsaCertificateFactory,
		Ssh2DsaPublicKey.Ssh2DsaPublicKeyFactory,
		Ssh2EcdsaSha2Nist256PublicKey.Ssh2EcdsaSha2Nist256PublicKeyFactory,
		Ssh2EcdsaSha2Nist384PublicKey.Ssh2EcdsaSha2Nist384PublicKeyFactory,
		Ssh2EcdsaSha2Nist521PublicKey.Ssh2EcdsaSha2Nist521PublicKeyFactory,
		Ssh2RsaPublicKey.Ssh2RsaPublicKeyFactory,
		Ssh2RsaPublicKeySHA256.Ssh2RsaPublicKeySHA256Factory,
		Ssh2RsaPublicKeySHA512.Ssh2RsaPublicKeySHA512Factory,
		SshEd25519PublicKeyJCE.SshEd25519PublicKeyJCEFactory,
		SshEd448PublicKeyJCE.SshEd448PublicKeyJCEFactory;
	
	uses SshCipherFactory;
	provides SshCipherFactory with
		NoneCipher.NoneCipherFactory,
		AES128Cbc.AES128CbcFactory,
		AES128Ctr.AES128CtrFactory,
		AES128Gcm.AES128GcmFactory,
		AES192Cbc.AES192CbcFactory,
		AES192Ctr.AES192CtrFactory,
		AES256Cbc.AES256CbcFactory,
		AES256Ctr.AES256CtrFactory,
		AES256Gcm.AES256GcmFactory,
		ArcFour.ArcFourFactory,
		ArcFour128.ArcFour128Factory,
		ArcFour256.ArcFour256Factory,
		BlowfishCbc.BlowfishCbcFactory,
		TripleDesCbc.TripleDesCbcFactory,
		TripleDesCtr.TripleDesCtrFactory,
		ChaCha20Poly1305.ChaCha20Poly1305Factory;
	
	provides SshCompressionFactory with NoneCompression.NoneCompressionFactory; 
		
}